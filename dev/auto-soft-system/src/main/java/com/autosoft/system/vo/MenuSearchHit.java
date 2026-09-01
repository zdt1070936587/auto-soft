package com.autosoft.system.vo;

/**
 * 菜单搜索结果（Assistant search_menus）。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class MenuSearchHit {

    private String name;
    private String path;
    private String permission;
    private String parentName;
    private Integer sort;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
