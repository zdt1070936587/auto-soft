package com.autosoft.agent.llm;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions（tool calling 主路径）。
 */
@Component
public class ChatCompletionsClient {

    private static final Logger log = LoggerFactory.getLogger(ChatCompletionsClient.class);

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public ChatCompletionsClient(@Qualifier("openCodeRestClient") RestClient restClient, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    public LlmTurn chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools, String modelId,
                        String apiKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelId);
        body.put("messages", messages);
        body.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }
        String raw;
        try {
            raw = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 401, (req, res) -> {
                        throw new BizException(ResultCode.BAD_REQUEST, "API Key 无效，请在模型设置中重新保存");
                    })
                    .onStatus(status -> status.value() == 429, (req, res) -> {
                        throw new BizException(ResultCode.BAD_REQUEST, "额度或限流，请更换模型或稍后");
                    })
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        String err = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("opencode chat http={}, model={}", res.getStatusCode().value(), modelId);
                        throw new BizException(ResultCode.BAD_REQUEST, friendlyHttp(res.getStatusCode().value(), err));
                    })
                    .body(String.class);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("opencode chat failed, model={}", modelId);
            throw new BizException(ResultCode.BAD_REQUEST, "模型调用失败，请稍后重试");
        }
        return parse(raw);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listModels(String apiKey) {
        String raw = restClient.get()
                .uri("/models")
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .onStatus(status -> status.value() == 401, (req, res) -> {
                    throw new BizException(ResultCode.BAD_REQUEST, "API Key 无效，请在模型设置中重新保存");
                })
                .onStatus(status -> status.value() == 429, (req, res) -> {
                    throw new BizException(ResultCode.BAD_REQUEST, "额度或限流，请更换模型或稍后");
                })
                .onStatus(status -> status.isError(), (req, res) -> {
                    throw new BizException(ResultCode.BAD_REQUEST, "拉取模型列表失败");
                })
                .body(String.class);
        Map<String, Object> root = jsonMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
        });
        Object data = root.get("data");
        if (!(data instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> models = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                models.add((Map<String, Object>) map);
            }
        }
        return models;
    }

    @SuppressWarnings("unchecked")
    private LlmTurn parse(String raw) {
        Map<String, Object> root = jsonMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
        });
        LlmTurn turn = new LlmTurn();
        Object usage = root.get("usage");
        if (usage instanceof Map<?, ?> usageMap) {
            turn.setPromptTokens(asInt(usageMap.get("prompt_tokens")));
            turn.setCompletionTokens(asInt(usageMap.get("completion_tokens")));
        }
        Object choices = root.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) {
            return turn;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return turn;
        }
        turn.setFinishReason(str(choice.get("finish_reason")));
        Object messageObj = choice.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return turn;
        }
        turn.setContent(str(message.get("content")));
        Object toolCalls = message.get("tool_calls");
        if (toolCalls instanceof List<?> calls) {
            for (Object callObj : calls) {
                if (!(callObj instanceof Map<?, ?> call)) {
                    continue;
                }
                LlmTurn.ToolCall toolCall = new LlmTurn.ToolCall();
                toolCall.setId(str(call.get("id")));
                Object fn = call.get("function");
                if (fn instanceof Map<?, ?> function) {
                    toolCall.setName(str(function.get("name")));
                    toolCall.setArgumentsJson(str(function.get("arguments")));
                }
                turn.getToolCalls().add(toolCall);
            }
        }
        return turn;
    }

    private static String friendlyHttp(int status, String body) {
        if (body != null && body.toLowerCase().contains("quota")) {
            return "额度或限流，请更换模型或稍后";
        }
        if (status == 404) {
            return "模型不存在，请更换默认模型";
        }
        return "模型错误，请更换模型或检查配置";
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
