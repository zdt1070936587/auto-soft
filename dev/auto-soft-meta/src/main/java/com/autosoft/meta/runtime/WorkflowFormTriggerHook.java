package com.autosoft.meta.runtime;

import java.util.Map;

/**
 * 动态表单提交成功后触发已发布 form 工作流。
 */
public interface WorkflowFormTriggerHook {

    default void onRowSubmitted(String app, String entity, Long id, Map<String, Object> row) {
    }
}
