package com.autosoft.agent.assistant;

import com.autosoft.agent.assistant.time.RelativeTimeParser;

import java.util.Set;

/**
 * 轻量意图 hint，供 Prompt 注入。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class AssistantIntentHint {

    private static final Set<String> NAV_KEYWORDS = Set.of(
            "在哪", "哪里", "入口", "菜单", "怎么打开", "怎么去", "找不到", "位置");
    private static final Set<String> OPER_KEYWORDS = Set.of(
            "有没有", "是否", "操作过", "新增", "修改", "删除", "发布", "昨天", "今天", "上周", "前天");

    private static final Set<String> VISIT_KEYWORDS = Set.of(
            "打开", "浏览", "看过", "访问", "进入", "页面", "列表页", "详情页");

    private static final Set<String> MEMORY_KEYWORDS = Set.of(
            "我叫", "我是", "记得", "之前", "说过", "我的名", "负责", "你还记得");

    private AssistantIntentHint() {
    }

    public static String buildHint(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (looksNav(userMessage)) {
            sb.append("[意图提示] 用户可能在询问系统导航，请优先调用 search_menus。\n");
        }
        if (looksVisit(userMessage) && !looksOperWrite(userMessage)) {
            sb.append("[意图提示] 用户可能在询问页面浏览历史，请调用 query_my_page_visits；若需解析页面名称可先 search_menus 获取 path。\n");
            RelativeTimeParser.TimeRange range = RelativeTimeParser.firstOrNull(userMessage);
            if (range != null) {
                sb.append("[时间解析] 「").append(range.label()).append("」对应 ")
                        .append(range.from()).append(" ~ ").append(range.to()).append("\n");
            }
        } else if (looksOper(userMessage)) {
            sb.append("[意图提示] 用户可能在询问个人操作历史，请调用 query_my_operations 或 get_operation_timeline，禁止编造记录。\n");
            RelativeTimeParser.TimeRange range = RelativeTimeParser.firstOrNull(userMessage);
            if (range != null) {
                sb.append("[时间解析] 「").append(range.label()).append("」对应 ")
                        .append(range.from()).append(" ~ ").append(range.to()).append("\n");
            }
        }
        if (looksMemory(userMessage)) {
            sb.append("[意图提示] 用户可能在询问或提供个人记忆，请使用 recall_user_memory 或 remember_fact。\n");
        }
        return sb.toString();
    }

    private static boolean looksNav(String text) {
        for (String kw : NAV_KEYWORDS) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksOper(String text) {
        for (String kw : OPER_KEYWORDS) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksOperWrite(String text) {
        return text.contains("新增") || text.contains("修改") || text.contains("删除") || text.contains("发布");
    }

    private static boolean looksVisit(String text) {
        for (String kw : VISIT_KEYWORDS) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksMemory(String text) {
        for (String kw : MEMORY_KEYWORDS) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
