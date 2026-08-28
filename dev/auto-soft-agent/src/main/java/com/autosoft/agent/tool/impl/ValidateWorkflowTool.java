package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ValidateWorkflowTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;

    public ValidateWorkflowTool(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public String name() {
        return "validate_workflow";
    }

    @Override
    public String description() {
        return "校验当前工作流图：环、未连接、未知类型、未发布实体。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return AgentTool.objectSchema(new LinkedHashMap<>(), List.of());
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        var def = definitionService.requireByAppId(context.requireAppId());
        definitionService.validate(def.getId());
        return "{\"ok\":true}";
    }
}
