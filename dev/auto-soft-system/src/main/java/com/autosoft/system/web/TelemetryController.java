package com.autosoft.system.web;

import com.autosoft.common.core.R;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.system.dto.PageVisitBatchDTO;
import com.autosoft.system.log.PageVisitService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 页面访问埋点上报。无业务逻辑。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Validated
@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

    private final PageVisitService pageVisitService;

    public TelemetryController(PageVisitService pageVisitService) {
        this.pageVisitService = pageVisitService;
    }

    @PostMapping("/page-visits")
    public R<Map<String, Integer>> ingest(@Valid @RequestBody PageVisitBatchDTO dto) {
        Long userId = SecurityUtils.requireUser().getUserId();
        int inserted = pageVisitService.ingest(userId, dto);
        return R.ok(Map.of("inserted", inserted));
    }
}
