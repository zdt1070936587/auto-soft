package com.autosoft.agent.assistant.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局助手专用工具接口（与 Studio AgentTool 分离）。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public interface AssistantTool {

    String name();

    String description();

    Map<String, Object> parametersSchema();

    String execute(AssistantToolContext context, Map<String, Object> args);

    static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    static Map<String, Object> prop(String type, String description) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("description", description);
        return map;
    }
}
