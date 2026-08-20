package com.autosoft.system.vo;

/**
 * 角色展示。
 */
public class RoleVO {

    private Long id;
    private String code;
    private String name;
    private String remark;
    private Integer sort;
    private Integer status;
    private Integer builtin;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Integer builtin) {
        this.builtin = builtin;
    }
}
