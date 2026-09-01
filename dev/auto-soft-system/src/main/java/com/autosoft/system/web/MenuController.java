package com.autosoft.system.web;

import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.system.menu.MenuService;
import com.autosoft.system.vo.MenuVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单查询入口。无业务逻辑。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/mine")
    public R<List<MenuVO>> mine() {
        return R.ok(menuService.listMineTree(SecurityUtils.requireUser().getUserId()));
    }

    @GetMapping("/tree")
    @RequiresPermission("system:role:grant")
    public R<List<MenuVO>> tree() {
        return R.ok(menuService.listFullTree());
    }
}
