package com.autosoft.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录入参。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class LoginDTO {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
