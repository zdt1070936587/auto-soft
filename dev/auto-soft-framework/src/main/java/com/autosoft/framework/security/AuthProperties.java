package com.autosoft.framework.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证开关。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@ConfigurationProperties(prefix = "autosoft.auth")
public class AuthProperties {

    /**
     * 是否开放自助注册，阶段 1 默认关闭。
     */
    private boolean registerEnabled = false;

    public boolean isRegisterEnabled() {
        return registerEnabled;
    }

    public void setRegisterEnabled(boolean registerEnabled) {
        this.registerEnabled = registerEnabled;
    }
}
