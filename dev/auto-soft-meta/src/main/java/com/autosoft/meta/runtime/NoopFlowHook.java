package com.autosoft.meta.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * NoopFlow钩子。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
@ConditionalOnMissingBean(FlowHook.class)
public class NoopFlowHook implements FlowHook {
}
