package com.autosoft.meta.page;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LowCodeSchemaValidator {

    private static final Set<String> BLOCK_TYPES = Set.of(
            "textarea", "text", "button", "toolbar", "card", "divider");
    private static final Set<String> LAYOUTS = Set.of("admin", "h5", "blank");

    private LowCodeSchemaValidator() {
    }

    public static void validate(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "低代码页面 schema 不能为空");
        }
        JsonMapper mapper = new JsonMapper();
        Map<String, Object> root;
        try {
            root = mapper.readValue(schemaJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "低代码 schema 不是合法 JSON");
        }
        Object version = root.get("version");
        if (!(version instanceof Number) || ((Number) version).intValue() != 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "低代码 schema version 必须为 1");
        }
        String layout = str(root.get("layout"));
        if (layout != null && !LAYOUTS.contains(layout)) {
            throw new BizException(ResultCode.BAD_REQUEST, "layout 必须是 admin/h5/blank");
        }
        Object blocksObj = root.get("blocks");
        if (!(blocksObj instanceof List<?> blocks) || blocks.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "blocks 不能为空");
        }
        for (Object blockObj : blocks) {
            if (!(blockObj instanceof Map<?, ?> block)) {
                throw new BizException(ResultCode.BAD_REQUEST, "block 必须是对象");
            }
            String type = str(block.get("type"));
            if (type == null || !BLOCK_TYPES.contains(type)) {
                throw new BizException(ResultCode.BAD_REQUEST, "不支持的 block 类型: " + type);
            }
        }
    }

    public static String extractTitle(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return null;
        }
        try {
            JsonMapper mapper = new JsonMapper();
            Map<String, Object> root = mapper.readValue(schemaJson, new TypeReference<Map<String, Object>>() {
            });
            return str(root.get("title"));
        } catch (Exception ex) {
            return null;
        }
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
