package com.autosoft.agent.assistant.memory;

import com.autosoft.agent.assistant.config.AssistantMemoryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 向量化封装：截断、失败降级标记。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingPort embeddingPort;
    private final AssistantMemoryProperties properties;

    public EmbeddingService(EmbeddingPort embeddingPort, AssistantMemoryProperties properties) {
        this.embeddingPort = embeddingPort;
        this.properties = properties;
    }

    public Optional<String> embedToPgVector(String text) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            float[] values = embeddingPort.embed(text);
            return Optional.of(VectorUtils.toPgVector(values));
        } catch (RuntimeException ex) {
            log.warn("embedding degraded: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
