package com.autosoft.meta.publish;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.ddl.DdlManager;
import com.autosoft.meta.dto.PublishDTO;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.entity.MetaAppMenuDO;
import com.autosoft.meta.entity.MetaEntityDO;
import com.autosoft.meta.entity.MetaFieldDO;
import com.autosoft.meta.mapper.MetaAppMapper;
import com.autosoft.meta.mapper.MetaAppMenuMapper;
import com.autosoft.system.entity.MenuDO;
import com.autosoft.system.entity.RoleDO;
import com.autosoft.system.entity.RoleMenuDO;
import com.autosoft.system.mapper.MenuMapper;
import com.autosoft.system.mapper.RoleMapper;
import com.autosoft.system.mapper.RoleMenuMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 发布：先 DDL，失败保持 DRAFT。
 */
@Service
public class PublishService {

    private static final Logger log = LoggerFactory.getLogger(PublishService.class);

    private final MetaCatalogService catalogService;
    private final MetaAppMapper appMapper;
    private final MetaAppMenuMapper appMenuMapper;
    private final DdlManager ddlManager;
    private final MenuMapper menuMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;

    public PublishService(MetaCatalogService catalogService, MetaAppMapper appMapper,
                          MetaAppMenuMapper appMenuMapper, DdlManager ddlManager,
                          MenuMapper menuMapper, RoleMapper roleMapper, RoleMenuMapper roleMenuMapper) {
        this.catalogService = catalogService;
        this.appMapper = appMapper;
        this.appMenuMapper = appMenuMapper;
        this.ddlManager = ddlManager;
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @OperLog(module = "META", action = "PUBLISH")
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long appId, PublishDTO dto) {
        MetaAppDO app = catalogService.requireApp(appId);
        List<MetaEntityDO> entities = catalogService.listEntities(appId);
        AssertUtils.isTrue(!entities.isEmpty(), "至少需要一个实体");
        for (MetaEntityDO entity : entities) {
            List<MetaFieldDO> fields = catalogService.listFields(entity.getId());
            AssertUtils.isTrue(!fields.isEmpty(), "实体 " + entity.getName() + " 至少需要一个字段");
            try {
                ddlManager.ensureTable(app.getCode(), entity.getCode(), fields);
            } catch (RuntimeException ex) {
                log.error("ddl failed for {} {}", app.getCode(), entity.getCode(), ex);
                throw new BizException(ResultCode.SERVER_ERROR, "建表失败，应用仍为草稿：" + ex.getMessage());
            }
        }
        if (dto != null && dto.getGrantRoles() != null && !dto.getGrantRoles().isBlank()) {
            app.setGrantRoles(dto.getGrantRoles());
        }
        upsertMenus(app, entities);
        app.setStatus(MetaAppDO.PUBLISHED);
        app.setVersion(app.getVersion() == null ? 1 : app.getVersion() + 1);
        appMapper.updateById(app);
    }

    @OperLog(module = "META", action = "UPDATE")
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long appId) {
        MetaAppDO app = catalogService.requireApp(appId);
        List<MetaAppMenuDO> links = appMenuMapper.selectList(new LambdaQueryWrapper<MetaAppMenuDO>()
                .eq(MetaAppMenuDO::getAppId, appId));
        for (MetaAppMenuDO link : links) {
            MenuDO menu = menuMapper.selectById(link.getMenuId());
            if (menu != null) {
                menu.setVisible(0);
                menuMapper.updateById(menu);
            }
        }
        app.setStatus(MetaAppDO.DRAFT);
        appMapper.updateById(app);
    }

    private void upsertMenus(MetaAppDO app, List<MetaEntityDO> entities) {
        String dirPath = "/app/" + app.getCode();
        MenuDO dir = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getPath, dirPath));
        if (dir == null) {
            dir = newMenu(0L, app.getName(), dirPath, MenuDO.TYPE_DIR, null, 80);
            menuMapper.insert(dir);
            saveLink(app.getId(), dir.getId());
        } else {
            dir.setName(app.getName());
            dir.setVisible(1);
            dir.setStatus(1);
            menuMapper.updateById(dir);
        }
        for (MetaEntityDO entity : entities) {
            String path = dirPath + "/" + entity.getCode();
            MenuDO menu = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getPath, path));
            String listPerm = perm(app.getCode(), entity.getCode(), "list");
            if (menu == null) {
                menu = newMenu(dir.getId(), entity.getName(), path, MenuDO.TYPE_MENU, listPerm, 10);
                menu.setComponent("RuntimePageView");
                menuMapper.insert(menu);
                saveLink(app.getId(), menu.getId());
            } else {
                menu.setName(entity.getName());
                menu.setVisible(1);
                menu.setStatus(1);
                menu.setParentId(dir.getId());
                menuMapper.updateById(menu);
            }
            upsertButton(app.getId(), menu.getId(), "查询", perm(app.getCode(), entity.getCode(), "list"), 10);
            upsertButton(app.getId(), menu.getId(), "新建", perm(app.getCode(), entity.getCode(), "create"), 20);
            upsertButton(app.getId(), menu.getId(), "修改", perm(app.getCode(), entity.getCode(), "update"), 30);
            upsertButton(app.getId(), menu.getId(), "删除", perm(app.getCode(), entity.getCode(), "delete"), 40);
            upsertButton(app.getId(), menu.getId(), "提交", perm(app.getCode(), entity.getCode(), "submit"), 50);
        }
        grantRoles(app);
    }

    private void upsertButton(Long appId, Long parentId, String name, String permission, int sort) {
        MenuDO button = menuMapper.selectOne(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getParentId, parentId).eq(MenuDO::getPermission, permission)
                .eq(MenuDO::getMenuType, MenuDO.TYPE_BUTTON));
        if (button == null) {
            button = newMenu(parentId, name, null, MenuDO.TYPE_BUTTON, permission, sort);
            button.setVisible(0);
            menuMapper.insert(button);
            saveLink(appId, button.getId());
            return;
        }
        button.setVisible(0);
        button.setStatus(1);
        menuMapper.updateById(button);
    }

    private void grantRoles(MetaAppDO app) {
        List<String> codes = Arrays.stream(app.getGrantRoles().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        List<MetaAppMenuDO> links = appMenuMapper.selectList(new LambdaQueryWrapper<MetaAppMenuDO>()
                .eq(MetaAppMenuDO::getAppId, app.getId()));
        for (String code : codes) {
            RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, code));
            if (role == null) {
                throw new BizException(ResultCode.BAD_REQUEST, "角色不存在: " + code);
            }
            for (MetaAppMenuDO link : links) {
                Long count = roleMenuMapper.selectCount(new LambdaQueryWrapper<RoleMenuDO>()
                        .eq(RoleMenuDO::getRoleId, role.getId()).eq(RoleMenuDO::getMenuId, link.getMenuId()));
                if (count == 0) {
                    RoleMenuDO rm = new RoleMenuDO();
                    rm.setRoleId(role.getId());
                    rm.setMenuId(link.getMenuId());
                    roleMenuMapper.insert(rm);
                }
            }
        }
    }

    private void saveLink(Long appId, Long menuId) {
        Long count = appMenuMapper.selectCount(new LambdaQueryWrapper<MetaAppMenuDO>()
                .eq(MetaAppMenuDO::getAppId, appId).eq(MetaAppMenuDO::getMenuId, menuId));
        if (count > 0) {
            return;
        }
        MetaAppMenuDO link = new MetaAppMenuDO();
        link.setAppId(appId);
        link.setMenuId(menuId);
        appMenuMapper.insert(link);
    }

    private MenuDO newMenu(Long parentId, String name, String path, String type, String permission, int sort) {
        MenuDO menu = new MenuDO();
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setPath(path);
        menu.setMenuType(type);
        menu.setPermission(permission);
        menu.setSort(sort);
        menu.setVisible(1);
        menu.setStatus(1);
        return menu;
    }

    private String perm(String app, String entity, String action) {
        return "app:" + app + ":" + entity + ":" + action;
    }
}
