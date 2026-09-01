package com.autosoft.workflow.exec;

/**
 * WorkflowLlm端口。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public interface WorkflowLlmPort {

    LlmResult complete(String prompt, String contextJson);

    record LlmResult(String text, int promptTokens, int completionTokens) {
    }
}
