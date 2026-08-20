package com.autosoft.system.role;

import com.autosoft.common.core.PageResult;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import com.autosoft.system.dto.RoleCreateDTO;
import com.autosoft.system.dto.RoleQuery;
import com.autosoft.system.dto.RoleUpdateDTO;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.entity.RoleMenuDO;
import com.autosoft.system.entity.UserRoleDO;
import com.autosoft.system.mapper.MenuMapper;
import com.autosoft.system.mapper.RoleMapper;
import com.autosoft.system.mapper.RoleMenuMapper;
import com.autosoft.system.mapper.UserRoleMapper;
import com.autosoft.system.menu.MenuService;
import com.autosoft.system.vo.RoleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色维护。
 */
@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final MenuService menuService;

    public RoleService(RoleMapper roleMapper, UserRoleMapper userRoleMapper, RoleMenuMapper roleMenuMapper,
                       MenuMapper menuMapper, MenuService menuService) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.menuService = menuService;
    }

    public PageResult<RoleVO> page(RoleQuery query) {
        Page<RoleDO> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), RoleDO::getName, query.getName());
        wrapper.orderByAsc(RoleDO::getSort).orderByAsc(RoleDO::getId);
        roleMapper.selectPage(page, wrapper);
        List<RoleVO> records = page.getRecords().stream().map(menuService::toRoleVo).toList();
        return new PageResult<>(page.getTotal(), records);
    }

    public List<RoleVO> listAll() {
        List<RoleDO> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getStatus, 1)
                .orderByAsc(RoleDO::getSort));
        return roles.stream().map(menuService::toRoleVo).toList();
    }

    @OperLog(module = "ROLE", action = "CREATE")
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateDTO dto) {
        RoleDO existing = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, dto.getCode()));
        if (existing != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "角色编码已存在");
        }
        RoleDO role = new RoleDO();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setRemark(dto.getRemark());
        role.setSort(dto.getSort() == null ? 0 : dto.getSort());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setBuiltin(0);
        roleMapper.insert(role);
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleUpdateDTO dto) {
        RoleDO role = requireRole(id);
        role.setName(dto.getName());
        role.setRemark(dto.getRemark());
        if (dto.getSort() != null) {
            role.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        roleMapper.updateById(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        RoleDO role = requireRole(id);
        AssertUtils.isTrue(role.getBuiltin() == null || role.getBuiltin() == 0, "内置角色不能删除");
        Long bindCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getRoleId, id));
        AssertUtils.isTrue(bindCount == 0, "角色已绑定用户，不能删除");
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getRoleId, id));
    }

    public List<Long> listMenuIds(Long roleId) {
        requireRole(roleId);
        return menuService.listMenuIdsByRole(roleId);
    }

    @OperLog(module = "ROLE", action = "UPDATE")
    @Transactional(rollbackFor = Exception.class)
    public void grantMenus(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            Long exist = menuMapper.selectCount(new LambdaQueryWrapper<com.autosoft.system.entity.MenuDO>()
                    .in(com.autosoft.system.entity.MenuDO::getId, menuIds));
            if (exist != menuIds.stream().distinct().count()) {
                throw new BizException(ResultCode.BAD_REQUEST, "菜单不存在");
            }
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds.stream().distinct().toList()) {
            RoleMenuDO link = new RoleMenuDO();
            link.setRoleId(roleId);
            link.setMenuId(menuId);
            roleMenuMapper.insert(link);
        }
    }

    private RoleDO requireRole(Long id) {
        RoleDO role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }
}
