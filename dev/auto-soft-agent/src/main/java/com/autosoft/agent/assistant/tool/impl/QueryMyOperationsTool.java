package com.autosoft.agent.assistant.tool.impl;

import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import com.autosoft.system.log.OperLogUserQueryService;
import com.autosoft.system.vo.OperLogVO;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * QueryMyOperations工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class QueryMyOperationsTool implements AssistantTool {

    private final OperLogUserQueryService operLogUserQueryService;
    private final JsonMapper jsonMapper;

    public QueryMyOperationsTool(OperLogUserQueryService operLogUserQueryService, JsonMapper jsonMapper) {
        this.operLogUserQueryService = operLogUserQueryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "query_my_operations";
    }

    @Override
    public String description() {
        return "查询当前用户在指定时间范围内的操作记录。仅返回本人已记录的写操作。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("time_from", AssistantTool.prop("string", "区间开始 ISO8601，如 2026-08-27T00:00:00+08:00"));
        props.put("time_to", AssistantTool.prop("string", "区间结束 ISO8601"));
        props.put("module", AssistantTool.prop("string", "模块码，如 USER、META、WORKFLOW"));
        props.put("action", AssistantTool.prop("string", "动作码，如 CREATE、UPDATE、DELETE"));
        props.put("limit", AssistantTool.prop("integer", "最多返回条数，默认 20"));
        return AssistantTool.objectSchema(props, List.of("time_from", "time_to"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        Instant from = parseInstant(args.get("time_from"));
        Instant to = parseInstant(args.get("time_to"));
        String module = str(args.get("module"));
        String action = str(args.get("action"));
        int limit = intVal(args.get("limit"), 20);
        List<OperLogVO> items = operLogUserQueryService.queryMine(
                context.getUserId(), from, to, module, action, limit);
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
