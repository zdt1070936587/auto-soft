package com.autosoft.meta.runtime;

/**
 * 流程钩子。阶段 2 默认空实现；阶段 4 由 flow 模块提供。
 */
public interface FlowHook {

    default boolean bound(String appCode, String entityCode) {
        return false;
    }

    default String initialStatus(String appCode, String entityCode) {
        return bound(appCode, entityCode) ? "draft" : "none";
    }

    default void assertWritable(String appCode, String entityCode, String flowStatus) {
    }

    default void assertDeletable(String appCode, String entityCode, String flowStatus) {
        assertWritable(appCode, entityCode, flowStatus);
    }
}
