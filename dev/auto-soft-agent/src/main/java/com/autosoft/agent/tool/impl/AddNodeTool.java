package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.workflow.def.WorkflowDefinitionService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AddNode工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class AddNodeTool implements AgentTool {

    private final WorkflowDefinitionService definitionService;

    public AddNodeTool(WorkflowDefinitionService definitionService) {
        this.definitionService = definitionService;
    }

    @Override
    public String name() {
        return "add_node";
    }

    @Override
    public String description() {
        return "添加节点。type：start/end/meta.query/llm/notify/condition/approval/meta.upsert/http。meta.query、approval、meta.upsert 只能引用已发布实体。http 仅开发模式且 url host 须在白名单。禁止把密钥写入 graph。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", AgentTool.prop("string", "节点 id"));
        props.put("type", AgentTool.prop("string", "节点类型"));
        props.put("title", AgentTool.prop("string", "标题"));
        props.put("config", AgentTool.prop("object", "配置"));
        return AgentTool.objectSchema(props, List.of("id", "type"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        definitionService.addNode(context.requireAppId(), args.requireStr("id"), args.requireStr("type"),
                args.str("title"), args.objectMap("config"));
        context.markSchemaUpdated();
        return "{\"ok\":true}";
    }
}
