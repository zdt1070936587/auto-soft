package com.autosoft.boot.health;

/**
 * 探活状态常量，避免接口层散落魔法值。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class HealthStatus {

    public static final String UP = "UP";
    public static final String DOWN = "DOWN";

    private HealthStatus() {
    }
}
