package com.autosoft.system.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 分配角色。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class RoleIdsDTO {

    @NotEmpty
    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
