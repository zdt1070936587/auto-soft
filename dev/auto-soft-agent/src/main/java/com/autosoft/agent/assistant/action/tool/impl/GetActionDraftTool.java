package com.autosoft.agent.assistant.action.tool.impl;

import com.autosoft.agent.assistant.action.ActionDraftService;
import com.autosoft.agent.assistant.action.vo.ActionDraftVO;
import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取操作草稿工具。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class GetActionDraftTool implements AssistantTool {

    private final ActionDraftService draftService;
    private final JsonMapper jsonMapper;

    public GetActionDraftTool(ActionDraftService draftService, JsonMapper jsonMapper) {
        this.draftService = draftService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "get_action_draft";
    }

    @Override
    public String description() {
        return "获取指定 draftId 的操作草稿详情。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("draftId", AssistantTool.prop("string", "草稿 UUID"));
        return AssistantTool.objectSchema(props, List.of("draftId"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        String draftId = str(args.get("draftId"));
        try {
            ActionDraftVO draft = draftService.get(draftId, context.getUserId());
            context.setLastDraft(draft);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "action_draft_detail");
            payload.put("draftId", draft.getDraftId());
            payload.put("sessionId", draft.getSessionId());
            payload.put("capabilityId", draft.getCapabilityId());
            payload.put("status", draft.getStatus());
            payload.put("label", draft.getLabel());
            payload.put("targetPath", draft.getTargetPath());
            payload.put("targetType", draft.getTargetType());
            payload.put("modalKey", draft.getModalKey());
            payload.put("permission", draft.getPermission());
            payload.put("values", draft.getValues());
            payload.put("displayValues", draft.getDisplayValues());
            payload.put("missing", draft.getMissing());
            payload.put("unknown", draft.getUnknown());
            payload.put("expiresAt", draft.getExpiresAt());
            return AssistantToolRegistry.json(jsonMapper, payload);
        } catch (com.autosoft.common.exception.BizException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("过期")) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("error", "draft_expired");
                return AssistantToolRegistry.json(jsonMapper, payload);
            }
            throw ex;
        }
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
