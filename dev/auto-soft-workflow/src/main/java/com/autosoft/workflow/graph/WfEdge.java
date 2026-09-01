package com.autosoft.workflow.graph;

/**
 * WfEdge。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class WfEdge {

    private String from;
    private String to;
    private String when;

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getWhen() { return when; }
    public void setWhen(String when) { this.when = when; }
}
