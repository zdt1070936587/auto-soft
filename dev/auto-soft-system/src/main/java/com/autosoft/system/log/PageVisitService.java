package com.autosoft.system.log;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.system.config.PageVisitProperties;
import com.autosoft.system.dto.PageVisitBatchDTO;
import com.autosoft.system.dto.PageVisitItemDTO;
import com.autosoft.system.entity.PageVisitDO;
import com.autosoft.system.mapper.PageVisitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * PageVisit业务服务。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Service
public class PageVisitService {

    private final PageVisitMapper pageVisitMapper;
    private final PageVisitProperties properties;

    public PageVisitService(PageVisitMapper pageVisitMapper, PageVisitProperties properties) {
        this.pageVisitMapper = pageVisitMapper;
        this.properties = properties;
    }

    @Transactional
    public int ingest(Long userId, PageVisitBatchDTO dto) {
        if (!properties.isEnabled()) {
            return 0;
        }
        if (dto == null || dto.getVisits() == null || dto.getVisits().isEmpty()) {
            return 0;
        }
        if (dto.getVisits().size() > properties.getMaxBatchSize()) {
            throw new BizException(ResultCode.BAD_REQUEST, "单次最多上报 " + properties.getMaxBatchSize() + " 条");
        }
        Instant dedupeSince = Instant.now().minus(properties.getDedupeWindowSeconds(), ChronoUnit.SECONDS);
        Set<String> seenInBatch = new LinkedHashSet<>();
        int inserted = 0;
        for (PageVisitItemDTO item : dto.getVisits()) {
            String path = normalizePath(item.getPath());
            validatePath(path);
            if (!seenInBatch.add(path)) {
                continue;
            }
            if (pageVisitMapper.existsRecent(userId, path, dedupeSince)) {
                continue;
            }
            PageVisitDO row = new PageVisitDO();
            row.setUserId(userId);
            row.setPath(path);
            row.setRouteName(trimTo(item.getRouteName(), 64));
            row.setPageTitle(trimTo(item.getPageTitle(), 128));
            row.setVisitedAt(clampVisitedAt(item.getVisitedAt()));
            pageVisitMapper.insert(row);
            inserted++;
        }
        return inserted;
    }

    public int purgeOlderThanRetention() {
        if (!properties.isEnabled() || properties.getRetentionDays() <= 0) {
            return 0;
        }
        Instant before = Instant.now().minus(properties.getRetentionDays(), ChronoUnit.DAYS);
        return pageVisitMapper.deleteOlderThan(before);
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.trim();
        int queryIdx = trimmed.indexOf('?');
        if (queryIdx >= 0) {
            trimmed = trimmed.substring(0, queryIdx);
        }
        int hashIdx = trimmed.indexOf('#');
        if (hashIdx >= 0) {
            trimmed = trimmed.substring(0, hashIdx);
        }
        return trimmed;
    }

    private static void validatePath(String path) {
        if (!StringUtils.hasText(path) || !path.startsWith("/")) {
            throw new BizException(ResultCode.BAD_REQUEST, "path 必须以 / 开头");
        }
        if (path.length() > 256) {
            throw new BizException(ResultCode.BAD_REQUEST, "path 过长");
        }
        if (path.contains("#")) {
            throw new BizException(ResultCode.BAD_REQUEST, "path 不能包含 #");
        }
        String lower = path.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            throw new BizException(ResultCode.BAD_REQUEST, "path 不能为外链");
        }
    }

    private static Instant clampVisitedAt(Instant clientTime) {
        Instant now = Instant.now();
        if (clientTime == null) {
            return now;
        }
        Instant min = now.minus(7, ChronoUnit.DAYS);
        Instant max = now.plus(1, ChronoUnit.MINUTES);
        if (clientTime.isBefore(min)) {
            return min;
        }
        if (clientTime.isAfter(max)) {
            return now;
        }
        return clientTime;
    }

    private static String trimTo(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
}
