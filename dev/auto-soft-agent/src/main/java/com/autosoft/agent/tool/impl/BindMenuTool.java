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

/**
 * BindMenu工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class BindMenuTool implements AgentTool {

    private final MetaCatalogService catalogService;

    public BindMenuTool(MetaCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public String name() {
        return "bind_menu";
    }

    @Override
    public String description() {
        return "指定发布后授权给哪些角色。角色 code 必须已存在，如 USER、ADMIN。不要编造。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("grant_roles", AgentTool.prop("string", "逗号分隔角色编码，默认 USER"));
        return AgentTool.objectSchema(props, List.of());
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        Long appId = context.requireAppId();
        MetaAppDO app = catalogService.requireApp(appId);
        MetaAppSaveDTO dto = new MetaAppSaveDTO();
        dto.setCode(app.getCode());
        dto.setName(app.getName());
        dto.setGrantRoles(args.str("grant_roles", "USER"));
        catalogService.updateApp(appId, dto);
        return "{\"grantRoles\":\"" + dto.getGrantRoles() + "\"}";
    }
}
