package com.autosoft.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 管理员重置密码。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class ResetPasswordDTO {

    @NotBlank
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$", message = "密码需 8-32 位且包含字母和数字")
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
