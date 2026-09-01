package com.autosoft.agent.dto;

/**
 * LlmConfigSave传输对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class LlmConfigSaveDTO {

    private String apiKey;
    private String defaultModel;
    private String allowedModelsJson;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

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
}
