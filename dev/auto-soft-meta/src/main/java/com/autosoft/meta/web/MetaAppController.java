package com.autosoft.meta.web;

import com.autosoft.common.core.R;
import com.autosoft.framework.security.RequiresPermission;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.dto.MetaAppSaveDTO;
import com.autosoft.meta.dto.MetaEntitySaveDTO;
import com.autosoft.meta.dto.MetaFieldSaveDTO;
import com.autosoft.meta.dto.PageSchemaDTO;
import com.autosoft.meta.dto.PublishDTO;
import com.autosoft.meta.publish.PublishService;
import com.autosoft.meta.vo.MetaAppVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 元数据建模入口。无业务逻辑。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@RestController
@RequestMapping("/api/meta")
public class MetaAppController {

    private final MetaCatalogService catalogService;
    private final PublishService publishService;

    public MetaAppController(MetaCatalogService catalogService, PublishService publishService) {
        this.catalogService = catalogService;
        this.publishService = publishService;
    }

    @GetMapping("/apps")
    @RequiresPermission("meta:app:manage")
    public R<List<MetaAppVO>> apps() {
        return R.ok(catalogService.listApps());
    }

    @GetMapping("/apps/{id}")
    @RequiresPermission("meta:app:manage")
    public R<MetaAppVO> app(@PathVariable Long id) {
        return R.ok(catalogService.getAppSchema(id));
    }

    @GetMapping("/apps/{id}/schema")
    @RequiresPermission("meta:app:manage")
    public R<MetaAppVO> schema(@PathVariable Long id) {
        return R.ok(catalogService.getAppSchema(id));
    }

    @PostMapping("/apps")
    @RequiresPermission("meta:app:manage")
    public R<Long> createApp(@Valid @RequestBody MetaAppSaveDTO dto) {
        return R.ok(catalogService.createApp(dto));
    }

    @PutMapping("/apps/{id}")
    @RequiresPermission("meta:app:manage")
    public R<Void> updateApp(@PathVariable Long id, @Valid @RequestBody MetaAppSaveDTO dto) {
        catalogService.updateApp(id, dto);
        return R.ok();
    }

    @DeleteMapping("/apps/{id}")
    @RequiresPermission("meta:app:manage")
    public R<Void> deleteApp(@PathVariable Long id) {
        catalogService.deleteApp(id);
        return R.ok();
    }

    @PostMapping("/apps/{appId}/entities")
    @RequiresPermission("meta:app:manage")
    public R<Long> createEntity(@PathVariable Long appId, @Valid @RequestBody MetaEntitySaveDTO dto) {
        return R.ok(catalogService.createEntity(appId, dto));
    }

    @PutMapping("/entities/{id}")
    @RequiresPermission("meta:app:manage")
    public R<Void> updateEntity(@PathVariable Long id, @Valid @RequestBody MetaEntitySaveDTO dto) {
        catalogService.updateEntity(id, dto);
        return R.ok();
    }

    @DeleteMapping("/entities/{id}")
    @RequiresPermission("meta:app:manage")
    public R<Void> deleteEntity(@PathVariable Long id) {
        catalogService.deleteEntity(id);
        return R.ok();
    }

    @PostMapping("/entities/{id}/fields")
    @RequiresPermission("meta:app:manage")
    public R<Long> addField(@PathVariable Long id, @Valid @RequestBody MetaFieldSaveDTO dto) {
        return R.ok(catalogService.addField(id, dto));
    }

    @PutMapping("/fields/{id}")
    @RequiresPermission("meta:app:manage")
    public R<Void> updateField(@PathVariable Long id, @Valid @RequestBody MetaFieldSaveDTO dto) {
        catalogService.updateField(id, dto);
        return R.ok();
    }

    @DeleteMapping("/fields/{id}")
    @RequiresPermission("meta:app:manage")
    public R<Void> deleteField(@PathVariable Long id) {
        catalogService.deleteField(id);
        return R.ok();
    }

    @PutMapping("/entities/{id}/pages/{type}")
    @RequiresPermission("meta:app:manage")
    public R<Void> savePage(@PathVariable Long id, @PathVariable String type, @RequestBody PageSchemaDTO dto) {
        catalogService.savePage(id, type, dto);
        return R.ok();
    }

    @PostMapping("/apps/{id}/publish")
    @RequiresPermission("meta:app:manage")
    public R<Void> publish(@PathVariable Long id, @RequestBody(required = false) PublishDTO dto) {
        publishService.publish(id, dto == null ? new PublishDTO() : dto);
        return R.ok();
    }

    @PostMapping("/apps/{id}/unpublish")
    @RequiresPermission("meta:app:manage")
    public R<Void> unpublish(@PathVariable Long id) {
        publishService.unpublish(id);
        return R.ok();
    }
}
