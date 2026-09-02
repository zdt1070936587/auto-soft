package com.autosoft.agent.assistant.action.tool.impl;

import com.autosoft.agent.assistant.action.CapabilityDiscoveryService;
import com.autosoft.agent.assistant.action.model.CapabilityDefinition;
import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取能力 schema 工具。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class GetCapabilitySchemaTool implements AssistantTool {

    private final CapabilityDiscoveryService discoveryService;
    private final JsonMapper jsonMapper;

    public GetCapabilitySchemaTool(CapabilityDiscoveryService discoveryService, JsonMapper jsonMapper) {
        this.discoveryService = discoveryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "get_capability_schema";
    }

    @Override
    public String description() {
        return "获取指定 capability 的完整字段定义，用于准备操作草稿。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("capabilityId", AssistantTool.prop("string", "能力 ID，如 system.user.create"));
        return AssistantTool.objectSchema(props, List.of("capabilityId"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        String capabilityId = str(args.get("capabilityId"));
        CapabilityDefinition schema = discoveryService.schemaForResponse(capabilityId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "capability_schema");
        payload.put("schema", schema);
        return AssistantToolRegistry.json(jsonMapper, payload);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
