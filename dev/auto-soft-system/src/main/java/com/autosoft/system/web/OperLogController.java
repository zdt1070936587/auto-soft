package com.autosoft.system.web;

import com.autosoft.common.core.PageResult;
import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.system.dto.OperLogQuery;
import com.autosoft.system.log.OperLogQueryService;
import com.autosoft.system.vo.OperLogVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询。无业务逻辑。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Validated
@RestController
@RequestMapping("/api/system/logs")
public class OperLogController {

    private final OperLogQueryService operLogQueryService;

    public OperLogController(OperLogQueryService operLogQueryService) {
        this.operLogQueryService = operLogQueryService;
    }

    @GetMapping
    @RequiresPermission("system:log:list")
    public R<PageResult<OperLogVO>> page(@Validated OperLogQuery query) {
        return R.ok(operLogQueryService.page(query));
    }
}
