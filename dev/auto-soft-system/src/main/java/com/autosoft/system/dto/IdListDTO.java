package com.autosoft.system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色或菜单 ID 列表。
 */
public class IdListDTO {

    @NotNull
    private List<Long> ids = new ArrayList<>();

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
