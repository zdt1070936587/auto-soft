package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.dto.MetaAppSaveDTO;
import com.autosoft.meta.entity.MetaAppDO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UpdateAppTool implements AgentTool {

    private final MetaCatalogService catalogService;

    public UpdateAppTool(MetaCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public String name() {
        return "update_app";
    }

    @Override
    public String description() {
        return "更新当前会话绑定的草稿应用名称或授权角色。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("name", AgentTool.prop("string", "应用中文名"));
        props.put("grant_roles", AgentTool.prop("string", "授权角色，逗号分隔"));
        return AgentTool.objectSchema(props, List.of());
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        Long appId = context.requireAppId();
        MetaAppDO app = catalogService.requireApp(appId);
        MetaAppSaveDTO dto = new MetaAppSaveDTO();
        dto.setCode(app.getCode());
        dto.setName(args.str("name", app.getName()));
        dto.setGrantRoles(args.str("grant_roles", app.getGrantRoles()));
        catalogService.updateApp(appId, dto);
        context.markSchemaUpdated();
        return "{\"appId\":" + appId + "}";
    }
}
