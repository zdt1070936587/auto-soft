package com.autosoft.meta.runtime;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnMissingBean(FlowStartPort.class)
public class NoopFlowStartPort implements FlowStartPort {

    @Override
    public Long startSimple(String app, String entity, Long bizId, List<String> roleCodes) {
        throw new BizException(ResultCode.BAD_REQUEST, "未接入审批");
    }
}
