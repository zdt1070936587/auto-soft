package com.autosoft.meta.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * NoopWorkflowResume钩子。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
@ConditionalOnMissingBean(WorkflowResumeHook.class)
public class NoopWorkflowResumeHook implements WorkflowResumeHook {
}
