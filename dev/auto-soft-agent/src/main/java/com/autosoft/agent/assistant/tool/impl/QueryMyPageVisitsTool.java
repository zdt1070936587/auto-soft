package com.autosoft.agent.assistant.tool.impl;

import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import com.autosoft.system.log.PageVisitUserQueryService;
import com.autosoft.system.vo.PageVisitVO;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QueryMyPageVisitsTool implements AssistantTool {

    private final PageVisitUserQueryService pageVisitUserQueryService;
    private final JsonMapper jsonMapper;

    public QueryMyPageVisitsTool(PageVisitUserQueryService pageVisitUserQueryService, JsonMapper jsonMapper) {
        this.pageVisitUserQueryService = pageVisitUserQueryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "query_my_page_visits";
    }

    @Override
    public String description() {
        return "查询当前用户在指定时间范围内的页面浏览记录。仅返回本人已记录的页面访问。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("time_from", prop("string", "区间开始 ISO8601，如 2026-08-27T00:00:00+08:00"));
        props.put("time_to", prop("string", "区间结束 ISO8601"));
        props.put("path", prop("string", "页面路径，如 /system/users"));
        props.put("title_keyword", prop("string", "页面标题关键词，如「用户管理」"));
        props.put("limit", prop("integer", "最多返回条数，默认 20"));
        return objectSchema(props, List.of("time_from", "time_to"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        Instant from = parseInstant(args.get("time_from"));
        Instant to = parseInstant(args.get("time_to"));
        String path = str(args.get("path"));
        String titleKeyword = str(args.get("title_keyword"));
        int limit = intVal(args.get("limit"), 20);
        List<PageVisitVO> items = pageVisitUserQueryService.queryMine(
                context.getUserId(), from, to, path, titleKeyword, limit);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("total", items.size());
        payload.put("items", items);
        return AssistantToolRegistry.json(jsonMapper, payload);
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private static int intVal(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private static Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        return Instant.parse(String.valueOf(value));
    }
}
