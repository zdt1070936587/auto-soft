package com.autosoft.meta.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * NoopWorkflowFormTrigger钩子。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
@ConditionalOnMissingBean(WorkflowFormTriggerHook.class)
public class NoopWorkflowFormTriggerHook implements WorkflowFormTriggerHook {
}
