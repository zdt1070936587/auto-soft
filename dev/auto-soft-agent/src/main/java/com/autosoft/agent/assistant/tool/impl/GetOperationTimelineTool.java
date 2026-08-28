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

@Component
public class GetOperationTimelineTool implements AssistantTool {

    private final OperLogUserQueryService operLogUserQueryService;
    private final JsonMapper jsonMapper;

    public GetOperationTimelineTool(OperLogUserQueryService operLogUserQueryService, JsonMapper jsonMapper) {
        this.operLogUserQueryService = operLogUserQueryService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "get_operation_timeline";
    }

    @Override
    public String description() {
        return "查询当前用户在时间窗口内的操作时间线（含前后 padding 分钟内的相邻操作）。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("time_from", prop("string", "关注区间开始 ISO8601"));
        props.put("time_to", prop("string", "关注区间结束 ISO8601"));
        props.put("padding_minutes", prop("integer", "前后扩展分钟数，默认 30"));
        props.put("limit", prop("integer", "最多返回条数，默认 50"));
        return objectSchema(props, List.of("time_from", "time_to"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        Instant from = Instant.parse(String.valueOf(args.get("time_from")));
        Instant to = Instant.parse(String.valueOf(args.get("time_to")));
        int padding = intVal(args.get("padding_minutes"), 30);
        int limit = intVal(args.get("limit"), 50);
        List<OperLogVO> items = operLogUserQueryService.timelineMine(
                context.getUserId(), from, to, padding, limit);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "oper_timeline");
        payload.put("items", items);
        return AssistantToolRegistry.json(jsonMapper, payload);
    }

    private static int intVal(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}
