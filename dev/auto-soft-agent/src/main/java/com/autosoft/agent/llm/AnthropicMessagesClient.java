package com.autosoft.agent.llm;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages。MVP 提示改选 Chat 模型，避免半成品协议。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class AnthropicMessagesClient {

    public LlmTurn chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools, String modelId,
                        String apiKey) {
        throw new BizException(ResultCode.BAD_REQUEST,
                "当前模型需要 Anthropic 协议，MVP 请改选 Chat Completions 模型（如 kimi-k2.7-code）");
    }
}
