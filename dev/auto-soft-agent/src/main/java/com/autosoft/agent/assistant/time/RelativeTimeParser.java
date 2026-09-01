package com.autosoft.agent.assistant.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 相对时间解析（Assistant 操作历史问答）。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class RelativeTimeParser {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private RelativeTimeParser() {
    }

    public record TimeRange(Instant from, Instant to, String label) {
    }

    public static List<TimeRange> parseAll(String text) {
        List<TimeRange> ranges = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return ranges;
        }
        LocalDate today = LocalDate.now(ZONE);
        if (text.contains("今天")) {
            ranges.add(dayRange(today, "今天"));
        }
        if (text.contains("昨天")) {
            ranges.add(dayRange(today.minusDays(1), "昨天"));
        }
        if (text.contains("前天")) {
            ranges.add(dayRange(today.minusDays(2), "前天"));
        }
        if (text.contains("上周") || text.contains("上个星期")) {
            LocalDate lastWeekStart = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            ranges.add(dayRange(lastWeekStart, "上周一"));
            ranges.add(new TimeRange(
                    lastWeekStart.atStartOfDay(ZONE).toInstant(),
                    lastWeekStart.plusDays(7).atStartOfDay(ZONE).toInstant(),
                    "上周"));
        }
        Matcher md = Pattern.compile("(\\d{1,2})月(\\d{1,2})日").matcher(text);
        while (md.find()) {
            int month = Integer.parseInt(md.group(1));
            int day = Integer.parseInt(md.group(2));
            LocalDate date = LocalDate.of(today.getYear(), month, day);
            ranges.add(dayRange(date, md.group()));
        }
        return ranges;
    }

    public static TimeRange firstOrNull(String text) {
        List<TimeRange> ranges = parseAll(text);
        return ranges.isEmpty() ? null : ranges.get(0);
    }

    private static TimeRange dayRange(LocalDate date, String label) {
        Instant from = date.atStartOfDay(ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZONE).toInstant();
        return new TimeRange(from, to, label);
    }
}
