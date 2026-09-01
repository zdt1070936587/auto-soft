package com.autosoft.system.web;

import com.autosoft.common.core.PageResult;
import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.system.dto.ResetPasswordDTO;
import com.autosoft.system.dto.RoleIdsDTO;
import com.autosoft.system.dto.StatusUpdateDTO;
import com.autosoft.system.dto.UserCreateDTO;
import com.autosoft.system.dto.UserQuery;
import com.autosoft.system.dto.UserUpdateDTO;
import com.autosoft.system.user.UserService;
import com.autosoft.system.vo.UserVO;
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

/**
 * 用户维护入口。无业务逻辑。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @RequiresPermission("system:user:list")
    public R<PageResult<UserVO>> page(@Validated UserQuery query) {
        return R.ok(userService.page(query));
    }

    @PostMapping
    @RequiresPermission("system:user:create")
    public R<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return R.ok(userService.create(dto));
    }

    @PutMapping("/{id}")
    @RequiresPermission("system:user:update")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.update(id, dto);
        return R.ok();
    }

    @PutMapping("/{id}/password")
    @RequiresPermission("system:user:update")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @RequiresPermission("system:user:update")
    public R<Void> status(@PathVariable Long id, @Valid @RequestBody StatusUpdateDTO dto) {
        userService.updateStatus(id, dto.getStatus());
        return R.ok();
    }

    @PutMapping("/{id}/roles")
    @RequiresPermission("system:user:update")
    public R<Void> roles(@PathVariable Long id, @Valid @RequestBody RoleIdsDTO dto) {
        userService.assignRoles(id, dto.getRoleIds());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("system:user:delete")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }
}
