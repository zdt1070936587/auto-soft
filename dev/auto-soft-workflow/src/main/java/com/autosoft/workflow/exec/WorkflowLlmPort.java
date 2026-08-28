package com.autosoft.workflow.exec;

public interface WorkflowLlmPort {

    LlmResult complete(String prompt, String contextJson);

    record LlmResult(String text, int promptTokens, int completionTokens) {
    }
}
