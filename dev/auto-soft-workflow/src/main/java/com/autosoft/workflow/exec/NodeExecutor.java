package com.autosoft.workflow.exec;

import com.autosoft.workflow.graph.WfNode;

import java.util.Map;

public interface NodeExecutor {

    String type();

    Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context);
}
