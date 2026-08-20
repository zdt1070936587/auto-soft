package com.autosoft.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 系统菜单。
 */
@TableName("sys_menu")
public class MenuDO extends BaseDO {

    public static final String TYPE_DIR = "DIR";
    public static final String TYPE_MENU = "MENU";
    public static final String TYPE_BUTTON = "BUTTON";

    private Long parentId;
    private String name;
    private String path;
    private String component;
    private String menuType;
    private String permission;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

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

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
