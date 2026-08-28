package com.autosoft.system.menu;

import com.autosoft.system.entity.MenuDO;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.entity.RoleMenuDO;
import com.autosoft.system.entity.UserRoleDO;
import com.autosoft.system.mapper.MenuMapper;
import com.autosoft.system.mapper.RoleMapper;
import com.autosoft.system.mapper.RoleMenuMapper;
import com.autosoft.system.mapper.UserRoleMapper;
import com.autosoft.system.vo.MenuSearchHit;
import com.autosoft.system.vo.MenuVO;
import com.autosoft.system.vo.RoleVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单树与权限集合。
 */
@Service
public class MenuService {

    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;

    public MenuService(MenuMapper menuMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                       RoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    public List<RoleDO> listRoles(Long userId) {
        List<Long> roleIds = listRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<RoleDO>().in(RoleDO::getId, roleIds)
                .eq(RoleDO::getStatus, 1)
                .orderByAsc(RoleDO::getSort));
    }

    public List<RoleVO> listRoleVos(Long userId) {
        return listRoles(userId).stream().map(this::toRoleVo).toList();
    }

    public Set<String> listPermissions(Long userId) {
        List<MenuDO> menus = listAuthorizedMenus(userId, false);
        Set<String> permissions = new LinkedHashSet<>();
        for (MenuDO menu : menus) {
            if (menu.getPermission() != null && !menu.getPermission().isBlank()) {
                permissions.add(menu.getPermission());
            }
        }
        return permissions;
    }

    public List<MenuVO> listMineTree(Long userId) {
        return buildTree(listAuthorizedMenus(userId, true));
    }

    public List<MenuVO> listFullTree() {
        List<MenuDO> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getStatus, 1)
                .orderByAsc(MenuDO::getSort)
                .orderByAsc(MenuDO::getId));
        return buildTree(menus);
    }

    /**
     * 按名称/路径搜索当前用户可见菜单（Assistant 导航）。
     */
    public List<MenuSearchHit> searchMine(Long userId, String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        int capped = limit <= 0 ? 5 : Math.min(limit, 10);
        String kw = keyword.trim().toLowerCase();
        List<MenuVO> tree = listMineTree(userId);
        List<ScoredHit> scored = new ArrayList<>();
        flattenForSearch(tree, null, scored, kw);
        scored.sort(Comparator.comparingInt(ScoredHit::score).reversed()
                .thenComparing(item -> item.hit.getSort() == null ? Integer.MAX_VALUE : item.hit.getSort()));
        return scored.stream().limit(capped).map(item -> item.hit).toList();
    }

    private void flattenForSearch(List<MenuVO> nodes, String parentName, List<ScoredHit> out, String kw) {
        for (MenuVO node : nodes) {
            if ("MENU".equals(node.getMenuType()) && node.getPath() != null && !node.getPath().isBlank()) {
                int score = scoreMenu(node, kw);
                if (score > 0) {
                    MenuSearchHit hit = new MenuSearchHit();
                    hit.setName(node.getName());
                    hit.setPath(node.getPath());
                    hit.setPermission(node.getPermission());
                    hit.setParentName(parentName);
                    hit.setSort(node.getSort());
                    out.add(new ScoredHit(score, hit));
                }
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                flattenForSearch(node.getChildren(), node.getName(), out, kw);
            }
        }
    }

    private int scoreMenu(MenuVO node, String kw) {
        String name = node.getName() == null ? "" : node.getName().toLowerCase();
        String path = node.getPath() == null ? "" : node.getPath().toLowerCase();
        if (name.contains(kw)) {
            return name.equals(kw) ? 100 : 80;
        }
        if (path.contains(kw.replace(" ", ""))) {
            return 40;
        }
        return 0;
    }

    private record ScoredHit(int score, MenuSearchHit hit) {
    }

    public List<Long> listMenuIdsByRole(Long roleId) {
        List<RoleMenuDO> links = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenuDO>()
                .eq(RoleMenuDO::getRoleId, roleId));
        return links.stream().map(RoleMenuDO::getMenuId).toList();
    }

    private List<Long> listRoleIds(Long userId) {
        List<UserRoleDO> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getUserId, userId));
        return links.stream().map(UserRoleDO::getRoleId).toList();
    }

    private List<MenuDO> listAuthorizedMenus(Long userId, boolean visibleOnly) {
        List<Long> roleIds = listRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<RoleMenuDO> links = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenuDO>()
                .in(RoleMenuDO::getRoleId, roleIds));
        Set<Long> menuIds = new HashSet<>();
        for (RoleMenuDO link : links) {
            menuIds.add(link.getMenuId());
        }
        if (menuIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<MenuDO> wrapper = new LambdaQueryWrapper<MenuDO>()
                .in(MenuDO::getId, menuIds)
                .eq(MenuDO::getStatus, 1);
        if (visibleOnly) {
            wrapper.eq(MenuDO::getVisible, 1);
        }
        wrapper.orderByAsc(MenuDO::getSort).orderByAsc(MenuDO::getId);
        return menuMapper.selectList(wrapper);
    }

    private List<MenuVO> buildTree(List<MenuDO> menus) {
        List<MenuVO> nodes = menus.stream().map(this::toMenuVo).toList();
        Map<Long, MenuVO> index = nodes.stream().collect(Collectors.toMap(MenuVO::getId, item -> item));
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO node : nodes) {
            Long parentId = node.getParentId() == null ? 0L : node.getParentId();
            if (parentId == 0L || !index.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            index.get(parentId).getChildren().add(node);
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<MenuVO> nodes) {
        nodes.sort(Comparator.comparing(MenuVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuVO::getId));
        for (MenuVO node : nodes) {
            sortTree(node.getChildren());
        }
    }

    private MenuVO toMenuVo(MenuDO source) {
        MenuVO vo = new MenuVO();
        vo.setId(source.getId());
        vo.setParentId(source.getParentId());
        vo.setName(source.getName());
        vo.setPath(source.getPath());
        vo.setComponent(source.getComponent());
        vo.setMenuType(source.getMenuType());
        vo.setPermission(source.getPermission());
        vo.setIcon(source.getIcon());
        vo.setSort(source.getSort());
        vo.setVisible(source.getVisible());
        return vo;
    }

    public RoleVO toRoleVo(RoleDO source) {
        RoleVO vo = new RoleVO();
        vo.setId(source.getId());
        vo.setCode(source.getCode());
        vo.setName(source.getName());
        vo.setRemark(source.getRemark());
        vo.setSort(source.getSort());
        vo.setStatus(source.getStatus());
        vo.setBuiltin(source.getBuiltin());
        return vo;
    }
}
