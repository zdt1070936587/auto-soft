package com.autosoft.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新用户基本信息，不改密码。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class UserUpdateDTO {

    @NotBlank
    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
