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
 * RemoveNode工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class RemoveNodeTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;

    public RemoveNodeTool(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public String name() {
        return "remove_node";
    }

    @Override
    public String description() {
        return "删除节点（不能删 start/end），同时删除相关边。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", AgentTool.prop("string", "节点 id"));
        return AgentTool.objectSchema(props, List.of("id"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        definitionService.removeNode(context.requireAppId(), args.requireStr("id"));
        context.markSchemaUpdated();
        return "{\"ok\":true}";
    }
}
