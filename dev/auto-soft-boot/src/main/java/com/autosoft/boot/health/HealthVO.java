package com.autosoft.boot.health;

import java.time.Instant;

/**
 * 健康检查出参。
 */
public class HealthVO {

    private String appName;
    private String profile;
    private String db;
    private Instant now;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public Instant getNow() {
        return now;
    }

    public void setNow(Instant now) {
        this.now = now;
    }
}
