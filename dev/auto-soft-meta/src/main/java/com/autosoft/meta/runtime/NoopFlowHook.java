package com.autosoft.meta.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FlowHook.class)
public class NoopFlowHook implements FlowHook {
}
