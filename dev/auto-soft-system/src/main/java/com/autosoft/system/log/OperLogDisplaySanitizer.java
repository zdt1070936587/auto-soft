package com.autosoft.system.log;

import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志详情展示脱敏。
 */
@Component
public class OperLogDisplaySanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "oldpassword", "newpassword", "apikey", "api_key",
            "cipher", "token", "secret", "authorization");

    private final JsonMapper jsonMapper;

    public OperLogDisplaySanitizer(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String sanitizeDetail(String detailJson) {
        if (detailJson == null || detailJson.isBlank()) {
            return detailJson;
        }
        try {
            Map<String, Object> map = jsonMapper.readValue(detailJson, new TypeReference<Map<String, Object>>() {
            });
            Map<String, Object> cleaned = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (isSensitive(entry.getKey())) {
                    cleaned.put(entry.getKey(), "***");
                } else {
                    cleaned.put(entry.getKey(), entry.getValue());
                }
            }
            return jsonMapper.writeValueAsString(cleaned);
        } catch (RuntimeException ex) {
            return detailJson.length() > 500 ? detailJson.substring(0, 500) + "…" : detailJson;
        }
    }

    private boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("_", "");
        for (String sensitive : SENSITIVE_KEYS) {
            if (normalized.contains(sensitive.replace("_", ""))) {
                return true;
            }
        }
        return false;
    }
}
