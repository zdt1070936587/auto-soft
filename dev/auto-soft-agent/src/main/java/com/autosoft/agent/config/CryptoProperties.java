package com.autosoft.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Crypto配置属性。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@ConfigurationProperties(prefix = "autosoft.crypto")
public class CryptoProperties {

    /**
     * AES-256-GCM 密钥材料。开发占位可提交，生产必须替换。实际使用时做 SHA-256 得到 32 字节。
     */
    private String aesKey;

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
