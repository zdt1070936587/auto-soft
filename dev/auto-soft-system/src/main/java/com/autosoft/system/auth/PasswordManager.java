package com.autosoft.system.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码哈希，不承担业务编排。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class PasswordManager {

    private final PasswordEncoder passwordEncoder;

    public PasswordManager(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
