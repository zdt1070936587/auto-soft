package com.autosoft.system.user;

import com.autosoft.common.core.PageResult;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.core.RoleCodes;
import com.autosoft.common.core.UserStatus;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.system.auth.PasswordManager;
import com.autosoft.system.dto.UserCreateDTO;
import com.autosoft.system.dto.UserQuery;
import com.autosoft.system.dto.UserUpdateDTO;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.entity.UserDO;
import com.autosoft.system.entity.UserRoleDO;
import com.autosoft.system.mapper.RoleMapper;
import com.autosoft.system.mapper.UserMapper;
import com.autosoft.system.mapper.UserRoleMapper;
import com.autosoft.system.menu.MenuService;
import com.autosoft.system.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户维护。主方法只写步骤。
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordManager passwordManager;
    private final MenuService menuService;

    public UserService(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                       PasswordManager passwordManager, MenuService menuService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordManager = passwordManager;
        this.menuService = menuService;
    }

    public PageResult<UserVO> page(UserQuery query) {
        Page<UserDO> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getUsername()), UserDO::getUsername, query.getUsername());
        wrapper.eq(query.getStatus() != null, UserDO::getStatus, query.getStatus());
        wrapper.orderByDesc(UserDO::getId);
        userMapper.selectPage(page, wrapper);
        List<UserVO> records = page.getRecords().stream().map(this::toVo).toList();
        return new PageResult<>(page.getTotal(), records);
    }

    public UserVO get(Long id) {
        return toVo(requireUser(id));
    }

    @OperLog(module = "USER", action = "CREATE")
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateDTO dto) {
        assertUsernameUnique(dto.getUsername(), null);
        List<RoleDO> roles = loadRoles(dto.getRoleIds());
        assertCanGrant(roles);
        UserDO user = new UserDO();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordManager.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(dto.getStatus() == null ? UserStatus.ENABLED.getCode() : dto.getStatus());
        userMapper.insert(user);
        replaceRoles(user.getId(), dto.getRoleIds());
        return user.getId();
    }

    @OperLog(module = "USER", action = "UPDATE")
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserUpdateDTO dto) {
        UserDO user = requireUser(id);
        user.setNickname(dto.getNickname());
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        UserDO user = requireUser(id);
        user.setPassword(passwordManager.encode(newPassword));
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        AssertUtils.isTrue(status != null && (status == 0 || status == 1), "状态不合法");
        Long currentId = SecurityUtils.requireUser().getUserId();
        AssertUtils.isTrue(!id.equals(currentId), "不能停用自己");
        UserDO user = requireUser(id);
        if (status == UserStatus.DISABLED.getCode()) {
            assertNotLastSuperAdmin(user);
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @OperLog(module = "USER", action = "UPDATE")
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long id, List<Long> roleIds) {
        requireUser(id);
        List<RoleDO> roles = loadRoles(roleIds);
        assertCanGrant(roles);
        replaceRoles(id, roleIds);
    }

    @OperLog(module = "USER", action = "DELETE")
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long currentId = SecurityUtils.requireUser().getUserId();
        AssertUtils.isTrue(!id.equals(currentId), "不能删除自己");
        UserDO user = requireUser(id);
        assertNotLastSuperAdmin(user);
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, id));
    }

    public UserVO toVo(UserDO source) {
        UserVO vo = new UserVO();
        vo.setId(source.getId());
        vo.setUsername(source.getUsername());
        vo.setNickname(source.getNickname());
        vo.setStatus(source.getStatus());
        vo.setLastLoginAt(source.getLastLoginAt());
        vo.setCreatedAt(source.getCreatedAt());
        vo.setRoles(menuService.listRoleVos(source.getId()));
        return vo;
    }

    public UserDO requireUser(Long id) {
        UserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    public UserDO findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
    }

    public void assertUsernameUnique(String username, Long excludeId) {
        UserDO existing = findByUsername(username);
        if (existing != null && (excludeId == null || !existing.getId().equals(excludeId))) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名已存在");
        }
    }

    private List<RoleDO> loadRoles(List<Long> roleIds) {
        AssertUtils.notNull(roleIds, "角色不能为空");
        AssertUtils.isTrue(!roleIds.isEmpty(), "角色不能为空");
        List<RoleDO> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>().in(RoleDO::getId, roleIds));
        if (roles.size() != roleIds.stream().distinct().count()) {
            throw new BizException(ResultCode.BAD_REQUEST, "角色不存在");
        }
        return roles;
    }

    private void assertCanGrant(List<RoleDO> roles) {
        boolean grantSuper = roles.stream().anyMatch(role -> RoleCodes.SUPER_ADMIN.equals(role.getCode()));
        if (!grantSuper) {
            return;
        }
        LoginUser current = SecurityUtils.requireUser();
        if (!current.isSuperAdmin()) {
            throw new BizException(ResultCode.FORBIDDEN, "无权授予超级管理员");
        }
    }

    private void assertNotLastSuperAdmin(UserDO user) {
        boolean isSuper = menuService.listRoles(user.getId()).stream()
                .anyMatch(role -> RoleCodes.SUPER_ADMIN.equals(role.getCode()));
        if (isSuper && countEnabledSuperAdmin() <= 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "不能删除或停用最后一个超级管理员");
        }
    }

    private long countEnabledSuperAdmin() {
        RoleDO superRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getCode, RoleCodes.SUPER_ADMIN));
        if (superRole == null) {
            return 0;
        }
        List<UserRoleDO> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getRoleId, superRole.getId()));
        long count = 0;
        for (UserRoleDO link : links) {
            UserDO user = userMapper.selectById(link.getUserId());
            if (user != null && user.getStatus() != null && user.getStatus() == UserStatus.ENABLED.getCode()) {
                count++;
            }
        }
        return count;
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, userId));
        for (Long roleId : roleIds.stream().distinct().toList()) {
            UserRoleDO link = new UserRoleDO();
            link.setUserId(userId);
            link.setRoleId(roleId);
            userRoleMapper.insert(link);
        }
    }
}
