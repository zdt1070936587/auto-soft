package com.autosoft.agent.assistant.memory;

/**
 * 文本向量化端口。
 */
public interface EmbeddingPort {

    float[] embed(String text);
}
