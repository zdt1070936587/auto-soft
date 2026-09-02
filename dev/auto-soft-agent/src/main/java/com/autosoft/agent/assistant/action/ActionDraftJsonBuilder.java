package com.autosoft.agent.assistant.action;

import com.autosoft.agent.assistant.action.model.CapabilityDefinition;
import com.autosoft.agent.assistant.action.model.CapabilityField;
import com.autosoft.agent.assistant.action.vo.ActionDraftVO;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作草稿 SSE / structured 载荷构建。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
public final class ActionDraftJsonBuilder {

    private ActionDraftJsonBuilder() {
    }

    public static Map<String, Object> buildActionMissing(ActionDraftVO draft, CapabilityDefinition capability) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("draftId", draft.getDraftId());
        payload.put("capabilityId", draft.getCapabilityId());
        payload.put("label", draft.getLabel());
        payload.put("missing", buildMissingItems(draft.getMissing(), capability));
        payload.put("unknown", buildUnknownItems(draft.getUnknown(), capability));
        payload.put("filled", buildFilledItems(draft, capability));
        return payload;
    }

    public static String buildActionPlanJson(JsonMapper jsonMapper, ActionDraftVO draft, CapabilityDefinition capability) {
        Map<String, Object> payload = buildActionPlan(draft, capability);
        return jsonMapper.writeValueAsString(payload);
    }

    public static Map<String, Object> buildActionPlan(ActionDraftVO draft, CapabilityDefinition capability) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "action_plan");
        payload.put("draftId", draft.getDraftId());
        payload.put("capabilityId", draft.getCapabilityId());
        payload.put("label", draft.getLabel());
        payload.put("targetPath", draft.getTargetPath());
        payload.put("targetType", draft.getTargetType());
        payload.put("summary", "将前往「" + pageLabel(draft) + "」并打开新建表单");
        payload.put("fields", buildPlanFields(draft, capability));
        payload.put("canConfirm", "ready".equals(draft.getStatus()));
        return payload;
    }

    private static List<Map<String, Object>> buildMissingItems(List<String> missing, CapabilityDefinition capability) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (String key : missing) {
            CapabilityField field = findField(capability, key);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", key);
            item.put("label", field == null ? key : field.getLabel());
            items.add(item);
        }
        return items;
    }

    private static List<Map<String, Object>> buildUnknownItems(List<String> unknown, CapabilityDefinition capability) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (String name : unknown) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("label", name);
            item.put("reason", "field_not_supported");
            items.add(item);
        }
        return items;
    }

    private static List<Map<String, Object>> buildFilledItems(ActionDraftVO draft, CapabilityDefinition capability) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, Object> entry : draft.getDisplayValues().entrySet()) {
            CapabilityField field = findField(capability, entry.getKey());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", entry.getKey());
            item.put("label", field == null ? entry.getKey() : field.getLabel());
            if (field != null && field.isSensitive()) {
                item.put("display", "********");
            } else {
                item.put("display", entry.getValue());
            }
            items.add(item);
        }
        return items;
    }

    private static List<Map<String, Object>> buildPlanFields(ActionDraftVO draft, CapabilityDefinition capability) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<String, Object> entry : draft.getDisplayValues().entrySet()) {
            CapabilityField field = findField(capability, entry.getKey());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", field == null ? entry.getKey() : field.getLabel());
            if (field != null && field.isSensitive()) {
                item.put("display", "********");
            } else {
                item.put("display", entry.getValue());
            }
            items.add(item);
        }
        for (String key : draft.getMissing()) {
            CapabilityField field = findField(capability, key);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", field == null ? key : field.getLabel());
            item.put("display", "（待填写）");
            items.add(item);
        }
        return items;
    }

    private static CapabilityField findField(CapabilityDefinition capability, String key) {
        if (capability == null || capability.getFields() == null) {
            return null;
        }
        for (CapabilityField field : capability.getFields()) {
            if (key.equals(field.getKey())) {
                return field;
            }
        }
        return null;
    }

    private static String pageLabel(ActionDraftVO draft) {
        if ("system_modal".equals(draft.getTargetType())) {
            return "用户管理";
        }
        return draft.getLabel() == null ? draft.getTargetPath() : draft.getLabel();
    }
}
