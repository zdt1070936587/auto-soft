package com.autosoft.agent.assistant.action.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 能力搜索结果。
 *
 * @author zhaodt
 * @since 2026-09-02
 */
public class CapabilitySearchResult {

    private List<CapabilitySearchHit> items = new ArrayList<>();
    private boolean ambiguous;

    public List<CapabilitySearchHit> getItems() {
        return items;
    }

    public void setItems(List<CapabilitySearchHit> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }

    public void setAmbiguous(boolean ambiguous) {
        this.ambiguous = ambiguous;
    }
}
