package com.autosoft.system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 分配菜单。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class MenuIdsDTO {

    @NotNull
    private List<Long> menuIds = new ArrayList<>();

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
}
