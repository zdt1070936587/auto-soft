package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.dto.MetaEntitySaveDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefineEntityTool implements AgentTool {

    private final MetaCatalogService catalogService;

    public DefineEntityTool(MetaCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public String name() {
        return "define_entity";
    }

    @Override
    public String description() {
        return "在当前应用下创建实体。code 必须符合标识符白名单。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("code", AgentTool.prop("string", "实体编码，如 leave_form"));
        props.put("name", AgentTool.prop("string", "实体中文名"));
        return AgentTool.objectSchema(props, List.of("code", "name"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        MetaEntitySaveDTO dto = new MetaEntitySaveDTO();
        dto.setCode(args.requireStr("code"));
        dto.setName(args.requireStr("name"));
        Long id = catalogService.createEntity(context.requireAppId(), dto);
        context.markSchemaUpdated();
        return "{\"entityId\":" + id + "}";
    }
}
