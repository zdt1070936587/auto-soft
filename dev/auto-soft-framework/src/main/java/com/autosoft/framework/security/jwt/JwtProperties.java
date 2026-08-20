package com.autosoft.framework.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置。生产环境请用环境变量覆盖 secret。
 */
@ConfigurationProperties(prefix = "autosoft.jwt")
public class JwtProperties {

    /**
     * HS256 密钥，至少 32 字节。
     */
    private String secret = "dev-only-change-me-32bytes-min-secret!";

    /**
     * 过期秒数，默认 8 小时。
     */
    private long expireSeconds = 28800L;

    private String issuer = "auto-soft";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
