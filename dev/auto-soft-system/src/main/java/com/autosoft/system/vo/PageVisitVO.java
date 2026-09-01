package com.autosoft.system.vo;

import java.time.Instant;

/**
 * PageVisit视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class PageVisitVO {

    private Long id;
    private String path;
    private String routeName;
    private String pageTitle;
    private Instant visitedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
