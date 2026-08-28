package com.autosoft.agent.assistant.memory;

import com.autosoft.agent.assistant.config.AssistantMemoryProperties;
import com.autosoft.agent.llm.OpenCodeGoManager;
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

@Component
public class OpenAiEmbeddingClient implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingClient.class);
    private static final int MAX_INPUT_CHARS = 8000;

    private final RestClient restClient;
    private final OpenCodeGoManager openCodeGoManager;
    private final AssistantMemoryProperties properties;
    private final JsonMapper jsonMapper;

    public OpenAiEmbeddingClient(@Qualifier("openCodeRestClient") RestClient restClient,
                                 OpenCodeGoManager openCodeGoManager,
                                 AssistantMemoryProperties properties,
                                 JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.openCodeGoManager = openCodeGoManager;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public float[] embed(String text) {
        String input = VectorUtils.truncateForEmbed(text, MAX_INPUT_CHARS);
        if (input.isBlank()) {
            return new float[properties.getEmbeddingDimensions()];
        }
        String apiKey = openCodeGoManager.decryptKey(openCodeGoManager.requireConfig());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getEmbeddingModel());
        body.put("input", input);
        String raw;
        try {
            raw = restClient.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        String err = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.warn("embedding http={}, model={}", res.getStatusCode().value(), properties.getEmbeddingModel());
                        throw new BizException(ResultCode.BAD_REQUEST, "向量模型调用失败: " + err);
                    })
                    .body(String.class);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("embedding failed, model={}", properties.getEmbeddingModel());
            throw new BizException(ResultCode.BAD_REQUEST, "向量模型调用失败");
        }
        return parse(raw);
    }

    @SuppressWarnings("unchecked")
    private float[] parse(String raw) {
        Map<String, Object> root = jsonMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
        });
        List<Map<String, Object>> data = (List<Map<String, Object>>) root.get("data");
        if (data == null || data.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "向量模型返回为空");
        }
        List<Number> embedding = (List<Number>) data.get(0).get("embedding");
        if (embedding == null || embedding.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "向量模型返回为空");
        }
        float[] values = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            values[i] = embedding.get(i).floatValue();
        }
        if (values.length != properties.getEmbeddingDimensions()) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "向量维度不匹配，期望 " + properties.getEmbeddingDimensions() + " 实际 " + values.length);
        }
        return values;
    }
}
