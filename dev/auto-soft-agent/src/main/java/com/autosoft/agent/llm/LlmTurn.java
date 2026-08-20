package com.autosoft.agent.llm;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次模型回合结果。
 */
public class LlmTurn {

    private String content;
    private String finishReason;
    private int promptTokens;
    private int completionTokens;
    private List<ToolCall> toolCalls = new ArrayList<>();

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public static class ToolCall {
        private String id;
        private String name;
        private String argumentsJson;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArgumentsJson() {
            return argumentsJson;
        }

        public void setArgumentsJson(String argumentsJson) {
            this.argumentsJson = argumentsJson;
        }
    }
}
