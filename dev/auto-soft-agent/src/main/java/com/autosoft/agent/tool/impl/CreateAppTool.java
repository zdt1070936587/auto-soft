package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.dto.MetaAppSaveDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CreateAppTool implements AgentTool {

    private final MetaCatalogService catalogService;

    public CreateAppTool(MetaCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public String name() {
        return "create_app";
    }

    @Override
    public String description() {
        return "创建草稿应用。admin=CRUD 后台；frontend=纯前端工具页；h5=移动端页。code 必须小写字母开头。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("code", AgentTool.prop("string", "应用编码，如 leave 或 json_toolkit"));
        props.put("name", AgentTool.prop("string", "应用中文名"));
        props.put("app_kind", AgentTool.prop("string", "admin / frontend / h5，默认 admin"));
        props.put("grant_roles", AgentTool.prop("string", "发布后授权角色，逗号分隔，默认 USER"));
        return AgentTool.objectSchema(props, List.of("code", "name"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        MetaAppSaveDTO dto = new MetaAppSaveDTO();
        dto.setCode(args.requireStr("code"));
        dto.setName(args.requireStr("name"));
        dto.setAppKind(args.str("app_kind", "admin"));
        dto.setGrantRoles(args.str("grant_roles", "USER"));
        Long id = catalogService.createApp(dto);
        context.bindApp(id);
        return "{\"appId\":" + id + ",\"appKind\":\"" + dto.getAppKind() + "\"}";
    }
}
