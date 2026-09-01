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
 * SetTrigger工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class SetTriggerTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;

    public SetTriggerTool(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public String name() {
        return "set_trigger";
    }

    @Override
    public String description() {
        return "设置工作流触发器。type=manual|form|cron。form 必填已发布 app+entity；cron 必填 cron 表达式。禁止编造未发布实体。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("type", AgentTool.prop("string", "manual / form / cron"));
        props.put("input_schema", AgentTool.prop("object", "如 {\"contractId\":\"long\"}"));
        props.put("app", AgentTool.prop("string", "form 触发的已发布应用 code"));
        props.put("entity", AgentTool.prop("string", "form 触发的已发布实体 code"));
        props.put("cron", AgentTool.prop("string", "Spring cron，5 或 6 位"));
        props.put("enabled", AgentTool.prop("boolean", "定时是否启用"));
        return AgentTool.objectSchema(props, List.of());
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        Boolean enabled = args.map().containsKey("enabled") ? args.bool("enabled") : null;
        definitionService.setTrigger(context.requireAppId(), args.str("type", "manual"), args.strMap("input_schema"),
                args.str("app"), args.str("entity"), args.str("cron"), enabled);
        context.markSchemaUpdated();
        return "{\"ok\":true}";
    }
}
