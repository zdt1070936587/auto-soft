package com.autosoft.agent.assistant.action;

import com.autosoft.agent.assistant.action.model.CapabilityDefinition;
import com.autosoft.agent.assistant.action.model.CapabilityField;
import com.autosoft.agent.assistant.action.model.FieldValidationResult;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 能力字段校验与 label/key 映射。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class ActionFieldValidator {

    private final RoleNameResolver roleNameResolver;

    public ActionFieldValidator(RoleNameResolver roleNameResolver) {
        this.roleNameResolver = roleNameResolver;
    }

    public FieldValidationResult validate(CapabilityDefinition capability, Map<String, Object> rawInput) {
        FieldValidationResult result = new FieldValidationResult();
        Map<String, Object> input = normalizeInput(capability, rawInput == null ? Map.of() : rawInput);
        Map<String, CapabilityField> fieldIndex = indexFields(capability.getFields());
        Set<String> knownKeys = fieldIndex.keySet();

        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            if (!knownKeys.contains(key)) {
                result.getUnknown().add(key);
            }
        }

        for (CapabilityField field : capability.getFields()) {
            Object raw = input.get(field.getKey());
            if (isEmpty(raw)) {
                if (field.getDefaultValue() != null) {
                    raw = field.getDefaultValue();
                }
            }
            if (isEmpty(raw)) {
                if (field.isRequired()) {
                    result.getMissing().add(field.getKey());
                }
                continue;
            }
            try {
                Object machine = convert(field, raw);
                result.getValues().put(field.getKey(), machine);
                result.getDisplayValues().put(field.getKey(), display(field, machine));
            } catch (BizException ex) {
                result.getFieldErrors().put(field.getKey(), ex.getMessage());
            }
        }

        result.setReady(result.getMissing().isEmpty()
                && result.getUnknown().isEmpty()
                && result.getFieldErrors().isEmpty());
        return result;
    }

    private Map<String, Object> normalizeInput(CapabilityDefinition capability, Map<String, Object> rawInput) {
        Map<String, String> labelToKey = new LinkedHashMap<>();
        for (CapabilityField field : capability.getFields()) {
            labelToKey.put(field.getKey(), field.getKey());
            if (field.getLabel() != null) {
                labelToKey.put(field.getLabel(), field.getKey());
            }
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        Set<String> knownKeys = new LinkedHashSet<>();
        for (CapabilityField field : capability.getFields()) {
            knownKeys.add(field.getKey());
        }
        for (Map.Entry<String, Object> entry : rawInput.entrySet()) {
            String rawKey = entry.getKey();
            String mapped = labelToKey.getOrDefault(rawKey, rawKey);
            if (knownKeys.contains(mapped)) {
                normalized.put(mapped, entry.getValue());
            } else {
                normalized.put(rawKey, entry.getValue());
            }
        }
        return normalized;
    }

    private static Map<String, CapabilityField> indexFields(List<CapabilityField> fields) {
        Map<String, CapabilityField> index = new LinkedHashMap<>();
        for (CapabilityField field : fields) {
            index.put(field.getKey(), field);
        }
        return index;
    }

    private Object convert(CapabilityField field, Object raw) {
        return switch (field.getType()) {
            case "int" -> convertInt(field, raw);
            case "decimal" -> convertDecimal(field, raw);
            case "datetime" -> convertDateTime(field, raw);
            case "enum" -> convertEnum(field, raw);
            case "role_ref" -> convertRoleRef(field, raw);
            case "bool" -> convertBool(field, raw);
            default -> convertString(field, raw);
        };
    }

    private Object convertString(CapabilityField field, Object raw) {
        String text = String.valueOf(raw).trim();
        if (field.getPattern() != null && !field.getPattern().isBlank()) {
            if (!Pattern.compile(field.getPattern()).matcher(text).matches()) {
                throw validationFailed(field, field.getHint() == null ? "格式不正确" : field.getHint());
            }
        }
        return text;
    }

    private Object convertInt(CapabilityField field, Object raw) {
        int value;
        if (raw instanceof Number number) {
            value = number.intValue();
        } else {
            value = Integer.parseInt(String.valueOf(raw).trim());
        }
        if (!field.getEnumValues().isEmpty() && !field.getEnumValues().contains(value)) {
            throw validationFailed(field, "取值不在允许范围内");
        }
        return value;
    }

    private Object convertDecimal(CapabilityField field, Object raw) {
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(raw).trim());
    }

    private Object convertDateTime(CapabilityField field, Object raw) {
        if (raw instanceof Instant instant) {
            return instant.toString();
        }
        String text = String.valueOf(raw).trim();
        try {
            return Instant.parse(text).toString();
        } catch (RuntimeException ignored) {
            return OffsetDateTime.parse(text).toInstant().toString();
        }
    }

    private Object convertEnum(CapabilityField field, Object raw) {
        String text = String.valueOf(raw).trim();
        for (Object allowed : field.getEnumValues()) {
            if (allowed != null && String.valueOf(allowed).equals(text)) {
                return allowed;
            }
        }
        throw validationFailed(field, "取值不在允许范围内");
    }

    private Object convertRoleRef(CapabilityField field, Object raw) {
        List<Long> ids = roleNameResolver.resolve(raw);
        if (ids.isEmpty() && field.isRequired()) {
            throw validationFailed(field, "角色不能为空");
        }
        if (field.isMulti()) {
            return ids;
        }
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Object convertBool(CapabilityField field, Object raw) {
        if (raw instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        if (raw instanceof Number number) {
            return number.intValue() == 0 ? 0 : 1;
        }
        String text = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(text) || "1".equals(text) || "是".equals(text) || "启用".equals(text)) {
            return 1;
        }
        if ("false".equals(text) || "0".equals(text) || "否".equals(text) || "禁用".equals(text)) {
            return 0;
        }
        throw validationFailed(field, "布尔值格式不正确");
    }

    private Object display(CapabilityField field, Object machine) {
        if ("role_ref".equals(field.getType())) {
            if (machine instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Long> ids = (List<Long>) list;
                if (field.isMulti()) {
                    return roleNameResolver.displayNameList(ids);
                }
                return roleNameResolver.displayNameList(ids).stream()
                        .findFirst().orElse("");
            }
            if (machine instanceof Number number) {
                return roleNameResolver.displayNameList(List.of(number.longValue())).stream()
                        .findFirst().orElse("");
            }
        }
        if ("int".equals(field.getType()) && "status".equals(field.getKey())) {
            return Integer.valueOf(1).equals(machine) ? "启用" : "禁用";
        }
        if ("bool".equals(field.getType())) {
            return Integer.valueOf(1).equals(machine) ? "是" : "否";
        }
        return machine;
    }

    private static boolean isEmpty(Object raw) {
        if (raw == null) {
            return true;
        }
        if (raw instanceof String text) {
            return text.isBlank();
        }
        if (raw instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }

    private static BizException validationFailed(CapabilityField field, String reason) {
        String label = field.getLabel() == null ? field.getKey() : field.getLabel();
        throw new BizException(ResultCode.BAD_REQUEST, "字段校验失败：" + label + " - " + reason);
    }
}
