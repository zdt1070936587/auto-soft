package com.autosoft.workflow.graph;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.ddl.Identifiers;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.mapper.RoleMapper;
import com.autosoft.workflow.http.WorkflowHttpHostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 阶段 B/C 图校验。未知类型与环直接拒绝。
 */
@Component
public class WorkflowGraphValidator {

    private static final Pattern TEMPLATE = Pattern.compile("\\{\\{\\s*([a-z][a-z0-9_]*)(?:\\.([A-Za-z0-9_]+))?\\s*}}");

    private final MetaCatalogService catalogService;
    private final RoleMapper roleMapper;
    private final WorkflowHttpHostService httpHostService;

    public WorkflowGraphValidator(MetaCatalogService catalogService, RoleMapper roleMapper,
                                  WorkflowHttpHostService httpHostService) {
        this.catalogService = catalogService;
        this.roleMapper = roleMapper;
        this.httpHostService = httpHostService;
    }

    public void validate(WorkflowGraph graph) {
        AssertUtils.notNull(graph, "图不能为空");
        AssertUtils.isTrue(graph.getVersion() == 1, "不支持的 IR 版本");
        AssertUtils.notNull(graph.getTrigger(), "缺少 trigger");
        String triggerType = graph.getTrigger().getType();
        AssertUtils.isTrue(NodeTypes.TRIGGER_TYPES.contains(triggerType), "不支持的触发类型: " + triggerType);
        validateTrigger(graph.getTrigger());
        AssertUtils.notNull(graph.getNodes(), "缺少 nodes");
        AssertUtils.notNull(graph.getEdges(), "缺少 edges");

        Map<String, WfNode> byId = new HashMap<>();
        int startCount = 0;
        int endCount = 0;
        for (WfNode node : graph.getNodes()) {
            AssertUtils.notBlank(node.getId(), "节点 id 不能为空");
            Identifiers.assertCode(node.getId(), "nodeId");
            AssertUtils.notBlank(node.getType(), "节点 type 不能为空");
            if (!NodeTypes.PHASE_C.contains(node.getType())) {
                throw new BizException(ResultCode.BAD_REQUEST, "未注册的节点类型: " + node.getType());
            }
            if (byId.put(node.getId(), node) != null) {
                throw new BizException(ResultCode.BAD_REQUEST, "节点 id 重复: " + node.getId());
            }
            if (NodeTypes.START.equals(node.getType())) {
                startCount++;
            }
            if (NodeTypes.END.equals(node.getType())) {
                endCount++;
            }
            validateNodeConfig(node);
        }
        AssertUtils.isTrue(startCount == 1, "必须恰好一个 start 节点");
        AssertUtils.isTrue(endCount >= 1, "至少需要一个 end 节点");

        Map<String, List<WfEdge>> outgoing = new HashMap<>();
        Map<String, List<String>> outgoingIds = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (String id : byId.keySet()) {
            outgoing.put(id, new ArrayList<>());
            outgoingIds.put(id, new ArrayList<>());
            indegree.put(id, 0);
        }
        for (WfEdge edge : graph.getEdges()) {
            AssertUtils.notBlank(edge.getFrom(), "边 from 不能为空");
            AssertUtils.notBlank(edge.getTo(), "边 to 不能为空");
            AssertUtils.isTrue(byId.containsKey(edge.getFrom()), "边引用未知节点: " + edge.getFrom());
            AssertUtils.isTrue(byId.containsKey(edge.getTo()), "边引用未知节点: " + edge.getTo());
            outgoing.get(edge.getFrom()).add(edge);
            outgoingIds.get(edge.getFrom()).add(edge.getTo());
            indegree.put(edge.getTo(), indegree.get(edge.getTo()) + 1);
        }

        WfNode start = graph.getNodes().stream()
                .filter(n -> NodeTypes.START.equals(n.getType()))
                .findFirst()
                .orElseThrow();
        AssertUtils.isTrue(indegree.get(start.getId()) == 0, "start 不能有入边");
        for (WfNode node : graph.getNodes()) {
            validateOutgoing(node, outgoing.get(node.getId()));
        }

        assertAcyclic(byId.keySet(), outgoingIds, indegree);
        assertReachable(start.getId(), byId.keySet(), outgoingIds);
        validateTemplates(graph, byId, outgoingIds);
    }

    private void validateTrigger(WfTrigger trigger) {
        if ("form".equals(trigger.getType())) {
            AssertUtils.notBlank(trigger.getApp(), "form 触发需要已发布 app");
            AssertUtils.notBlank(trigger.getEntity(), "form 触发需要已发布 entity");
            Identifiers.assertCode(trigger.getApp(), "app");
            Identifiers.assertCode(trigger.getEntity(), "entity");
            MetaAppDO metaApp = catalogService.requireAppByCode(trigger.getApp());
            AssertUtils.isTrue(MetaAppDO.PUBLISHED.equals(metaApp.getStatus()), "form 触发只能绑定已发布应用");
            catalogService.requireEntity(trigger.getApp(), trigger.getEntity());
        }
        if ("cron".equals(trigger.getType())) {
            AssertUtils.notBlank(trigger.getCron(), "cron 触发需要表达式");
            try {
                CronExpression.parse(normalizeCron(trigger.getCron()));
            } catch (IllegalArgumentException ex) {
                throw new BizException(ResultCode.BAD_REQUEST, "cron 表达式不合法");
            }
        }
    }

    private void validateOutgoing(WfNode node, List<WfEdge> edges) {
        if (NodeTypes.END.equals(node.getType())) {
            AssertUtils.isTrue(edges.isEmpty(), "end 不能有出边");
            return;
        }
        if (NodeTypes.CONDITION.equals(node.getType())) {
            AssertUtils.isTrue(edges.size() == 2, "condition 必须恰好两条出边: " + node.getId());
            boolean hasTrue = edges.stream().anyMatch(e -> "true".equals(e.getWhen()));
            boolean hasFalse = edges.stream().anyMatch(e -> "false".equals(e.getWhen()));
            AssertUtils.isTrue(hasTrue && hasFalse, "condition 必须同时有 when=true 与 when=false: " + node.getId());
            return;
        }
        long error = edges.stream().filter(e -> "error".equals(e.getWhen())).count();
        long success = edges.size() - error;
        AssertUtils.isTrue(error <= 1, "最多一条 error 出边: " + node.getId());
        AssertUtils.isTrue(success == 1, "每个非结束节点必须恰好一条成功出边: " + node.getId());
    }

    private void validateNodeConfig(WfNode node) {
        Map<String, Object> config = node.getConfig() == null ? Map.of() : node.getConfig();
        if (NodeTypes.META_QUERY.equals(node.getType()) || NodeTypes.META_UPSERT.equals(node.getType())
                || NodeTypes.APPROVAL.equals(node.getType())) {
            String app = str(config.get("app"));
            String entity = str(config.get("entity"));
            AssertUtils.notBlank(app, node.getType() + " 需要 app");
            AssertUtils.notBlank(entity, node.getType() + " 需要 entity");
            Identifiers.assertCode(app, "app");
            Identifiers.assertCode(entity, "entity");
            MetaAppDO metaApp = catalogService.requireAppByCode(app);
            AssertUtils.isTrue(MetaAppDO.PUBLISHED.equals(metaApp.getStatus()), node.getType() + " 只能引用已发布应用");
            catalogService.requireEntity(app, entity);
        }
        if (NodeTypes.NOTIFY.equals(node.getType())) {
            AssertUtils.notBlank(str(config.get("toRole")), "notify 需要 toRole");
        }
        if (NodeTypes.LLM.equals(node.getType()) || NodeTypes.HTTP.equals(node.getType())) {
            String blob = String.valueOf(config);
            AssertUtils.isTrue(!blob.toLowerCase().contains("apikey") && !blob.toLowerCase().contains("api_key"),
                    node.getType() + " 节点禁止包含 API Key");
        }
        if (NodeTypes.CONDITION.equals(node.getType())) {
            ConditionDsl.validate(str(config.get("expr")));
        }
        if (NodeTypes.APPROVAL.equals(node.getType())) {
            List<String> roles = roleCodes(config.get("role_codes"));
            AssertUtils.isTrue(!roles.isEmpty() && roles.size() <= 3, "approval 需要 1-3 个 role_codes");
            for (String code : roles) {
                AssertUtils.notNull(roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, code)),
                        "角色不存在: " + code);
            }
        }
        if (NodeTypes.HTTP.equals(node.getType())) {
            String url = str(config.get("url"));
            AssertUtils.notBlank(url, "http 需要 url");
            AssertUtils.isTrue(!url.contains("{{"), "http url 禁止模板，避免 SSRF");
            String method = str(config.get("method"));
            if (method != null && !method.isBlank()) {
                AssertUtils.isTrue("GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method), "http 仅允许 GET/POST");
            }
            HttpHostGuard.assertAllowed(url, httpHostService.listAllowedHosts());
        }
    }

    private void assertAcyclic(Set<String> ids, Map<String, List<String>> outgoing, Map<String, Integer> indegree) {
        Map<String, Integer> deg = new HashMap<>(indegree);
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (String id : ids) {
            if (deg.get(id) == 0) {
                queue.add(id);
            }
        }
        int seen = 0;
        while (!queue.isEmpty()) {
            String cur = queue.removeFirst();
            seen++;
            for (String next : outgoing.get(cur)) {
                deg.put(next, deg.get(next) - 1);
                if (deg.get(next) == 0) {
                    queue.add(next);
                }
            }
        }
        AssertUtils.isTrue(seen == ids.size(), "图中存在环");
    }

    private void assertReachable(String startId, Set<String> ids, Map<String, List<String>> outgoing) {
        Set<String> seen = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(startId);
        seen.add(startId);
        while (!queue.isEmpty()) {
            String cur = queue.removeFirst();
            for (String next : outgoing.get(cur)) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        AssertUtils.isTrue(seen.size() == ids.size(), "存在从 start 不可达的节点");
    }

    private void validateTemplates(WorkflowGraph graph, Map<String, WfNode> byId, Map<String, List<String>> outgoing) {
        Map<String, Set<String>> ancestors = ancestors(graph, outgoing);
        for (WfNode node : graph.getNodes()) {
            String blob = String.valueOf(node.getConfig());
            Matcher matcher = TEMPLATE.matcher(blob);
            while (matcher.find()) {
                String ref = matcher.group(1);
                if ("input".equals(ref)) {
                    continue;
                }
                AssertUtils.isTrue(byId.containsKey(ref), "模板引用未知节点: " + ref);
                AssertUtils.isTrue(ancestors.getOrDefault(node.getId(), Set.of()).contains(ref),
                        "模板只能引用上游节点: " + ref);
            }
        }
    }

    private Map<String, Set<String>> ancestors(WorkflowGraph graph, Map<String, List<String>> outgoing) {
        Map<String, Set<String>> result = new HashMap<>();
        for (WfNode node : graph.getNodes()) {
            result.put(node.getId(), new HashSet<>());
        }
        ArrayDeque<String> queue = new ArrayDeque<>();
        WfNode start = graph.getNodes().stream().filter(n -> NodeTypes.START.equals(n.getType())).findFirst().orElseThrow();
        queue.add(start.getId());
        while (!queue.isEmpty()) {
            String cur = queue.removeFirst();
            for (String next : outgoing.get(cur)) {
                Set<String> nextAnc = result.get(next);
                int before = nextAnc.size();
                nextAnc.add(cur);
                nextAnc.addAll(result.get(cur));
                if (nextAnc.size() != before) {
                    queue.add(next);
                }
            }
        }
        return result;
    }

    public static String normalizeCron(String cron) {
        String text = cron.trim();
        if (text.split("\\s+").length == 5) {
            return "0 " + text;
        }
        return text;
    }

    @SuppressWarnings("unchecked")
    public static List<String> roleCodes(Object raw) {
        List<String> result = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    result.add(String.valueOf(item).trim());
                }
            }
            return result;
        }
        if (raw == null) {
            return result;
        }
        for (String part : String.valueOf(raw).split(",")) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
