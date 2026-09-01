package com.autosoft.workflow.exec;

import com.autosoft.workflow.graph.WfNode;

import java.util.Map;

/**
 * Node执行器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public interface NodeExecutor {

    String type();

    Object execute(WfNode node, Map<String, Object> renderedConfig, RunContext context);
}
