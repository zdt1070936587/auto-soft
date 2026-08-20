package com.autosoft.meta.web;

import com.autosoft.common.core.PageQuery;
import com.autosoft.common.core.PageResult;
import com.autosoft.common.core.R;
import com.autosoft.meta.runtime.RuntimeService;
import com.autosoft.meta.vo.RuntimeSchemaVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 动态 CRUD 入口。无业务逻辑。
 */
@RestController
@RequestMapping("/api/runtime/{app}/{entity}")
public class RuntimeController {

    private final RuntimeService runtimeService;

    public RuntimeController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @GetMapping("/schema")
    public R<RuntimeSchemaVO> schema(@PathVariable String app, @PathVariable String entity,
                                     @RequestParam(defaultValue = "false") boolean preview) {
        return R.ok(runtimeService.schema(app, entity, preview));
    }

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@PathVariable String app, @PathVariable String entity,
                                                   PageQuery page, @RequestParam Map<String, String> query,
                                                   @RequestParam(defaultValue = "false") boolean preview) {
        return R.ok(runtimeService.page(app, entity, page, query, preview));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String app, @PathVariable String entity,
                                      @PathVariable Long id,
                                      @RequestParam(defaultValue = "false") boolean preview) {
        return R.ok(runtimeService.get(app, entity, id, preview));
    }

    @PostMapping
    public R<Long> create(@PathVariable String app, @PathVariable String entity,
                          @RequestBody Map<String, Object> body) {
        return R.ok(runtimeService.create(app, entity, body));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String app, @PathVariable String entity, @PathVariable Long id,
                          @RequestBody Map<String, Object> body) {
        runtimeService.update(app, entity, id, body);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String app, @PathVariable String entity, @PathVariable Long id) {
        runtimeService.delete(app, entity, id);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable String app, @PathVariable String entity, @PathVariable Long id) {
        runtimeService.submit(app, entity, id);
        return R.ok();
    }
}
