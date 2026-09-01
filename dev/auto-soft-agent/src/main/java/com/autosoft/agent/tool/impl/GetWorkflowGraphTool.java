package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.agent.tool.ToolRegistry;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GetWorkflowGraph工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class GetWorkflowGraphTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;
    private final JsonMapper jsonMapper;

    public GetWorkflowGraphTool(WorkflowDefinitionService definitionService, JsonMapper jsonMapper) {
        this.definitionService = definitionService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "get_workflow_graph";
    }

    @Override
    public String description() {
        return "读取当前会话绑定的工作流图 IR。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return AgentTool.objectSchema(new LinkedHashMap<>(), List.of());
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        var vo = definitionService.getByAppId(context.requireAppId());
        return ToolRegistry.json(jsonMapper, vo.getGraph());
    }
}
