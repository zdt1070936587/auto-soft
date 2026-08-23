package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AskUserTool implements AgentTool {

    private final JsonMapper jsonMapper;

    public AskUserTool(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "ask_user";
    }

    @Override
    public String description() {
        return "向用户澄清需求：实体中文名、字段列表、是否需要列表查询或审批。不写库。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("question", AgentTool.prop("string", "要向用户确认的问题"));
        return AgentTool.objectSchema(props, List.of("question"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("asked", true);
        payload.put("question", args.requireStr("question"));
        return ToolRegistry.json(jsonMapper, payload);
    }
}
