package com.autosoft.agent.assistant.memory;

import java.time.Instant;

/**
 * EpisodeSearchHit。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class EpisodeSearchHit {

    private Long id;
    private String contentSummary;
    private Instant occurredAt;
    private Integer importance;
    private Double score;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContentSummary() {
        return contentSummary;
    }

    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Integer getImportance() {
        return importance;
    }

    public void setImportance(Integer importance) {
        this.importance = importance;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
