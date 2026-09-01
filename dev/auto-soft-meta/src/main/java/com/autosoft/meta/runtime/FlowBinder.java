package com.autosoft.meta.runtime;

/**
 * FlowBinder。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public interface FlowBinder {
    Long createSimpleFlow(Long entityId, java.util.List<String> roleCodes);

    void bindFlow(Long entityId, Long definitionId);
}
