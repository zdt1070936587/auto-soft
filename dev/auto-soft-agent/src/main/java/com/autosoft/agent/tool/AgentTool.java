package com.autosoft.agent.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 可调用的平台工具。模型不能发明未注册名称。
 */
public interface AgentTool {

    String name();

    String description();

    Map<String, Object> parametersSchema();

    String execute(ToolContext context, ToolArgs args);

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
