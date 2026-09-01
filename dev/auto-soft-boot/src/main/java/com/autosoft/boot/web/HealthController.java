package com.autosoft.boot.web;

import com.autosoft.boot.health.HealthService;
import com.autosoft.boot.health.HealthVO;
import com.autosoft.common.core.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查入口。无业务逻辑，仅转发 Service。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/api/health")
    public R<HealthVO> health() {
        return R.ok(healthService.check());
    }
}
