package com.autosoft.boot.health;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 健康检查编排。主方法只写步骤，逐步填充 VO。
 */
@Service
public class HealthService {

    private final Environment environment;
    private final DbHealthManager dbHealthManager;

    public HealthService(Environment environment, DbHealthManager dbHealthManager) {
        this.environment = environment;
        this.dbHealthManager = dbHealthManager;
    }

    /**
     * 组装应用、数据库与时间信息。
     */
    public HealthVO check() {
        HealthVO vo = new HealthVO();
        fillAppInfo(vo);
        fillDbStatus(vo);
        fillServerTime(vo);
        return vo;
    }

    private void fillAppInfo(HealthVO vo) {
        vo.setAppName(environment.getProperty("spring.application.name", "auto-soft"));
        String[] profiles = environment.getActiveProfiles();
        vo.setProfile(profiles.length == 0 ? "default" : profiles[0]);
    }

    private void fillDbStatus(HealthVO vo) {
        vo.setDb(dbHealthManager.ping() ? HealthStatus.UP : HealthStatus.DOWN);
    }

    private void fillServerTime(HealthVO vo) {
        vo.setNow(Instant.now());
    }
}
