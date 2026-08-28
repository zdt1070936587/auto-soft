package com.autosoft.workflow.schedule;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.workflow.config.WorkflowProperties;
import com.autosoft.workflow.entity.WfDefinitionDO;
import com.autosoft.workflow.entity.WfScheduleDO;
import com.autosoft.workflow.exec.WorkflowExecutor;
import com.autosoft.workflow.graph.GraphCodec;
import com.autosoft.workflow.graph.WorkflowGraph;
import com.autosoft.workflow.graph.WorkflowGraphValidator;
import com.autosoft.workflow.mapper.WfDefinitionMapper;
import com.autosoft.workflow.mapper.WfScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowScheduleService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowScheduleService.class);

    private final WfScheduleMapper scheduleMapper;
    private final WfDefinitionMapper definitionMapper;
    private final WorkflowExecutor executor;
    private final WorkflowProperties properties;
    private final JsonMapper jsonMapper;

    public WorkflowScheduleService(WfScheduleMapper scheduleMapper, WfDefinitionMapper definitionMapper,
                                   WorkflowExecutor executor, WorkflowProperties properties, JsonMapper jsonMapper) {
        this.scheduleMapper = scheduleMapper;
        this.definitionMapper = definitionMapper;
        this.executor = executor;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncFromGraph(WfDefinitionDO def, WorkflowGraph graph) {
        if (graph.getTrigger() == null || !"cron".equals(graph.getTrigger().getType())) {
            WfScheduleDO existing = find(def.getId());
            if (existing != null) {
                existing.setEnabled(0);
                scheduleMapper.updateById(existing);
            }
            return;
        }
        WfScheduleDO row = find(def.getId());
        if (row == null) {
            row = new WfScheduleDO();
            row.setDefinitionId(def.getId());
            row.setCron(graph.getTrigger().getCron());
            row.setEnabled(graph.getTrigger().getEnabled() == null || graph.getTrigger().getEnabled() != 0 ? 1 : 0);
            scheduleMapper.insert(row);
            return;
        }
        row.setCron(graph.getTrigger().getCron());
        if (graph.getTrigger().getEnabled() != null) {
            row.setEnabled(graph.getTrigger().getEnabled());
        }
        scheduleMapper.updateById(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setEnabled(Long definitionId, boolean enabled) {
        LoginUser user = SecurityUtils.requireUser();
        AssertUtils.isTrue(user.isSuperAdmin(), "仅超管可关停定时");
        WfScheduleDO row = find(definitionId);
        if (row == null) {
            throw new BizException(ResultCode.NOT_FOUND, "未配置定时");
        }
        row.setEnabled(enabled ? 1 : 0);
        scheduleMapper.updateById(row);
    }

    public WfScheduleDO find(Long definitionId) {
        return scheduleMapper.selectOne(new LambdaQueryWrapper<WfScheduleDO>()
                .eq(WfScheduleDO::getDefinitionId, definitionId));
    }

    @Scheduled(fixedDelay = 60_000)
    public void tick() {
        List<WfScheduleDO> rows = scheduleMapper.selectList(new LambdaQueryWrapper<WfScheduleDO>()
                .eq(WfScheduleDO::getEnabled, 1));
        Instant now = Instant.now();
        long minMs = properties.getSchedule().getMinIntervalMs();
        for (WfScheduleDO row : rows) {
            try {
                fireIfDue(row, now, minMs);
            } catch (Exception ex) {
                log.warn("定时工作流失败 definitionId={}: {}", row.getDefinitionId(), ex.getMessage());
            }
        }
    }

    private void fireIfDue(WfScheduleDO row, Instant now, long minMs) {
        if (row.getLastRunAt() != null && ChronoUnit.MILLIS.between(row.getLastRunAt(), now) < minMs) {
            return;
        }
        if (executor.hasActiveRun(row.getDefinitionId())) {
            return;
        }
        CronExpression cron = CronExpression.parse(WorkflowGraphValidator.normalizeCron(row.getCron()));
        Instant last = row.getLastRunAt() == null ? Instant.EPOCH : row.getLastRunAt();
        Instant next = cron.next(last);
        if (next == null || next.isAfter(now)) {
            return;
        }
        WfDefinitionDO def = definitionMapper.selectById(row.getDefinitionId());
        if (def == null || !WfDefinitionDO.PUBLISHED.equals(def.getStatus())) {
            return;
        }
        row.setLastRunAt(now);
        scheduleMapper.updateById(row);
        executor.runPublishedInternal(def.getCode(), Map.of());
    }
}
