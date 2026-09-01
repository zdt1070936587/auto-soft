package com.autosoft.workflow.graph;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 图 JSON 编解码。兼容 input_schema / inputSchema。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class GraphCodec {

    private GraphCodec() {
    }

    public static WorkflowGraph parse(String json, JsonMapper mapper) {
        if (json == null || json.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "图不能为空");
        }
        Map<String, Object> raw;
        try {
            raw = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (RuntimeException ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "图 JSON 不合法");
        }
        WorkflowGraph graph = new WorkflowGraph();
        graph.setVersion(asInt(raw.get("version"), 1));
        graph.setName(asStr(raw.get("name")));
        graph.setTrigger(parseTrigger(asMap(raw.get("trigger"))));
        graph.setNodes(parseNodes(raw.get("nodes")));
        graph.setEdges(parseEdges(raw.get("edges")));
        return graph;
    }

    public static String toJson(WorkflowGraph graph, JsonMapper mapper) {
        Map<String, Object> raw = toMap(graph);
        return mapper.writeValueAsString(raw);
    }

    public static Map<String, Object> toMap(WorkflowGraph graph) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("version", graph.getVersion());
        raw.put("name", graph.getName());
        Map<String, Object> trigger = new LinkedHashMap<>();
        WfTrigger t = graph.getTrigger() == null ? new WfTrigger() : graph.getTrigger();
        trigger.put("type", t.getType() == null ? "manual" : t.getType());
        trigger.put("input_schema", t.getInputSchema() == null ? Map.of() : t.getInputSchema());
        if (t.getApp() != null && !t.getApp().isBlank()) {
            trigger.put("app", t.getApp());
        }
        if (t.getEntity() != null && !t.getEntity().isBlank()) {
            trigger.put("entity", t.getEntity());
        }
        if (t.getCron() != null && !t.getCron().isBlank()) {
            trigger.put("cron", t.getCron());
        }
        if (t.getEnabled() != null) {
            trigger.put("enabled", t.getEnabled());
        }
        raw.put("trigger", trigger);
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (WfNode node : graph.getNodes()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", node.getId());
            item.put("type", node.getType());
            item.put("title", node.getTitle());
            item.put("config", node.getConfig() == null ? Map.of() : node.getConfig());
            nodes.add(item);
        }
        raw.put("nodes", nodes);
        List<Map<String, Object>> edges = new ArrayList<>();
        for (WfEdge edge : graph.getEdges()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("from", edge.getFrom());
            item.put("to", edge.getTo());
            if (edge.getWhen() != null && !edge.getWhen().isBlank()) {
                item.put("when", edge.getWhen());
            }
            edges.add(item);
        }
        raw.put("edges", edges);
        return raw;
    }

    public static WorkflowGraph empty(String name) {
        WorkflowGraph graph = new WorkflowGraph();
        graph.setVersion(1);
        graph.setName(name);
        WfTrigger trigger = new WfTrigger();
        trigger.setType("manual");
        graph.setTrigger(trigger);
        WfNode start = new WfNode();
        start.setId("start");
        start.setType(NodeTypes.START);
        start.setTitle("开始");
        WfNode end = new WfNode();
        end.setId("end");
        end.setType(NodeTypes.END);
        end.setTitle("结束");
        graph.getNodes().add(start);
        graph.getNodes().add(end);
        return graph;
    }

    private static WfTrigger parseTrigger(Map<String, Object> raw) {
        WfTrigger trigger = new WfTrigger();
        trigger.setType(asStr(raw.get("type")));
        if (trigger.getType() == null || trigger.getType().isBlank()) {
            trigger.setType("manual");
        }
        Object schema = raw.get("input_schema");
        if (schema == null) {
            schema = raw.get("inputSchema");
        }
        Map<String, String> input = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : asMap(schema).entrySet()) {
            input.put(entry.getKey(), entry.getValue() == null ? "string" : String.valueOf(entry.getValue()));
        }
        trigger.setInputSchema(input);
        trigger.setApp(asStr(raw.get("app")));
        trigger.setEntity(asStr(raw.get("entity")));
        trigger.setCron(asStr(raw.get("cron")));
        Object enabled = raw.get("enabled");
        if (enabled instanceof Boolean b) {
            trigger.setEnabled(b ? 1 : 0);
        } else if (enabled instanceof Number n) {
            trigger.setEnabled(n.intValue());
        } else if (enabled != null && !String.valueOf(enabled).isBlank()) {
            trigger.setEnabled("false".equalsIgnoreCase(String.valueOf(enabled)) || "0".equals(String.valueOf(enabled)) ? 0 : 1);
        }
        return trigger;
    }

    @SuppressWarnings("unchecked")
    private static List<WfNode> parseNodes(Object raw) {
        List<WfNode> nodes = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return nodes;
        }
        for (Object item : list) {
            Map<String, Object> map = asMap(item);
            WfNode node = new WfNode();
            node.setId(asStr(map.get("id")));
            node.setType(asStr(map.get("type")));
            node.setTitle(asStr(map.get("title")));
            node.setConfig(asMap(map.get("config")));
            nodes.add(node);
        }
        return nodes;
    }

    private static List<WfEdge> parseEdges(Object raw) {
        List<WfEdge> edges = new ArrayList<>();
        if (!(raw instanceof List<?> list)) {
            return edges;
        }
        for (Object item : list) {
            Map<String, Object> map = asMap(item);
            WfEdge edge = new WfEdge();
            edge.setFrom(asStr(map.get("from")));
            edge.setTo(asStr(map.get("to")));
            edge.setWhen(asStr(map.get("when")));
            edges.add(edge);
        }
        return edges;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private static String asStr(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
