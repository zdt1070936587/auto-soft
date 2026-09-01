package com.autosoft.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

/**
 * 系统用户。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@TableName("sys_user")
public class UserDO extends BaseDO {

    private String username;
    private String password;
    private String nickname;
    private Integer status;
    private Instant lastLoginAt;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
