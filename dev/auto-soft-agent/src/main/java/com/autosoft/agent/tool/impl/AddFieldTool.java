package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.dto.MetaFieldSaveDTO;
import com.autosoft.meta.entity.MetaEntityDO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AddFieldTool implements AgentTool {

    private final MetaCatalogService catalogService;

    public AddFieldTool(MetaCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public String name() {
        return "add_field";
    }

    @Override
    public String description() {
        return "给实体添加字段。field_type 必须是 string/text/int/long/decimal/bool/date/datetime/dict/ref。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("entity_code", AgentTool.prop("string", "实体编码"));
        props.put("code", AgentTool.prop("string", "字段编码"));
        props.put("name", AgentTool.prop("string", "字段中文名"));
        props.put("field_type", AgentTool.prop("string", "字段类型"));
        props.put("listed", AgentTool.prop("integer", "是否列表展示，1/0"));
        props.put("queryable", AgentTool.prop("integer", "是否可查询，1/0"));
        props.put("required_flag", AgentTool.prop("integer", "是否必填，1/0"));
        return AgentTool.objectSchema(props, List.of("entity_code", "code", "name", "field_type"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        MetaEntityDO entity = catalogService.requireEntity(
                catalogService.requireApp(context.requireAppId()).getCode(), args.requireStr("entity_code"));
        MetaFieldSaveDTO dto = new MetaFieldSaveDTO();
        dto.setCode(args.requireStr("code"));
        dto.setName(args.requireStr("name"));
        dto.setFieldType(args.requireStr("field_type"));
        dto.setListed(args.integer("listed") == null ? 1 : args.integer("listed"));
        dto.setQueryable(args.integer("queryable") == null ? 0 : args.integer("queryable"));
        dto.setRequiredFlag(args.integer("required_flag") == null ? 0 : args.integer("required_flag"));
        dto.setNullableFlag(dto.getRequiredFlag() != null && dto.getRequiredFlag() == 1 ? 0 : 1);
        Long id = catalogService.addField(entity.getId(), dto);
        context.markSchemaUpdated();
        return "{\"fieldId\":" + id + "}";
    }
}
