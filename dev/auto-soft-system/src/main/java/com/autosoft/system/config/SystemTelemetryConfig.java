package com.autosoft.system.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PageVisitProperties.class)
public class SystemTelemetryConfig {
}
