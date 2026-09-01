package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import com.autosoft.workflow.dto.WorkflowCreateDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CreateWorkflow工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class CreateWorkflowTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;

    public CreateWorkflowTool(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public String name() {
        return "create_workflow";
    }

    @Override
    public String description() {
        return "创建自动化工作流草稿（app_kind=workflow）。不要用 create_app。code 小写字母开头。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("code", AgentTool.prop("string", "工作流编码，如 contract_remind"));
        props.put("name", AgentTool.prop("string", "中文名称"));
        props.put("grant_roles", AgentTool.prop("string", "发布后授权角色，默认 USER"));
        return AgentTool.objectSchema(props, List.of("code", "name"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        WorkflowCreateDTO dto = new WorkflowCreateDTO();
        dto.setCode(args.requireStr("code"));
        dto.setName(args.requireStr("name"));
        dto.setGrantRoles(args.str("grant_roles", "USER"));
        Long id = definitionService.create(dto);
        var vo = definitionService.get(id);
        context.bindApp(vo.getAppId());
        return "{\"definitionId\":" + id + ",\"appId\":" + vo.getAppId() + ",\"appKind\":\"workflow\"}";
    }
}
