package com.autosoft.agent.assistant.action.tool.impl;

import com.autosoft.agent.assistant.action.ActionDraftService;
import com.autosoft.agent.assistant.action.vo.ActionDraftVO;
import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 准备操作草稿工具。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class PrepareActionDraftTool implements AssistantTool {

    private final ActionDraftService draftService;
    private final JsonMapper jsonMapper;

    public PrepareActionDraftTool(ActionDraftService draftService, JsonMapper jsonMapper) {
        this.draftService = draftService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "prepare_action_draft";
    }

    @Override
    public String description() {
        return "根据 capability 与用户提供的字段值创建或更新操作草稿。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("capabilityId", AssistantTool.prop("string", "能力 ID"));
        props.put("fieldValues", AssistantTool.prop("object", "字段值，key 可为 field key 或中文 label"));
        props.put("draftId", AssistantTool.prop("string", "续聊补全时传入已有 draftId"));
        return AssistantTool.objectSchema(props, List.of("capabilityId"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        String capabilityId = str(args.get("capabilityId"));
        String draftId = str(args.get("draftId"));
        Map<String, Object> fieldValues = Map.of();
        Object rawValues = args.get("fieldValues");
        if (rawValues instanceof Map<?, ?> map) {
            fieldValues = (Map<String, Object>) map;
        }
        ActionDraftService.PrepareResult result = draftService.createOrUpdate(
                context.getSessionId(), context.getUserId(), capabilityId, fieldValues, blankToNull(draftId));
        ActionDraftVO draft = result.draft();
        context.setLastDraft(draft);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "action_draft");
        payload.put("draftId", draft.getDraftId());
        payload.put("status", draft.getStatus());
        payload.put("missing", draft.getMissing());
        payload.put("unknown", draft.getUnknown());
        payload.put("message", result.message());
        return AssistantToolRegistry.json(jsonMapper, payload);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
