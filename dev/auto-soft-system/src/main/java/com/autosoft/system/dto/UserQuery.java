package com.autosoft.system.dto;

import com.autosoft.common.core.PageQuery;

/**
 * 用户分页查询。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class UserQuery extends PageQuery {

    private String username;
    private Integer status;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
