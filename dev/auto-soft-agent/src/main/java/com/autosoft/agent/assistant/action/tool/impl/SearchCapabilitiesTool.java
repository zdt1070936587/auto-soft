package com.autosoft.agent.assistant.action.tool.impl;

import com.autosoft.agent.assistant.action.CapabilityDiscoveryService;
import com.autosoft.agent.assistant.action.model.CapabilitySearchResult;
import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索可执行能力工具。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class SearchCapabilitiesTool implements AssistantTool {

    private final CapabilityDiscoveryService discoveryService;
    private final JsonMapper jsonMapper;

    public SearchCapabilitiesTool(CapabilityDiscoveryService discoveryService, JsonMapper jsonMapper) {
        this.discoveryService = discoveryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "search_capabilities";
    }

    @Override
    public String description() {
        return "按关键词与操作意图搜索当前用户可执行的能力（系统注册 + 已发布 runtime）。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("keyword", AssistantTool.prop("string", "如「用户」「请假单」"));
        props.put("intent", AssistantTool.prop("string", "P0 仅 create"));
        props.put("limit", AssistantTool.prop("integer", "最多返回条数，默认 5"));
        return AssistantTool.objectSchema(props, List.of("keyword", "intent"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        String keyword = str(args.get("keyword"));
        String intent = str(args.get("intent"));
        int limit = intVal(args.get("limit"), 5);
        CapabilitySearchResult result = discoveryService.search(keyword, intent, limit, context.getUserId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "capability_search");
        payload.put("items", result.getItems());
        payload.put("ambiguous", result.isAmbiguous());
        return AssistantToolRegistry.json(jsonMapper, payload);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intVal(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}
