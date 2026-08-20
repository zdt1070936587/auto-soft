package com.autosoft.system.web;

import com.autosoft.common.core.PageResult;
import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.system.dto.MenuIdsDTO;
import com.autosoft.system.dto.RoleCreateDTO;
import com.autosoft.system.dto.RoleQuery;
import com.autosoft.system.dto.RoleUpdateDTO;
import com.autosoft.system.role.RoleService;
import com.autosoft.system.vo.RoleVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色维护入口。无业务逻辑。
 */
@Validated
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @RequiresPermission("system:role:list")
    public R<PageResult<RoleVO>> page(@Validated RoleQuery query) {
        return R.ok(roleService.page(query));
    }

    @GetMapping("/options")
    @RequiresPermission("system:user:list")
    public R<List<RoleVO>> options() {
        return R.ok(roleService.listAll());
    }

    @PostMapping
    @RequiresPermission("system:role:create")
    public R<Long> create(@Valid @RequestBody RoleCreateDTO dto) {
        return R.ok(roleService.create(dto));
    }

    @PutMapping("/{id}")
    @RequiresPermission("system:role:update")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.update(id, dto);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("system:role:delete")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/menus")
    @RequiresPermission("system:role:list")
    public R<List<Long>> menus(@PathVariable Long id) {
        return R.ok(roleService.listMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    @RequiresPermission("system:role:grant")
    public R<Void> grant(@PathVariable Long id, @Valid @RequestBody MenuIdsDTO dto) {
        roleService.grantMenus(id, dto.getMenuIds());
        return R.ok();
    }
}
