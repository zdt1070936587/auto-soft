package com.autosoft.workflow.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * WorkflowGraph传输对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class WorkflowGraphDTO {

    @NotNull
    private Map<String, Object> graph;

    public Map<String, Object> getGraph() { return graph; }
    public void setGraph(Map<String, Object> graph) { this.graph = graph; }
}
