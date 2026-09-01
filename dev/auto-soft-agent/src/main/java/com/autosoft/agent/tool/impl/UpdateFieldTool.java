package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.dto.MetaFieldSaveDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UpdateField工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class UpdateFieldTool implements AgentTool {

    private final MetaCatalogService catalogService;

    public UpdateFieldTool(MetaCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public String name() {
        return "update_field";
    }

    @Override
    public String description() {
        return "更新已有字段的中文名、类型或展示属性。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("field_id", AgentTool.prop("integer", "字段 ID"));
        props.put("name", AgentTool.prop("string", "字段中文名"));
        props.put("field_type", AgentTool.prop("string", "字段类型"));
        props.put("listed", AgentTool.prop("integer", "是否列表展示"));
        props.put("queryable", AgentTool.prop("integer", "是否可查询"));
        props.put("required_flag", AgentTool.prop("integer", "是否必填"));
        return AgentTool.objectSchema(props, List.of("field_id", "name", "field_type"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        context.requireAppId();
        MetaFieldSaveDTO dto = new MetaFieldSaveDTO();
        dto.setName(args.requireStr("name"));
        dto.setFieldType(args.requireStr("field_type"));
        dto.setListed(args.integer("listed"));
        dto.setQueryable(args.integer("queryable"));
        dto.setRequiredFlag(args.integer("required_flag"));
        catalogService.updateField(args.requireLng("field_id"), dto);
        context.markSchemaUpdated();
        return "{\"ok\":true}";
    }
}
