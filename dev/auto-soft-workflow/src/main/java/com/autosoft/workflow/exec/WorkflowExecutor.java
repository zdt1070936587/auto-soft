package com.autosoft.workflow.exec;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import com.autosoft.workflow.entity.WfDefinitionDO;
import com.autosoft.workflow.entity.WfRunDO;
import com.autosoft.workflow.entity.WfRunStepDO;
import com.autosoft.workflow.graph.GraphCodec;
import com.autosoft.workflow.graph.NodeTypes;
import com.autosoft.workflow.graph.WfEdge;
import com.autosoft.workflow.graph.WfNode;
import com.autosoft.workflow.graph.WorkflowGraph;
import com.autosoft.workflow.graph.WorkflowGraphValidator;
import com.autosoft.workflow.mapper.WfRunMapper;
import com.autosoft.workflow.mapper.WfRunStepMapper;
import com.autosoft.workflow.vo.WorkflowRunVO;
import com.autosoft.workflow.vo.WorkflowStepVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Workflow执行器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Service
public class WorkflowExecutor {

    public static final int TIMEOUT_MS = 30_000;
    public static final int MAX_NODES = 32;

    private final WorkflowDefinitionService definitionService;
    private final WorkflowGraphValidator validator;
    private final WfRunMapper runMapper;
    private final WfRunStepMapper stepMapper;
    private final JsonMapper jsonMapper;
    private final Map<String, NodeExecutor> executors;

    public WorkflowExecutor(WorkflowDefinitionService definitionService, WorkflowGraphValidator validator,
                            WfRunMapper runMapper, WfRunStepMapper stepMapper, JsonMapper jsonMapper,
                            List<NodeExecutor> nodeExecutors) {
        this.definitionService = definitionService;
        this.validator = validator;
        this.runMapper = runMapper;
        this.stepMapper = stepMapper;
        this.jsonMapper = jsonMapper;
        this.executors = nodeExecutors.stream().collect(Collectors.toMap(NodeExecutor::type, e -> e, (a, b) -> a));
    }

    @OperLog(module = "WORKFLOW", action = "RUN")
    @Transactional(rollbackFor = Exception.class)
    public WorkflowRunVO dryRun(Long definitionId, Map<String, Object> input) {
        WfDefinitionDO def = definitionService.requireStudio(definitionId);
        return execute(def, def.getGraphJson(), input, true, 0, false);
    }

    @OperLog(module = "WORKFLOW", action = "RUN")
    @Transactional(rollbackFor = Exception.class)
    public WorkflowRunVO runPublished(String code, Map<String, Object> input) {
        WfDefinitionDO def = definitionService.requirePublishedByCode(code);
        LoginUser user = SecurityUtils.requireUser();
        String perm = "wf:" + def.getCode() + ":run";
        if (!user.hasPermission(perm) && !user.isDeveloper()) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限运行该工作流");
        }
        String graphJson = definitionService.publishedGraphJson(def);
        return execute(def, graphJson, input, false, def.getVersion(), false);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowRunVO runPublishedInternal(String code, Map<String, Object> input) {
        WfDefinitionDO def = definitionService.requirePublishedByCode(code);
        String graphJson = definitionService.publishedGraphJson(def);
        return execute(def, graphJson, input, false, def.getVersion(), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resumeAfterApproval(String app, String entity, Long bizId, boolean approved, String comment) {
        List<WfRunDO> paused = runMapper.selectList(new LambdaQueryWrapper<WfRunDO>()
                .eq(WfRunDO::getStatus, WfRunDO.PAUSED)
                .eq(WfRunDO::getDryRun, 0));
        for (WfRunDO run : paused) {
            Map<String, Object> snap = parseSnapshot(run.getTriggerJson());
            if (!matchesPause(snap, app, entity, bizId)) {
                continue;
            }
            if (!approved) {
                run.setStatus(WfRunDO.FAILED);
                run.setErrorMsg(trimErr(comment == null || comment.isBlank() ? "审批驳回" : comment));
                runMapper.updateById(run);
                continue;
            }
            continueFromPause(run, snap);
        }
    }

    public boolean hasActiveRun(Long definitionId) {
        Long count = runMapper.selectCount(new LambdaQueryWrapper<WfRunDO>()
                .eq(WfRunDO::getDefinitionId, definitionId)
                .eq(WfRunDO::getDryRun, 0)
                .in(WfRunDO::getStatus, List.of(WfRunDO.RUNNING, WfRunDO.PAUSED)));
        return count != null && count > 0;
    }

    public WorkflowRunVO getRun(Long runId) {
        WfRunDO run = runMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ResultCode.NOT_FOUND, "运行记录不存在");
        }
        LoginUser user = SecurityUtils.requireUser();
        if (!user.isSuperAdmin() && !user.isDeveloper() && !user.getUserId().equals(run.getStartUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能查看他人运行记录");
        }
        return toVo(run);
    }

    private WorkflowRunVO execute(WfDefinitionDO def, String graphJson, Map<String, Object> input,
                                  boolean dryRun, int version, boolean internal) {
        WorkflowGraph graph = GraphCodec.parse(graphJson, jsonMapper);
        validator.validate(graph);

        LoginUser user = SecurityUtils.currentUser();
        if (!internal && user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        WfRunDO run = new WfRunDO();
        run.setDefinitionId(def.getId());
        run.setVersion(version);
        run.setDryRun(dryRun ? 1 : 0);
        run.setStatus(WfRunDO.RUNNING);
        run.setTriggerJson(jsonMapper.writeValueAsString(input == null ? Map.of() : input));
        run.setStartUserId(user == null ? 0L : user.getUserId());
        run.setTokenInput(0L);
        run.setTokenOutput(0L);
        runMapper.insert(run);

        RunContext context = new RunContext(run.getId(), input, dryRun);
        runLoop(graph, run, context, null);
        return toVo(runMapper.selectById(run.getId()));
    }

    private void continueFromPause(WfRunDO run, Map<String, Object> snap) {
        WfDefinitionDO def = definitionService.requireById(run.getDefinitionId());
        String graphJson = run.getVersion() != null && run.getVersion() > 0
                ? definitionService.publishedGraphJson(def)
                : def.getGraphJson();
        WorkflowGraph graph = GraphCodec.parse(graphJson, jsonMapper);
        validator.validate(graph);
        Map<String, Object> input = asMap(snap.get("input"));
        RunContext context = new RunContext(run.getId(), input, false);
        Object outputs = snap.get("outputs");
        if (outputs instanceof Map<?, ?> map) {
            map.forEach((k, v) -> context.putOutput(String.valueOf(k), v));
        }
        run.setStatus(WfRunDO.RUNNING);
        runMapper.updateById(run);
        WfNode pausedNode = nodeOf(graph, run.getCurrentNodeId());
        context.putOutput(pausedNode.getId(), Map.of("approved", true, "bizId", snap.get("pauseBizId")));
        WfNode next = nextNode(graph, pausedNode, Map.of("result", true), false);
        runLoop(graph, run, context, next);
    }

    private void runLoop(WorkflowGraph graph, WfRunDO run, RunContext context, WfNode startAt) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        try {
            WfNode current = startAt != null ? startAt : graph.getNodes().stream()
                    .filter(n -> NodeTypes.START.equals(n.getType()))
                    .findFirst()
                    .orElseThrow();
            int hops = 0;
            while (true) {
                if (System.currentTimeMillis() > deadline) {
                    throw new BizException(ResultCode.BAD_REQUEST, "运行超时");
                }
                hops++;
                AssertUtils.isTrue(hops <= MAX_NODES, "超过节点数上限");
                run.setCurrentNodeId(current.getId());
                runMapper.updateById(run);
                Object output;
                try {
                    output = executeNode(current, context, run.getId());
                } catch (WorkflowPausedException paused) {
                    persistPause(run, context);
                    return;
                } catch (NodeFailedException failed) {
                    WfEdge errorEdge = findEdge(graph, current.getId(), "error");
                    if (errorEdge == null) {
                        throw failed;
                    }
                    context.putOutput(current.getId(), Map.of("error", failed.getMessage() == null ? "failed" : failed.getMessage()));
                    current = nodeOf(graph, errorEdge.getTo());
                    continue;
                }
                if (NodeTypes.END.equals(current.getType())) {
                    break;
                }
                current = nextNode(graph, current, output, false);
            }
            run.setStatus(WfRunDO.SUCCEEDED);
            run.setTokenInput((long) context.promptTokens());
            run.setTokenOutput((long) context.completionTokens());
            run.setErrorMsg(null);
            runMapper.updateById(run);
        } catch (Exception ex) {
            run.setStatus(WfRunDO.FAILED);
            run.setErrorMsg(trimErr(ex.getMessage()));
            run.setTokenInput((long) context.promptTokens());
            run.setTokenOutput((long) context.completionTokens());
            runMapper.updateById(run);
        }
    }

    private void persistPause(WfRunDO run, RunContext context) {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("_paused", true);
        snap.put("input", context.input());
        snap.put("outputs", context.outputs());
        snap.put("pauseApp", context.pauseApp());
        snap.put("pauseEntity", context.pauseEntity());
        snap.put("pauseBizId", context.pauseBizId());
        snap.put("pauseInstanceId", context.pauseInstanceId());
        run.setStatus(WfRunDO.PAUSED);
        run.setTriggerJson(jsonMapper.writeValueAsString(snap));
        run.setTokenInput((long) context.promptTokens());
        run.setTokenOutput((long) context.completionTokens());
        runMapper.updateById(run);
    }

    private Object executeNode(WfNode node, RunContext context, Long runId) {
        long start = System.currentTimeMillis();
        WfRunStepDO step = new WfRunStepDO();
        step.setRunId(runId);
        step.setNodeId(node.getId());
        step.setNodeType(node.getType());
        Map<String, Object> rendered = TemplateRenderer.renderMap(node.getConfig(), context);
        step.setInputSummary(StepRedactor.summarize(rendered));
        try {
            NodeExecutor executor = executors.get(node.getType());
            if (executor == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "未注册执行器: " + node.getType());
            }
            Object output = executor.execute(node, rendered, context);
            context.putOutput(node.getId(), output);
            step.setStatus("succeeded");
            if (context.dryRun() && NodeTypes.APPROVAL.equals(node.getType())) {
                step.setStatus("skipped");
            }
            step.setOutputSummary(StepRedactor.summarize(output));
            step.setDurationMs((int) (System.currentTimeMillis() - start));
            stepMapper.insert(step);
            return output;
        } catch (WorkflowPausedException paused) {
            step.setStatus("paused");
            step.setOutputSummary(StepRedactor.summarize(Map.of(
                    "flowInstanceId", context.pauseInstanceId(),
                    "bizId", context.pauseBizId())));
            step.setDurationMs((int) (System.currentTimeMillis() - start));
            stepMapper.insert(step);
            throw paused;
        } catch (Exception ex) {
            step.setStatus("failed");
            step.setErrorMsg(trimErr(ex.getMessage()));
            step.setDurationMs((int) (System.currentTimeMillis() - start));
            stepMapper.insert(step);
            throw new NodeFailedException(trimErr(ex.getMessage()));
        }
    }

    private WfNode nextNode(WorkflowGraph graph, WfNode current, Object output, boolean error) {
        if (error) {
            WfEdge edge = findEdge(graph, current.getId(), "error");
            AssertUtils.notNull(edge, "节点无 error 出边: " + current.getId());
            return nodeOf(graph, edge.getTo());
        }
        if (NodeTypes.CONDITION.equals(current.getType())) {
            boolean result = false;
            if (output instanceof Map<?, ?> map && map.get("result") instanceof Boolean b) {
                result = b;
            }
            WfEdge edge = findEdge(graph, current.getId(), result ? "true" : "false");
            AssertUtils.notNull(edge, "condition 缺少分支边: " + current.getId());
            return nodeOf(graph, edge.getTo());
        }
        WfEdge edge = graph.getEdges().stream()
                .filter(e -> current.getId().equals(e.getFrom()) && !"error".equals(e.getWhen()))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST, "节点无出边: " + current.getId()));
        return nodeOf(graph, edge.getTo());
    }

    private WfEdge findEdge(WorkflowGraph graph, String fromId, String when) {
        return graph.getEdges().stream()
                .filter(e -> fromId.equals(e.getFrom()) && when.equals(e.getWhen()))
                .findFirst()
                .orElse(null);
    }

    private WfNode nodeOf(WorkflowGraph graph, String id) {
        return graph.getNodes().stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.BAD_REQUEST, "出边目标不存在"));
    }

    private boolean matchesPause(Map<String, Object> snap, String app, String entity, Long bizId) {
        if (!Boolean.TRUE.equals(snap.get("_paused")) && !"true".equals(String.valueOf(snap.get("_paused")))) {
            return false;
        }
        return app.equals(String.valueOf(snap.get("pauseApp")))
                && entity.equals(String.valueOf(snap.get("pauseEntity")))
                && String.valueOf(bizId).equals(String.valueOf(snap.get("pauseBizId")));
    }

    private Map<String, Object> parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private WorkflowRunVO toVo(WfRunDO run) {
        WorkflowRunVO vo = new WorkflowRunVO();
        vo.setId(run.getId());
        vo.setDefinitionId(run.getDefinitionId());
        WfDefinitionDO def = definitionService.requireById(run.getDefinitionId());
        vo.setDefinitionCode(def.getCode());
        vo.setVersion(run.getVersion());
        vo.setDryRun(run.getDryRun() != null && run.getDryRun() == 1);
        vo.setStatus(run.getStatus());
        vo.setCurrentNodeId(run.getCurrentNodeId());
        vo.setErrorMsg(run.getErrorMsg());
        vo.setTokenInput(run.getTokenInput());
        vo.setTokenOutput(run.getTokenOutput());
        vo.setCreatedAt(run.getCreatedAt());
        List<WfRunStepDO> steps = stepMapper.selectList(new LambdaQueryWrapper<WfRunStepDO>()
                .eq(WfRunStepDO::getRunId, run.getId()).orderByAsc(WfRunStepDO::getId));
        vo.setSteps(steps.stream().map(this::toStepVo).toList());
        return vo;
    }

    private WorkflowStepVO toStepVo(WfRunStepDO step) {
        WorkflowStepVO vo = new WorkflowStepVO();
        vo.setId(step.getId());
        vo.setNodeId(step.getNodeId());
        vo.setNodeType(step.getNodeType());
        vo.setStatus(step.getStatus());
        vo.setInputSummary(step.getInputSummary());
        vo.setOutputSummary(step.getOutputSummary());
        vo.setErrorMsg(step.getErrorMsg());
        vo.setDurationMs(step.getDurationMs());
        return vo;
    }

    private static String trimErr(String message) {
        if (message == null || message.isBlank()) {
            return "运行失败";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
