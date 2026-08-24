package com.autosoft.agent.studio;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作室软暂停标记。按 sessionId 登记，下一轮模型调用前检查。
 */
@Component
public class TurnPauseRegistry {

    private final ConcurrentHashMap<Long, Boolean> paused = new ConcurrentHashMap<>();

    public void requestPause(Long sessionId) {
        paused.put(sessionId, Boolean.TRUE);
    }

    public boolean consumePause(Long sessionId) {
        return paused.remove(sessionId) != null;
    }

    public void clear(Long sessionId) {
        paused.remove(sessionId);
    }
}
