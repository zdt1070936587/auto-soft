package com.autosoft.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 创建角色。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class RoleCreateDTO {

    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "角色编码需大写字母开头")
    private String code;

    @NotBlank
    private String name;

    private String remark;

    private Integer sort;

    private Integer status;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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
