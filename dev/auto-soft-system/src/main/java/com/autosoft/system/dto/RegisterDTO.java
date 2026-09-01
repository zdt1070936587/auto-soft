package com.autosoft.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 自助注册，默认关闭。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class RegisterDTO {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,31}$")
    private String username;

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$")
    private String password;

    @NotBlank
    private String nickname;

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
