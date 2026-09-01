package com.autosoft.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Agent配置。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Configuration
@EnableConfigurationProperties({CryptoProperties.class, OpenCodeProperties.class, StudioUploadProperties.class})
public class AgentConfig {

    @Bean
    public RestClient openCodeRestClient(OpenCodeProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(Math.max(30, properties.getTimeoutSeconds())));
        String base = properties.getBaseUrl() == null ? "https://opencode.ai/zen/go/v1" : properties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return RestClient.builder().baseUrl(base).requestFactory(factory).build();
    }
}
