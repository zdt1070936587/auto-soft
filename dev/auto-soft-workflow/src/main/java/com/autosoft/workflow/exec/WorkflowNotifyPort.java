package com.autosoft.workflow.exec;

public interface WorkflowNotifyPort {

    void send(String toRole, String title, String body, Long runId);
}
