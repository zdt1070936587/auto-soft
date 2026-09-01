package com.autosoft.workflow.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkflowGraph。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class WorkflowGraph {

    private int version = 1;
    private String name;
    private WfTrigger trigger = new WfTrigger();
    private List<WfNode> nodes = new ArrayList<>();
    private List<WfEdge> edges = new ArrayList<>();

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public WfTrigger getTrigger() { return trigger; }
    public void setTrigger(WfTrigger trigger) { this.trigger = trigger == null ? new WfTrigger() : trigger; }
    public List<WfNode> getNodes() { return nodes; }
    public void setNodes(List<WfNode> nodes) { this.nodes = nodes == null ? new ArrayList<>() : nodes; }
    public List<WfEdge> getEdges() { return edges; }
    public void setEdges(List<WfEdge> edges) { this.edges = edges == null ? new ArrayList<>() : edges; }
}
