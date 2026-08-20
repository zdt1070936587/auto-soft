package com.autosoft.agent.llm;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 按 modelId 选择协议。上层只暴露 streamChat/chat。
 */
@Component
public class ModelProtocolRouter {

    public static final String CHAT = "chat";
    public static final String ANTHROPIC = "anthropic";
    public static final String RESPONSES = "responses";

    private final ChatCompletionsClient chatCompletionsClient;
    private final AnthropicMessagesClient anthropicMessagesClient;

    public ModelProtocolRouter(ChatCompletionsClient chatCompletionsClient,
                               AnthropicMessagesClient anthropicMessagesClient) {
        this.chatCompletionsClient = chatCompletionsClient;
        this.anthropicMessagesClient = anthropicMessagesClient;
    }

    public String protocolOf(String modelId) {
        String id = modelId == null ? "" : modelId.toLowerCase(Locale.ROOT);
        if (id.contains("grok") || id.contains("gpt-5.6") || id.contains("luna")) {
            return RESPONSES;
        }
        if (id.contains("minimax") || id.contains("qwen3")) {
            return ANTHROPIC;
        }
        return CHAT;
    }

    public LlmTurn chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools, String modelId,
                        String apiKey) {
        String protocol = protocolOf(modelId);
        return switch (protocol) {
            case ANTHROPIC -> anthropicMessagesClient.chat(messages, tools, modelId, apiKey);
            case RESPONSES -> throw new BizException(ResultCode.BAD_REQUEST,
                    "Responses 协议尚未开放，请改选 Chat Completions 模型（如 kimi-k2.7-code）");
            default -> chatCompletionsClient.chat(messages, tools, modelId, apiKey);
        };
    }
}
