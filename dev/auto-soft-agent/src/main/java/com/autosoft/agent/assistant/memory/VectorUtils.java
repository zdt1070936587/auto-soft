package com.autosoft.agent.assistant.memory;

import java.util.Locale;

/**
 * pgvector 字面量工具。
 */
public final class VectorUtils {

    private VectorUtils() {
    }

    public static String toPgVector(float[] values) {
        if (values == null || values.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.US, "%.8f", values[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    public static String truncateForEmbed(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars);
    }
}
