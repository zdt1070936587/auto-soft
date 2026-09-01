package com.autosoft.agent.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

/**
 * AiMemoryEpisode实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("ai_memory_episode")
public class AiMemoryEpisodeDO extends BaseDO {

    private Long userId;
    private Long sessionId;
    private String episodeType;
    private String contentFull;
    private String contentSummary;
    private Integer importance;
    private Instant occurredAt;
    private Integer decayStage;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getEpisodeType() {
        return episodeType;
    }

    public void setEpisodeType(String episodeType) {
        this.episodeType = episodeType;
    }

    public String getContentFull() {
        return contentFull;
    }

    public void setContentFull(String contentFull) {
        this.contentFull = contentFull;
    }

    public String getContentSummary() {
        return contentSummary;
    }

    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }

    public Integer getImportance() {
        return importance;
    }

    public void setImportance(Integer importance) {
        this.importance = importance;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Integer getDecayStage() {
        return decayStage;
    }

    public void setDecayStage(Integer decayStage) {
        this.decayStage = decayStage;
    }
}
