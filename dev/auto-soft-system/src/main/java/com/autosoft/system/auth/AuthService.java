package com.autosoft.system.auth;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.core.RoleCodes;
import com.autosoft.common.core.UserStatus;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.security.AuthProperties;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.framework.security.jwt.JwtManager;
import com.autosoft.system.dto.LoginDTO;
import com.autosoft.system.dto.PasswordUpdateDTO;
import com.autosoft.system.dto.RegisterDTO;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.entity.UserDO;
import com.autosoft.system.entity.UserRoleDO;
import com.autosoft.system.mapper.RoleMapper;
import com.autosoft.system.mapper.UserMapper;
import com.autosoft.system.mapper.UserRoleMapper;
import com.autosoft.system.menu.MenuService;
import com.autosoft.system.user.UserService;
import com.autosoft.system.vo.CurrentUserVO;
import com.autosoft.system.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 登录与当前用户。主方法只写步骤。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordManager passwordManager;
    private final JwtManager jwtManager;
    private final MenuService menuService;
    private final UserService userService;
    private final AuthProperties authProperties;

    public AuthService(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                       PasswordManager passwordManager, JwtManager jwtManager, MenuService menuService,
                       UserService userService, AuthProperties authProperties) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordManager = passwordManager;
        this.jwtManager = jwtManager;
        this.menuService = menuService;
        this.userService = userService;
        this.authProperties = authProperties;
    }

    public LoginVO login(LoginDTO dto) {
        UserDO user = loadEnabledUser(dto.getUsername());
        verifyPassword(dto.getPassword(), user.getPassword());
        String token = jwtManager.issue(user.getId(), user.getUsername());
        touchLastLogin(user.getId());
        return buildLoginVO(user, token);
    }

    public CurrentUserVO currentUser() {
        LoginUser loginUser = SecurityUtils.requireUser();
        UserDO user = userService.requireUser(loginUser.getUserId());
        CurrentUserVO vo = new CurrentUserVO();
        vo.setUser(userService.toVo(user));
        vo.setMenus(menuService.listMineTree(user.getId()));
        vo.setPermissions(menuService.listPermissions(user.getId()).stream().toList());
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(PasswordUpdateDTO dto) {
        LoginUser loginUser = SecurityUtils.requireUser();
        UserDO user = userService.requireUser(loginUser.getUserId());
        if (!passwordManager.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "原密码不正确");
        }
        AssertUtils.isTrue(!dto.getOldPassword().equals(dto.getNewPassword()), "新密码不能与原密码相同");
        user.setPassword(passwordManager.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        if (!authProperties.isRegisterEnabled()) {
            throw new BizException(ResultCode.FORBIDDEN, "未开放注册");
        }
        userService.assertUsernameUnique(dto.getUsername(), null);
        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordManager.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(UserStatus.ENABLED.getCode());
        userMapper.insert(user);
        RoleDO userRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, RoleCodes.USER));
        if (userRole != null) {
            UserRoleDO link = new UserRoleDO();
            link.setUserId(user.getId());
            link.setRoleId(userRole.getId());
            userRoleMapper.insert(link);
        }
    }

    private UserDO loadEnabledUser(String username) {
        UserDO user = userService.findByUsername(username);
        if (user == null) {
            throw new BizException(ResultCode.USERNAME_PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != UserStatus.ENABLED.getCode()) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已停用");
        }
        return user;
    }

    private void verifyPassword(String raw, String encoded) {
        if (!passwordManager.matches(raw, encoded)) {
            throw new BizException(ResultCode.USERNAME_PASSWORD_ERROR);
        }
    }

    private void touchLastLogin(Long userId) {
        UserDO patch = new UserDO();
        patch.setId(userId);
        patch.setLastLoginAt(Instant.now());
        userMapper.updateById(patch);
    }

    private LoginVO buildLoginVO(UserDO user, String token) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtManager.expireSeconds());
        vo.setUser(userService.toVo(user));
        vo.setMenus(menuService.listMineTree(user.getId()));
        return vo;
    }
}
