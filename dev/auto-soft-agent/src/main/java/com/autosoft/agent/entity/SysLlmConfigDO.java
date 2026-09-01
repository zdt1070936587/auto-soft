package com.autosoft.agent.entity;

import com.autosoft.system.entity.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * SysLlmConfig实体。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("sys_llm_config")
public class SysLlmConfigDO extends BaseDO {

    private String apiKeyCipher;
    private String apiKeyIv;
    private String defaultModel;
    private String allowedModelsJson;

    public String getApiKeyCipher() {
        return apiKeyCipher;
    }

    public void setApiKeyCipher(String apiKeyCipher) {
        this.apiKeyCipher = apiKeyCipher;
    }

    public String getApiKeyIv() {
        return apiKeyIv;
    }

    public void setApiKeyIv(String apiKeyIv) {
        this.apiKeyIv = apiKeyIv;
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
