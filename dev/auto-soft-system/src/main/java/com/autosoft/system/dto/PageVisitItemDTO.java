package com.autosoft.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * PageVisitItem传输对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class PageVisitItemDTO {

    @NotBlank(message = "path 不能为空")
    @Size(max = 256, message = "path 过长")
    private String path;

    @Size(max = 64, message = "routeName 过长")
    private String routeName;

    @Size(max = 128, message = "pageTitle 过长")
    private String pageTitle;

    private Instant visitedAt;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public Instant getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(Instant visitedAt) {
        this.visitedAt = visitedAt;
    }
}
