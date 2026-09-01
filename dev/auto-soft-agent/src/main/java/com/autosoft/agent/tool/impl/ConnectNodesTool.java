package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ConnectNodes工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class ConnectNodesTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;

    public ConnectNodesTool(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public String name() {
        return "connect_nodes";
    }

    @Override
    public String description() {
        return "连接两个节点。condition 必须分别 connect when=true 与 when=false；其它节点可额外 when=error。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("from", AgentTool.prop("string", "起点 id"));
        props.put("to", AgentTool.prop("string", "终点 id"));
        props.put("when", AgentTool.prop("string", "true / false / error，可选"));
        return AgentTool.objectSchema(props, List.of("from", "to"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        definitionService.connect(context.requireAppId(), args.requireStr("from"), args.requireStr("to"), args.str("when"));
        context.markSchemaUpdated();
        return "{\"ok\":true}";
    }
}
