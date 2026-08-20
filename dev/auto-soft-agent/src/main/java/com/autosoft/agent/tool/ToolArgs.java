package com.autosoft.agent.tool;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析模型给出的工具参数。不信任模型，调用方仍走 Meta 校验。
 */
public class ToolArgs {

    private final Map<String, Object> map;

    public ToolArgs(Map<String, Object> map) {
        this.map = map == null ? Map.of() : map;
    }

    @SuppressWarnings("unchecked")
    public static ToolArgs parse(String json, JsonMapper jsonMapper) {
        if (json == null || json.isBlank()) {
            return new ToolArgs(Map.of());
        }
        try {
            Map<String, Object> map = jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            return new ToolArgs(map);
        } catch (RuntimeException ex) {
            throw new BizException(ResultCode.BAD_REQUEST, "工具参数不是合法 JSON");
        }
    }

    public String str(String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public String str(String key, String defaultValue) {
        String value = str(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public String requireStr(String key) {
        String value = str(key);
        AssertUtils.notBlank(value, "缺少参数 " + key);
        return value;
    }

    public Long lng(String key) {
        Object value = map.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new BizException(ResultCode.BAD_REQUEST, key + " 不是数字");
        }
    }

    public Long requireLng(String key) {
        Long value = lng(key);
        AssertUtils.notNull(value, "缺少参数 " + key);
        return value;
    }

    public Integer integer(String key) {
        Long value = lng(key);
        return value == null ? null : value.intValue();
    }

    public boolean bool(String key) {
        Object value = map.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return "true".equalsIgnoreCase(str(key));
    }

    @SuppressWarnings("unchecked")
    public List<String> strList(String key) {
        Object value = map.get(key);
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        String raw = str(key);
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String part : raw.split(",")) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    public Map<String, Object> map() {
        return new LinkedHashMap<>(map);
    }
}
