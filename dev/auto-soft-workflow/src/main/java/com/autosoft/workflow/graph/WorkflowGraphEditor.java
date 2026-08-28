package com.autosoft.workflow.graph;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.meta.ddl.Identifiers;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WorkflowGraphEditor {

    private WorkflowGraphEditor() {
    }

    public static void setTrigger(WorkflowGraph graph, String type, Map<String, String> inputSchema,
                                  String app, String entity, String cron, Boolean enabled) {
        String resolved = type == null || type.isBlank() ? "manual" : type;
        AssertUtils.isTrue(NodeTypes.TRIGGER_TYPES.contains(resolved), "不支持的触发类型: " + resolved);
        WfTrigger trigger = graph.getTrigger() == null ? new WfTrigger() : graph.getTrigger();
        trigger.setType(resolved);
        if (inputSchema != null) {
            Map<String, String> copy = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : inputSchema.entrySet()) {
                Identifiers.assertCode(entry.getKey(), "input");
                copy.put(entry.getKey(), entry.getValue() == null || entry.getValue().isBlank() ? "string" : entry.getValue());
            }
            trigger.setInputSchema(copy);
        }
        if (app != null) {
            trigger.setApp(app);
        }
        if (entity != null) {
            trigger.setEntity(entity);
        }
        if (cron != null) {
            trigger.setCron(cron);
        }
        if (enabled != null) {
            trigger.setEnabled(enabled ? 1 : 0);
        }
        graph.setTrigger(trigger);
    }

    public static void addNode(WorkflowGraph graph, String id, String type, String title, Map<String, Object> config) {
        Identifiers.assertCode(id, "nodeId");
        AssertUtils.notBlank(type, "type 不能为空");
        if (graph.getNodes().stream().anyMatch(n -> id.equals(n.getId()))) {
            throw new BizException(ResultCode.BAD_REQUEST, "节点已存在: " + id);
        }
        WfNode node = new WfNode();
        node.setId(id);
        node.setType(type);
        node.setTitle(title == null || title.isBlank() ? id : title);
        node.setConfig(config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config));
        graph.getNodes().add(node);
    }

    public static void updateNode(WorkflowGraph graph, String id, String title, Map<String, Object> config) {
        WfNode node = requireNode(graph, id);
        if (title != null && !title.isBlank()) {
            node.setTitle(title);
        }
        if (config != null) {
            node.setConfig(new LinkedHashMap<>(config));
        }
    }

    public static void removeNode(WorkflowGraph graph, String id) {
        WfNode node = requireNode(graph, id);
        if (NodeTypes.START.equals(node.getType()) || NodeTypes.END.equals(node.getType())) {
            throw new BizException(ResultCode.BAD_REQUEST, "不能删除 start/end");
        }
        graph.getNodes().removeIf(n -> id.equals(n.getId()));
        graph.getEdges().removeIf(e -> id.equals(e.getFrom()) || id.equals(e.getTo()));
    }

    public static void connect(WorkflowGraph graph, String from, String to, String when) {
        WfNode fromNode = requireNode(graph, from);
        requireNode(graph, to);
        String normalized = when == null || when.isBlank() ? null : when.trim();
        if (NodeTypes.CONDITION.equals(fromNode.getType())) {
            AssertUtils.isTrue("true".equals(normalized) || "false".equals(normalized),
                    "condition 连线必须 when=true 或 false");
            graph.getEdges().removeIf(e -> from.equals(e.getFrom()) && normalized.equals(e.getWhen()));
        } else if ("error".equals(normalized)) {
            graph.getEdges().removeIf(e -> from.equals(e.getFrom()) && "error".equals(e.getWhen()));
        } else {
            graph.getEdges().removeIf(e -> from.equals(e.getFrom()) && !"error".equals(e.getWhen()));
        }
        WfEdge edge = new WfEdge();
        edge.setFrom(from);
        edge.setTo(to);
        edge.setWhen(normalized);
        graph.getEdges().add(edge);
    }

    public static void disconnect(WorkflowGraph graph, String from, String to) {
        Iterator<WfEdge> it = graph.getEdges().iterator();
        while (it.hasNext()) {
            WfEdge edge = it.next();
            if (from.equals(edge.getFrom()) && (to == null || to.equals(edge.getTo()))) {
                it.remove();
            }
        }
    }

    private static WfNode requireNode(WorkflowGraph graph, String id) {
        return graph.getNodes().stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.NOT_FOUND, "节点不存在: " + id));
    }
}
