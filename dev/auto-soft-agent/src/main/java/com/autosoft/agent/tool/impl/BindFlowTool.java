package com.autosoft.agent.tool.impl;

import com.autosoft.agent.tool.AgentTool;
import com.autosoft.agent.tool.ToolArgs;
import com.autosoft.agent.tool.ToolContext;
import com.autosoft.meta.runtime.FlowBinder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BindFlow工具。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class BindFlowTool implements AgentTool {

    private final FlowBinder flowBinder;

    public BindFlowTool(FlowBinder flowBinder) {
        this.flowBinder = flowBinder;
    }

    @Override
    public String name() {
        return "bind_flow";
    }

    @Override
    public String description() {
        return "把已有流程定义绑定到实体。通常 create_simple_flow 已自动绑定，无需再调。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("entity_id", AgentTool.prop("integer", "实体 ID"));
        props.put("definition_id", AgentTool.prop("integer", "流程定义 ID"));
        return AgentTool.objectSchema(props, List.of("entity_id", "definition_id"));
    }

    @Override
    public String execute(ToolContext context, ToolArgs args) {
        context.requireAppId();
        flowBinder.bindFlow(args.requireLng("entity_id"), args.requireLng("definition_id"));
        return "{\"bound\":true}";
    }
}
