package com.autosoft.agent.vo;

/**
 * LlmConfig视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class LlmConfigVO {

    private String defaultModel;
    private String allowedModelsJson;
    private boolean keyConfigured;
    private String keyMask;

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getAllowedModelsJson() {
        return allowedModelsJson;
    }

    public void setAllowedModelsJson(String allowedModelsJson) {
        this.allowedModelsJson = allowedModelsJson;
    }

    public boolean isKeyConfigured() {
        return keyConfigured;
    }

    public void setKeyConfigured(boolean keyConfigured) {
        this.keyConfigured = keyConfigured;
    }

    public String getKeyMask() {
        return keyMask;
    }

    public void setKeyMask(String keyMask) {
        this.keyMask = keyMask;
    }
}
