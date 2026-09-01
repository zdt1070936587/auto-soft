package com.autosoft.workflow.exec;

import com.autosoft.meta.runtime.WorkflowResumeHook;
import org.springframework.stereotype.Component;

/**
 * WorkflowResume适配器。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class WorkflowResumeAdapter implements WorkflowResumeHook {

    private final WorkflowExecutor executor;

    public WorkflowResumeAdapter(WorkflowExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void onApprovalFinished(String app, String entity, Long bizId, boolean approved, String comment) {
        executor.resumeAfterApproval(app, entity, bizId, approved, comment);
    }
}
