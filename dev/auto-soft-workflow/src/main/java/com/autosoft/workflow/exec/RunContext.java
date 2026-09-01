package com.autosoft.workflow.exec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RunContext。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class RunContext {

    private final Long runId;
    private final Map<String, Object> input;
    private final Map<String, Object> outputs = new LinkedHashMap<>();
    private final boolean dryRun;
    private int promptTokens;
    private int completionTokens;
    private String pauseApp;
    private String pauseEntity;
    private Long pauseBizId;
    private Long pauseInstanceId;

    public RunContext(Long runId, Map<String, Object> input, boolean dryRun) {
        this.runId = runId;
        this.input = input == null ? Map.of() : input;
        this.dryRun = dryRun;
    }

    public Long runId() {
        return runId;
    }

    public Map<String, Object> input() {
        return input;
    }

    public Map<String, Object> outputs() {
        return outputs;
    }

    public boolean dryRun() {
        return dryRun;
    }

    public void putOutput(String nodeId, Object value) {
        outputs.put(nodeId, value);
    }

    public void addTokens(int prompt, int completion) {
        this.promptTokens += prompt;
        this.completionTokens += completion;
    }

    public int promptTokens() {
        return promptTokens;
    }

    public int completionTokens() {
        return completionTokens;
    }

    public void markPause(String app, String entity, Long bizId, Long instanceId) {
        this.pauseApp = app;
        this.pauseEntity = entity;
        this.pauseBizId = bizId;
        this.pauseInstanceId = instanceId;
    }

    public String pauseApp() {
        return pauseApp;
    }

    public String pauseEntity() {
        return pauseEntity;
    }

    public Long pauseBizId() {
        return pauseBizId;
    }

    public Long pauseInstanceId() {
        return pauseInstanceId;
    }
}
