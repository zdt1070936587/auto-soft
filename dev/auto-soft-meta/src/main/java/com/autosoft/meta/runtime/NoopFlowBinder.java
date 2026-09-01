package com.autosoft.meta.runtime;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NoopFlowBinder。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
@ConditionalOnMissingBean(FlowBinder.class)
public class NoopFlowBinder implements FlowBinder {
    @Override
    public Long createSimpleFlow(Long entityId, List<String> roleCodes) {
        throw new BizException(ResultCode.BAD_REQUEST, "审批能力未启用");
    }

    @Override
    public void bindFlow(Long entityId, Long definitionId) {
        throw new BizException(ResultCode.BAD_REQUEST, "审批能力未启用");
    }
}
