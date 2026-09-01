package com.autosoft.agent.assistant.memory;

/**
 * 文本向量化端口。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public interface EmbeddingPort {

    float[] embed(String text);
}
