package com.autosoft.meta.runtime;

/**
 * 审批实例全部办完后回调。workflow 实现以唤醒 paused 的运行。
 */
public interface WorkflowResumeHook {

    default void onApprovalFinished(String app, String entity, Long bizId, boolean approved, String comment) {
    }
}
