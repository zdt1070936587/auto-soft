package com.autosoft.agent.assistant.action.model;

/**
 * 能力搜索命中项。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
public class CapabilitySearchHit {

    private String capabilityId;
    private String label;
    private String path;
    private int score;
    private String source;

    public String getCapabilityId() {
        return capabilityId;
    }

    public void setCapabilityId(String capabilityId) {
        this.capabilityId = capabilityId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
