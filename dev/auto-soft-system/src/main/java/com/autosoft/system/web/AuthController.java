package com.autosoft.system.web;

import com.autosoft.common.core.R;
import com.autosoft.system.auth.AuthService;
import com.autosoft.system.dto.LoginDTO;
import com.autosoft.system.dto.PasswordUpdateDTO;
import com.autosoft.system.dto.RegisterDTO;
import com.autosoft.system.vo.CurrentUserVO;
import com.autosoft.system.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证入口。无业务逻辑。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public R<CurrentUserVO> me() {
        return R.ok(authService.currentUser());
    }

    @PutMapping("/password")
    public R<Void> password(@Valid @RequestBody PasswordUpdateDTO dto) {
        authService.updatePassword(dto);
        return R.ok();
    }

    @PostMapping("/register")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return R.ok();
    }
}
