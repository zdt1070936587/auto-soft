package com.autosoft.framework.security;

import com.autosoft.common.core.RoleCodes;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 当前登录用户，放入 SecurityContext。
 */
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private Set<String> roleCodes = new LinkedHashSet<>();
    private Set<String> permissions = new LinkedHashSet<>();

    public boolean isSuperAdmin() {
        return getRoleCodes().contains(RoleCodes.SUPER_ADMIN);
    }

    public boolean isDeveloper() {
        return isSuperAdmin() || getRoleCodes().contains(RoleCodes.DEVELOPER);
    }

    public boolean hasPermission(String permission) {
        if (isSuperAdmin()) {
            return true;
        }
        return getPermissions().contains(permission);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Set<String> getRoleCodes() {
        return roleCodes == null ? Collections.emptySet() : roleCodes;
    }

    public void setRoleCodes(Set<String> roleCodes) {
        this.roleCodes = roleCodes;
    }

    public Set<String> getPermissions() {
        return permissions == null ? Collections.emptySet() : permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
