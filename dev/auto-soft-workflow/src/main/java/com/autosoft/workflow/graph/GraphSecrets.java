package com.autosoft.workflow.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GraphSecrets {

    private static final Set<String> KEYS = Set.of(
            "secret", "header", "headers", "apikey", "api_key", "authorization", "password", "token");

    private GraphSecrets() {
    }

    @SuppressWarnings("unchecked")
    public static Object strip(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> {
                String key = String.valueOf(k);
                if (KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                    return;
                }
                copy.put(key, strip(v));
            });
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object item : list) {
                copy.add(strip(item));
            }
            return copy;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> stripMap(Map<String, Object> graph) {
        Object stripped = strip(graph);
        if (stripped instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return new LinkedHashMap<>();
    }
}
