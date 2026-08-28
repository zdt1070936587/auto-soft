package com.autosoft.agent.assistant.tool.impl;

import com.autosoft.agent.assistant.memory.MemoryService;
import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RememberFactTool implements AssistantTool {

    private final MemoryService memoryService;
    private final JsonMapper jsonMapper;

    public RememberFactTool(MemoryService memoryService, JsonMapper jsonMapper) {
        this.memoryService = memoryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "remember_fact";
    }

    @Override
    public String description() {
        return "记住用户告知的画像或偏好信息。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("category", prop("string", "PROFILE | PREFERENCE | PROJECT"));
        props.put("fact_key", prop("string", "name/role/team/preference_*"));
        props.put("fact_value", prop("string", "要记住的内容"));
        props.put("confidence", prop("number", "置信度 0~1，默认 0.8"));
        return objectSchema(props, List.of("category", "fact_key", "fact_value"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        String category = str(args.get("category"));
        String factKey = str(args.get("fact_key"));
        String factValue = str(args.get("fact_value"));
        float confidence = floatVal(args.get("confidence"), 0.8f);
        boolean confirmed = confidence >= 0.9f && "PROFILE".equalsIgnoreCase(category);
        memoryService.upsertFact(context.getUserId(), category.toUpperCase(), factKey, factValue,
                confidence, confirmed, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("confirmed", confirmed);
        return AssistantToolRegistry.json(jsonMapper, result);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static float floatVal(Object value, float defaultValue) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }
}
