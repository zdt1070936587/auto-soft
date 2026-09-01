package com.autosoft.system.auth;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.core.RoleCodes;
import com.autosoft.common.core.UserStatus;
import com.autosoft.common.exception.BizException;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.LoginUserLoader;
import com.autosoft.system.entity.UserDO;
import com.autosoft.system.mapper.UserMapper;
import com.autosoft.system.menu.MenuService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

/**
 * 每次请求按 userId 加载角色与权限。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class LoginUserLoaderImpl implements LoginUserLoader {

    private final UserMapper userMapper;
    private final MenuService menuService;

    public LoginUserLoaderImpl(UserMapper userMapper, MenuService menuService) {
        this.userMapper = userMapper;
        this.menuService = menuService;
    }

    @Override
    public LoginUser loadById(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        if (user.getStatus() == null || user.getStatus() != UserStatus.ENABLED.getCode()) {
            return null;
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setRoleCodes(new LinkedHashSet<>(menuService.listRoles(userId).stream()
                .map(role -> role.getCode())
                .toList()));
        loginUser.setPermissions(menuService.listPermissions(userId));
        if (loginUser.getRoleCodes().contains(RoleCodes.SUPER_ADMIN)) {
            loginUser.getPermissions().addAll(menuService.listPermissions(userId));
        }
        return loginUser;
    }

    public UserDO requireEnabled(Long userId) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != UserStatus.ENABLED.getCode()) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已停用");
        }
        return user;
    }
}
