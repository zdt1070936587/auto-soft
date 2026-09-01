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
 * UpdateNode工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class UpdateNodeTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;

    public UpdateNodeTool(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public String name() {
        return "update_node";
    }

    @Override
    public String description() {
        return "更新节点标题或 config。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", AgentTool.prop("string", "节点 id"));
        props.put("title", AgentTool.prop("string", "标题"));
        props.put("config", AgentTool.prop("object", "配置"));
        return AgentTool.objectSchema(props, List.of("id"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        Map<String, Object> config = args.map().containsKey("config") ? args.objectMap("config") : null;
        definitionService.updateNode(context.requireAppId(), args.requireStr("id"), args.str("title"), config);
        context.markSchemaUpdated();
        return "{\"ok\":true}";
    }
}
