package com.autosoft.workflow.exec;

/**
 * WorkflowNotify端口。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public interface WorkflowNotifyPort {

    void send(String toRole, String title, String body, Long runId);
}
