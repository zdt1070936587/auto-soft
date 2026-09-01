package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import com.autosoft.workflow.dto.WorkflowPublishDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PublishWorkflow工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class PublishWorkflowTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;

    public PublishWorkflowTool(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public String name() {
        return "publish_workflow";
    }

    @Override
    public String description() {
        return "发布工作流。必须 confirm=true，且用户已在界面确认。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("confirm", AgentTool.prop("boolean", "必须 true"));
        return AgentTool.objectSchema(props, List.of("confirm"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        AssertUtils.isTrue(args.bool("confirm"), "发布必须 confirm=true");
        var def = definitionService.requireByAppId(context.requireAppId());
        WorkflowPublishDTO dto = new WorkflowPublishDTO();
        dto.setConfirm(true);
        definitionService.publish(def.getId(), dto);
        context.markSchemaUpdated();
        return "{\"ok\":true,\"published\":true}";
    }
}
