package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.entity.MetaEntityDO;
import com.autosoft.meta.runtime.FlowBinder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CreateSimpleFlow工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class CreateSimpleFlowTool implements AgentTool {

    private final MetaCatalogService catalogService;
    private final FlowBinder flowBinder;

    public CreateSimpleFlowTool(MetaCatalogService catalogService, FlowBinder flowBinder) {
        this.catalogService = catalogService;
        this.flowBinder = flowBinder;
    }

    @Override
    public String name() {
        return "create_simple_flow";
    }

    @Override
    public String description() {
        return "为实体创建单线审批（1-3 级角色）。role_codes 必须是已存在的角色，如 ADMIN。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("entity_code", AgentTool.prop("string", "实体编码"));
        Map<String, Object> roles = AgentTool.prop("array", "审批角色编码，按顺序，最多 3 级");
        roles.put("items", Map.of("type", "string"));
        props.put("role_codes", roles);
        return AgentTool.objectSchema(props, List.of("entity_code", "role_codes"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        MetaAppDO app = catalogService.requireApp(context.requireAppId());
        MetaEntityDO entity = catalogService.requireEntity(app.getCode(), args.requireStr("entity_code"));
        List<String> roles = args.strList("role_codes");
        Long defId = flowBinder.createSimpleFlow(entity.getId(), roles);
        return "{\"definitionId\":" + defId + ",\"entityId\":" + entity.getId() + "}";
    }
}
