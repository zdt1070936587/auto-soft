package com.autosoft.agent.assistant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AssistantInfra配置。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AssistantMemoryProperties.class)
public class AssistantInfraConfig {
}
