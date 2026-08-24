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
        return "保存页面 schema。CRUD 用 LIST/FORM/DETAIL + entity_code；低代码整页用 PAGE + page_code + layout + schema_json。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("entity_code", AgentTool.prop("string", "实体编码（CRUD 页面必填）"));
        props.put("page_type", AgentTool.prop("string", "LIST / FORM / DETAIL / PAGE"));
        props.put("page_code", AgentTool.prop("string", "页面编码（PAGE 必填，如 json_tool）"));
        props.put("layout", AgentTool.prop("string", "admin / h5 / blank（PAGE 必填）"));
        props.put("schema_json", AgentTool.prop("string", "低代码 DSL JSON（PAGE 必填）或 CRUD 可选"));
        return AgentTool.objectSchema(props, List.of("page_type"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        MetaAppDO app = catalogService.requireApp(context.requireAppId());
        String pageType = args.requireStr("page_type").toUpperCase();
        if (MetaCatalogService.PAGE_TYPE_PAGE.equals(pageType)) {
            String pageCode = args.requireStr("page_code");
            String layout = args.str("layout", "admin");
            String schemaJson = args.requireStr("schema_json");
            catalogService.saveAppPage(app.getId(), pageCode, layout, schemaJson);
            context.markSchemaUpdated();
            return "{\"ok\":true,\"pageCode\":\"" + pageCode + "\"}";
        }
        MetaEntityDO entity = catalogService.requireEntity(app.getCode(), args.requireStr("entity_code"));
        PageSchemaDTO dto = new PageSchemaDTO();
        dto.setSchemaJson(args.str("schema_json"));
        catalogService.savePage(entity.getId(), pageType, dto);
        context.markSchemaUpdated();
        return "{\"ok\":true}";
    }
}
