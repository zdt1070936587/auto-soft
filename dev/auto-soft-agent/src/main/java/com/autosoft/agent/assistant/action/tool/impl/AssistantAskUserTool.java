package com.autosoft.agent.assistant.action.tool.impl;

import com.autosoft.agent.assistant.tool.AssistantTool;
import com.autosoft.agent.assistant.tool.AssistantToolContext;
import com.autosoft.agent.assistant.tool.AssistantToolRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 助手向用户澄清问题工具。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
@Component
public class AssistantAskUserTool implements AssistantTool {

    private final JsonMapper jsonMapper;

    public AssistantAskUserTool(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "ask_user";
    }

    @Override
    public String description() {
        return "向用户提出澄清问题；调用后本轮对话结束，等待用户下一条消息。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("question", AssistantTool.prop("string", "要向用户确认的问题"));
        return AssistantTool.objectSchema(props, List.of("question"));
    }

    @Override
    public String execute(AssistantToolContext context, Map<String, Object> args) {
        String question = args.get("question") == null ? "请补充说明。" : String.valueOf(args.get("question"));
        context.setAskUser(true);
        context.setAskQuestion(question);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("asked", true);
        payload.put("question", question);
        return AssistantToolRegistry.json(jsonMapper, payload);
    }
}
