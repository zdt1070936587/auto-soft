package com.autosoft.system.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 登录成功返回。不含密码。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class LoginVO {

    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserVO user;
    private List<MenuVO> menus = new ArrayList<>();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

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
}
