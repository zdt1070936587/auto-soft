package com.autosoft.meta.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FlowSubmitHook.class)
public class NoopFlowSubmitHook implements FlowSubmitHook {
}
