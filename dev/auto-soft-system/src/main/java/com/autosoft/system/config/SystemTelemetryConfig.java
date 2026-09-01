package com.autosoft.system.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SystemTelemetry配置。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Configuration
@EnableConfigurationProperties(PageVisitProperties.class)
public class SystemTelemetryConfig {
}
