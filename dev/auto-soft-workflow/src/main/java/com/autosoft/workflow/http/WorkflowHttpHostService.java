package com.autosoft.workflow.http;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.framework.log.OperLog;
import com.autosoft.workflow.config.WorkflowProperties;
import com.autosoft.workflow.dto.WorkflowHttpHostCreateDTO;
import com.autosoft.workflow.entity.SysWorkflowHttpHostDO;
import com.autosoft.workflow.mapper.SysWorkflowHttpHostMapper;
import com.autosoft.workflow.vo.WorkflowHttpHostVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 工作流 HTTP 出站域名白名单。合并 yml 静态配置与数据库记录。
 */
@Service
public class WorkflowHttpHostService {

    private static final Pattern HOST = Pattern.compile(
            "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

    private final SysWorkflowHttpHostMapper hostMapper;
    private final WorkflowProperties properties;
    private volatile List<String> cachedHosts = List.of();

    public WorkflowHttpHostService(SysWorkflowHttpHostMapper hostMapper, WorkflowProperties properties) {
        this.hostMapper = hostMapper;
        this.properties = properties;
    }

    public List<String> listAllowedHosts() {
        List<String> cached = cachedHosts;
        if (!cached.isEmpty()) {
            return cached;
        }
        return refreshCache();
    }

    public List<WorkflowHttpHostVO> listAll() {
        List<SysWorkflowHttpHostDO> rows = hostMapper.selectList(
                new LambdaQueryWrapper<SysWorkflowHttpHostDO>().orderByAsc(SysWorkflowHttpHostDO::getHost));
        return rows.stream().map(this::toVo).toList();
    }

    @OperLog(module = "WORKFLOW", action = "CREATE")
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkflowHttpHostCreateDTO dto) {
        String host = normalizeHost(dto.getHost());
        assertHostAllowed(host);
        assertNotDuplicate(host);
        SysWorkflowHttpHostDO row = new SysWorkflowHttpHostDO();
        row.setHost(host);
        row.setRemark(trimToNull(dto.getRemark()));
        hostMapper.insert(row);
        invalidateCache();
        return row.getId();
    }

    @OperLog(module = "WORKFLOW", action = "DELETE")
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysWorkflowHttpHostDO row = hostMapper.selectById(id);
        AssertUtils.notNull(row, "记录不存在");
        hostMapper.deleteById(id);
        invalidateCache();
    }

    public void invalidateCache() {
        cachedHosts = List.of();
    }

    private synchronized List<String> refreshCache() {
        if (!cachedHosts.isEmpty()) {
            return cachedHosts;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        List<String> fromYml = properties.getHttp().getAllowedHosts();
        if (fromYml != null) {
            for (String item : fromYml) {
                if (item != null && !item.isBlank()) {
                    merged.add(item.trim());
                }
            }
        }
        List<SysWorkflowHttpHostDO> rows = hostMapper.selectList(
                new LambdaQueryWrapper<SysWorkflowHttpHostDO>().orderByAsc(SysWorkflowHttpHostDO::getHost));
        for (SysWorkflowHttpHostDO row : rows) {
            if (row.getHost() != null && !row.getHost().isBlank()) {
                merged.add(row.getHost().trim());
            }
        }
        cachedHosts = List.copyOf(new ArrayList<>(merged));
        return cachedHosts;
    }

    private void assertNotDuplicate(String host) {
        Long count = hostMapper.selectCount(new LambdaQueryWrapper<SysWorkflowHttpHostDO>()
                .apply("LOWER(host) = {0}", host.toLowerCase(Locale.ROOT)));
        AssertUtils.isTrue(count == 0, "host 已存在: " + host);
    }

    static String normalizeHost(String raw) {
        AssertUtils.notBlank(raw, "host 不能为空");
        String host = raw.trim();
        if (host.contains("://")) {
            throw new BizException(ResultCode.BAD_REQUEST, "host 不要包含协议，仅填域名，如 www.example.com");
        }
        int slash = host.indexOf('/');
        if (slash >= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "host 不要包含路径");
        }
        int colon = host.indexOf(':');
        if (colon >= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "host 不要包含端口");
        }
        if (host.contains("*")) {
            throw new BizException(ResultCode.BAD_REQUEST, "不支持通配符 host");
        }
        return host.toLowerCase(Locale.ROOT);
    }

    static void assertHostAllowed(String host) {
        if (!HOST.matcher(host).matches()) {
            throw new BizException(ResultCode.BAD_REQUEST, "host 格式不合法: " + host);
        }
        String lower = host.toLowerCase(Locale.ROOT);
        AssertUtils.isTrue(!"localhost".equals(lower) && !lower.endsWith(".localhost"),
                "禁止添加 localhost");
    }

    private WorkflowHttpHostVO toVo(SysWorkflowHttpHostDO row) {
        WorkflowHttpHostVO vo = new WorkflowHttpHostVO();
        vo.setId(row.getId());
        vo.setHost(row.getHost());
        vo.setRemark(row.getRemark());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
