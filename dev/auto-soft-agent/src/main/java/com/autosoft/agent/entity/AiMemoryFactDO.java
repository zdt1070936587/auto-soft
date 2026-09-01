package com.autosoft.agent.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

/**
 * AiMemoryFact实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("ai_memory_fact")
public class AiMemoryFactDO extends BaseDO {

    private Long userId;
    private String category;
    private String factKey;
    private String factValue;
    private Float confidence;
    private Integer confirmed;
    private Long sourceEpisodeId;
    private Instant lastUsedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFactKey() {
        return factKey;
    }

    public void setFactKey(String factKey) {
        this.factKey = factKey;
    }

    public String getFactValue() {
        return factValue;
    }

    public void setFactValue(String factValue) {
        this.factValue = factValue;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public Integer getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Integer confirmed) {
        this.confirmed = confirmed;
    }

    public Long getSourceEpisodeId() {
        return sourceEpisodeId;
    }

    public void setSourceEpisodeId(Long sourceEpisodeId) {
        this.sourceEpisodeId = sourceEpisodeId;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
