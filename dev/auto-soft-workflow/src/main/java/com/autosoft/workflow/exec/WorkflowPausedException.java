package com.autosoft.workflow.exec;

/**
 * WorkflowPaused异常。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class WorkflowPausedException extends RuntimeException {

    private final String nodeId;

    public WorkflowPausedException(String nodeId) {
        super("paused");
        this.nodeId = nodeId;
    }

    public String nodeId() {
        return nodeId;
    }
}
