package com.autosoft.workflow.exec;

import com.autosoft.meta.runtime.WorkflowFormTriggerHook;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import com.autosoft.workflow.entity.WfDefinitionDO;
import com.autosoft.workflow.graph.GraphCodec;
import com.autosoft.workflow.graph.WorkflowGraph;
import com.autosoft.workflow.mapper.WfDefinitionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowFormTrigger适配器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class WorkflowFormTriggerAdapter implements WorkflowFormTriggerHook {

    private static final int LIMIT = 5;
    private static final Logger log = LoggerFactory.getLogger(WorkflowFormTriggerAdapter.class);

    private final WfDefinitionMapper definitionMapper;
    private final WorkflowDefinitionService definitionService;
    private final WorkflowExecutor executor;
    private final JsonMapper jsonMapper;

    public WorkflowFormTriggerAdapter(WfDefinitionMapper definitionMapper, WorkflowDefinitionService definitionService,
                                      WorkflowExecutor executor, JsonMapper jsonMapper) {
        this.definitionMapper = definitionMapper;
        this.definitionService = definitionService;
        this.executor = executor;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onRowSubmitted(String app, String entity, Long id, Map<String, Object> row) {
        List<WfDefinitionDO> defs = definitionMapper.selectList(new LambdaQueryWrapper<WfDefinitionDO>()
                .eq(WfDefinitionDO::getStatus, WfDefinitionDO.PUBLISHED));
        int fired = 0;
        Map<String, Object> input = toInput(row);
        input.put("id", id);
        for (WfDefinitionDO def : defs) {
            if (fired >= LIMIT) {
                break;
            }
            try {
                WorkflowGraph graph = GraphCodec.parse(definitionService.publishedGraphJson(def), jsonMapper);
                if (graph.getTrigger() == null || !"form".equals(graph.getTrigger().getType())) {
                    continue;
                }
                if (!app.equals(graph.getTrigger().getApp()) || !entity.equals(graph.getTrigger().getEntity())) {
                    continue;
                }
                executor.runPublishedInternal(def.getCode(), input);
                fired++;
            } catch (Exception ex) {
                log.warn("form 触发工作流失败 {}: {}", def.getCode(), ex.getMessage());
            }
        }
    }

    private Map<String, Object> toInput(Map<String, Object> row) {
        Map<String, Object> input = new LinkedHashMap<>();
        if (row == null) {
            return input;
        }
        row.forEach((k, v) -> input.put(k, v == null ? null : String.valueOf(v)));
        return input;
    }
}
