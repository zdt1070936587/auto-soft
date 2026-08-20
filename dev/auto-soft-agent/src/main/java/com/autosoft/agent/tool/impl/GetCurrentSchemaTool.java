package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.vo.MetaAppVO;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetCurrentSchemaTool implements AgentTool {

    private final MetaCatalogService catalogService;
    private final JsonMapper jsonMapper;

    public GetCurrentSchemaTool(MetaCatalogService catalogService, JsonMapper jsonMapper) {
        this.catalogService = catalogService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String name() {
        return "get_current_schema";
    }

    @Override
    public String description() {
        return "读取当前会话草稿应用的完整 schema，供核对字段。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return AgentTool.objectSchema(new LinkedHashMap<>(), List.of());
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        MetaAppVO vo = catalogService.getAppSchema(context.requireAppId());
        return jsonMapper.writeValueAsString(vo);
    }
}
