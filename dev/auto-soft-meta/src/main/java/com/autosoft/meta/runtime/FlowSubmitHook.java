package com.autosoft.meta.runtime;

/**
 * FlowSubmit钩子。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public interface FlowSubmitHook {
    default void submit(String appCode, String entityCode, Long bizId) {
        throw new com.autosoft.common.exception.BizException(
                com.autosoft.common.core.ResultCode.BAD_REQUEST, "未绑定审批流程");
    }
}
