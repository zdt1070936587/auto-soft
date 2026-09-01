package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.agent.tool.ToolRegistry;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import com.autosoft.workflow.exec.WorkflowExecutor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DryRunWorkflow工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class DryRunWorkflowTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;
    private final WorkflowExecutor executor;
    private final JsonMapper jsonMapper;

    public DryRunWorkflowTool(WorkflowDefinitionService definitionService, WorkflowExecutor executor,
                              JsonMapper jsonMapper) {
        this.definitionService = definitionService;
        this.executor = executor;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "dry_run_workflow";
    }

    @Override
    public String description() {
        return "用草稿图试跑。input 对应 trigger.input_schema。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("input", AgentTool.prop("object", "试跑输入"));
        return AgentTool.objectSchema(props, List.of());
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        var def = definitionService.requireByAppId(context.requireAppId());
        var vo = executor.dryRun(def.getId(), args.objectMap("input"));
        return ToolRegistry.json(jsonMapper, Map.of("runId", vo.getId(), "status", vo.getStatus()));
    }
}
