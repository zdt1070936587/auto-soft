package com.autosoft.workflow.exec;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class StepRedactor {

    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)(api[_-]?key|authorization|password|secret|token)\\s*[:=]\\s*\\S+");
    private static final int LIMIT = 2000;

    private StepRedactor() {
    }

    public static String summarize(Object value) {
        if (value == null) {
            return "";
        }
        String text = stringify(value);
        text = SENSITIVE.matcher(text).replaceAll("$1=***");
        if (text.toLowerCase(Locale.ROOT).contains("apikey") || text.toLowerCase(Locale.ROOT).contains("api_key")) {
            text = text.replaceAll("(?i)[\"']?api[_-]?key[\"']?\\s*[:=]\\s*[\"']?[^,\"'\\s}]+", "apiKey=***");
        }
        if (text.length() > LIMIT) {
            return text.substring(0, LIMIT) + "...(truncated)";
        }
        return text;
    }

    private static String stringify(Object value) {
        if (value instanceof Map<?, ?> || value instanceof java.util.List<?>) {
            return String.valueOf(value);
        }
        return String.valueOf(value);
    }
}
