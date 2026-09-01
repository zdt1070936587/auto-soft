package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PreviewApp工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class PreviewAppTool implements AgentTool {

    @Override
    public String name() {
        return "preview_app";
    }

    @Override
    public String description() {
        return "标记前端刷新右侧预览。不执行 DDL。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return AgentTool.objectSchema(new LinkedHashMap<>(), List.of());
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        context.requireAppId();
        context.markSchemaUpdated();
        return "{\"preview\":true}";
    }
}
