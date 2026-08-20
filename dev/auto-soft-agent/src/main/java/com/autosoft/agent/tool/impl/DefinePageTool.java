package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.dto.PageSchemaDTO;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.entity.MetaEntityDO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefinePageTool implements AgentTool {

    private final MetaCatalogService catalogService;

    public DefinePageTool(MetaCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public String name() {
        return "define_page";
    }

    @Override
    public String description() {
        return "保存 LIST/FORM/DETAIL 页面 schema。schema_json 可空，空则使用默认渲染。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("entity_code", AgentTool.prop("string", "实体编码"));
        props.put("page_type", AgentTool.prop("string", "LIST / FORM / DETAIL"));
        props.put("schema_json", AgentTool.prop("string", "可选 JSON，可空"));
        return AgentTool.objectSchema(props, List.of("entity_code", "page_type"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        MetaAppDO app = catalogService.requireApp(context.requireAppId());
        MetaEntityDO entity = catalogService.requireEntity(app.getCode(), args.requireStr("entity_code"));
        PageSchemaDTO dto = new PageSchemaDTO();
        dto.setSchemaJson(args.str("schema_json"));
        catalogService.savePage(entity.getId(), args.requireStr("page_type").toUpperCase(), dto);
        context.markSchemaUpdated();
        return "{\"ok\":true}";
    }
}
