package com.autosoft.system.dto;

import com.autosoft.common.core.PageQuery;

/**
 * 角色分页查询。
 */
public class RoleQuery extends PageQuery {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
