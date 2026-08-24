package com.autosoft.meta.web;

import com.autosoft.common.core.R;
import com.autosoft.meta.runtime.RuntimeService;
import com.autosoft.meta.vo.PageViewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 低代码页面运行时入口。
 */
@RestController
@RequestMapping("/api/runtime/{app}/pages/{page}")
public class PageViewController {

    private final RuntimeService runtimeService;

    public PageViewController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @GetMapping("/view")
    public R<PageViewVO> view(@PathVariable String app, @PathVariable String page,
                              @RequestParam(defaultValue = "false") boolean preview) {
        return R.ok(runtimeService.pageView(app, page, preview));
    }
}
