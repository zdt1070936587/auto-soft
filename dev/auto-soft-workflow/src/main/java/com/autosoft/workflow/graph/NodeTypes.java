package com.autosoft.workflow.graph;

import java.util.Set;

/**
 * NodeTypes。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public final class NodeTypes {

    public static final String START = "start";
    public static final String END = "end";
    public static final String META_QUERY = "meta.query";
    public static final String LLM = "llm";
    public static final String NOTIFY = "notify";
    public static final String CONDITION = "condition";
    public static final String APPROVAL = "approval";
    public static final String META_UPSERT = "meta.upsert";
    public static final String HTTP = "http";

    public static final Set<String> PHASE_A = Set.of(START, END, META_QUERY, LLM, NOTIFY);
    public static final Set<String> PHASE_B = Set.of(START, END, META_QUERY, LLM, NOTIFY, CONDITION, APPROVAL, META_UPSERT);
    public static final Set<String> PHASE_C = Set.of(START, END, META_QUERY, LLM, NOTIFY, CONDITION, APPROVAL, META_UPSERT, HTTP);

    public static final Set<String> TRIGGER_TYPES = Set.of("manual", "form", "cron");

    private NodeTypes() {
    }
}
