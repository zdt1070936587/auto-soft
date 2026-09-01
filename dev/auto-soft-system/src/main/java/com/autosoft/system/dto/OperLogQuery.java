package com.autosoft.system.dto;

import com.autosoft.common.core.PageQuery;

/**
 * OperLog查询条件。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class OperLogQuery extends PageQuery {

    private String module;
    private String username;

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
