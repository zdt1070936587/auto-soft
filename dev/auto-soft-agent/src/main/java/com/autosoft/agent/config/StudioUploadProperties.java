package com.autosoft.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * StudioUpload配置属性。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@ConfigurationProperties(prefix = "autosoft.upload")
public class StudioUploadProperties {

    private String dir = "./data/studio-uploads";

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
