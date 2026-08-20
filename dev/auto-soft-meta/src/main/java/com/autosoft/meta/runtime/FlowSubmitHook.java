package com.autosoft.meta.runtime;

public interface FlowSubmitHook {
    default void submit(String appCode, String entityCode, Long bizId) {
        throw new com.autosoft.common.exception.BizException(
                com.autosoft.common.core.ResultCode.BAD_REQUEST, "未绑定审批流程");
    }
}
