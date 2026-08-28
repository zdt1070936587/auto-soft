package com.autosoft.agent.assistant.memory;

import com.autosoft.system.entity.OperLogDO;
import com.autosoft.system.mapper.OperLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OperLogClusterService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.of("Asia/Shanghai"));

    private final OperLogMapper operLogMapper;
    private final MemoryService memoryService;

    public OperLogClusterService(OperLogMapper operLogMapper, MemoryService memoryService) {
        this.operLogMapper = operLogMapper;
        this.memoryService = memoryService;
    }

    public void clusterYesterday(int paddingMinutes) {
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofDays(1));
        List<OperLogDO> rows = operLogMapper.selectList(new LambdaQueryWrapper<OperLogDO>()
                .ge(OperLogDO::getCreatedAt, start)
                .le(OperLogDO::getCreatedAt, end)
                .orderByAsc(OperLogDO::getCreatedAt));
        Map<Long, List<OperLogDO>> byUser = new LinkedHashMap<>();
        for (OperLogDO row : rows) {
            if (row.getUserId() == null) {
                continue;
            }
            byUser.computeIfAbsent(row.getUserId(), key -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<Long, List<OperLogDO>> entry : byUser.entrySet()) {
            clusterUserLogs(entry.getKey(), entry.getValue(), paddingMinutes);
        }
    }

    private void clusterUserLogs(Long userId, List<OperLogDO> logs, int paddingMinutes) {
        if (logs.size() < 2) {
            return;
        }
        List<OperLogDO> window = new ArrayList<>();
        OperLogDO windowStart = logs.get(0);
        for (OperLogDO log : logs) {
            if (window.isEmpty()) {
                window.add(log);
                windowStart = log;
                continue;
            }
            long gapMinutes = Duration.between(windowStart.getCreatedAt(), log.getCreatedAt()).toMinutes();
            if (gapMinutes <= paddingMinutes && sameModule(window, log.getModule())) {
                window.add(log);
            } else {
                flushCluster(userId, window);
                window = new ArrayList<>();
                window.add(log);
                windowStart = log;
            }
        }
        flushCluster(userId, window);
    }

    private boolean sameModule(List<OperLogDO> window, String module) {
        if (window.isEmpty() || module == null) {
            return false;
        }
        return module.equals(window.get(0).getModule());
    }

    private void flushCluster(Long userId, List<OperLogDO> window) {
        if (window.size() < 2) {
            return;
        }
        String module = window.get(0).getModule();
        String from = TIME_FMT.format(window.get(0).getCreatedAt());
        String to = TIME_FMT.format(window.get(window.size() - 1).getCreatedAt());
        StringBuilder actions = new StringBuilder();
        for (OperLogDO row : window) {
            if (!actions.isEmpty()) {
                actions.append(", ");
            }
            actions.append(row.getAction());
        }
        String summary = String.format("%s %s–%s 在【%s】模块进行了 %d 次操作：%s",
                window.get(0).getCreatedAt().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate(),
                from, to, module, window.size(), actions);
        memoryService.captureOperClusterEpisode(userId, summary);
    }
}
