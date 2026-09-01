package com.autosoft.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 更新角色，不可改 code。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class RoleUpdateDTO {

    @NotBlank
    private String name;

    private String remark;

    private Integer sort;

    private Integer status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
