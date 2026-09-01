package com.autosoft.workflow.exec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TemplateRenderer。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class TemplateRenderer {

    private static final Pattern TEMPLATE = Pattern.compile("\\{\\{\\s*([a-z][a-z0-9_]*)(?:\\.([A-Za-z0-9_]+))?\\s*}}");

    private TemplateRenderer() {
    }

    public static Map<String, Object> renderMap(Map<String, Object> config, RunContext context) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (config == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            result.put(entry.getKey(), renderValue(entry.getValue(), context));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Object renderValue(Object value, RunContext context) {
        if (value instanceof String text) {
            return renderString(text, context);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), renderValue(v, context)));
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object item : list) {
                copy.add(renderValue(item, context));
            }
            return copy;
        }
        return value;
    }

    public static String renderString(String text, RunContext context) {
        if (text == null) {
            return null;
        }
        Matcher matcher = TEMPLATE.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String nodeId = matcher.group(1);
            String field = matcher.group(2);
            Object resolved = resolve(nodeId, field, context);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(resolved == null ? "" : String.valueOf(resolved)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Object resolve(String nodeId, String field, RunContext context) {
        Object source;
        if ("input".equals(nodeId)) {
            source = context.input();
        } else {
            source = context.outputs().get(nodeId);
        }
        if (field == null || field.isBlank()) {
            return source;
        }
        if (source instanceof Map<?, ?> map) {
            Object direct = map.get(field);
            if (direct != null) {
                return direct;
            }
            return map.get(toSnake(field));
        }
        return null;
    }

    private static String toSnake(String field) {
        return field.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
