package com.autosoft.system.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class CurrentUserVO {

    private UserVO user;
    private List<MenuVO> menus = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }

    public List<MenuVO> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuVO> menus) {
        this.menus = menus;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
