package com.autosoft.meta.runtime;

import com.autosoft.common.core.PageQuery;
import com.autosoft.common.core.PageResult;
import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.framework.security.LoginUser;
import com.autosoft.framework.security.SecurityUtils;
import com.autosoft.meta.app.AppKind;
import com.autosoft.meta.app.MetaCatalogService;
import com.autosoft.meta.ddl.Identifiers;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.entity.MetaEntityDO;
import com.autosoft.meta.entity.MetaFieldDO;
import com.autosoft.meta.entity.MetaPageDO;
import com.autosoft.meta.page.LowCodeSchemaValidator;
import com.autosoft.meta.vo.PageViewVO;
import com.autosoft.meta.vo.RuntimeSchemaVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 动态运行时。步骤：解析 → 加载元数据 → 鉴权 → SQL。
 */
@Service
public class RuntimeService {

    private final MetaCatalogService catalogService;
    private final RuntimeSqlManager sqlManager;
    private final FlowHook flowHook;
    private final FlowSubmitHook submitHook;

    public RuntimeService(MetaCatalogService catalogService, RuntimeSqlManager sqlManager, FlowHook flowHook,
                          FlowSubmitHook submitHook) {
        this.catalogService = catalogService;
        this.sqlManager = sqlManager;
        this.flowHook = flowHook;
        this.submitHook = submitHook;
    }

    public RuntimeSchemaVO schema(String appCode, String entityCode, boolean preview) {
        Loaded loaded = load(appCode, entityCode, preview, "list");
        RuntimeSchemaVO vo = new RuntimeSchemaVO();
        vo.setAppCode(loaded.app.getCode());
        vo.setAppName(loaded.app.getName());
        vo.setEntityCode(loaded.entity.getCode());
        vo.setEntityName(loaded.entity.getName());
        vo.setPublished(MetaAppDO.PUBLISHED.equals(loaded.app.getStatus()));
        vo.setFlowBound(flowHook.bound(appCode, entityCode));
        vo.setFields(loaded.fields.stream().map(catalogService::toFieldVo).toList());
        return vo;
    }

    public PageViewVO pageView(String appCode, String pageCode, boolean preview) {
        Identifiers.assertCode(appCode, "appCode");
        Identifiers.assertCode(pageCode, "pageCode");
        MetaAppDO app = catalogService.requireAppByCode(appCode);
        MetaPageDO page = catalogService.requirePage(appCode, pageCode);
        LoginUser user = SecurityUtils.requireUser();
        if (preview) {
            if (!user.isDeveloper()) {
                throw new BizException(ResultCode.FORBIDDEN, "仅开发者可预览未发布应用");
            }
        } else if (!MetaAppDO.PUBLISHED.equals(app.getStatus()) && !user.isDeveloper()) {
            throw new BizException(ResultCode.FORBIDDEN, "应用未发布");
        }
        String perm = "app:" + appCode + ":page:" + pageCode + ":view";
        if (!preview && !user.hasPermission(perm) && !user.isDeveloper()) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限");
        }
        PageViewVO vo = new PageViewVO();
        vo.setAppCode(app.getCode());
        vo.setAppName(app.getName());
        vo.setAppKind(app.getAppKind() == null ? AppKind.ADMIN.code() : app.getAppKind());
        vo.setPageCode(page.getPageCode());
        vo.setPageType(page.getPageType());
        vo.setLayout(page.getLayout());
        vo.setSchemaJson(page.getSchemaJson());
        vo.setPageTitle(LowCodeSchemaValidator.extractTitle(page.getSchemaJson()));
        vo.setPublished(MetaAppDO.PUBLISHED.equals(app.getStatus()));
        return vo;
    }

    public PageViewVO resolveAppView(String appCode, boolean preview) {
        MetaAppDO app = catalogService.requireAppByCode(appCode);
        AppKind kind = AppKind.from(app.getAppKind());
        List<MetaEntityDO> entities = catalogService.listEntities(app.getId());
        if (kind.needsDdl() && !entities.isEmpty()) {
            PageViewVO vo = new PageViewVO();
            vo.setAppCode(app.getCode());
            vo.setAppName(app.getName());
            vo.setAppKind(kind.code());
            vo.setPublished(MetaAppDO.PUBLISHED.equals(app.getStatus()));
            vo.setCrudSchema(schema(appCode, entities.get(0).getCode(), preview));
            return vo;
        }
        List<MetaPageDO> pages = catalogService.listAppPages(app.getId());
        MetaPageDO lowCode = pages.stream()
                .filter(page -> MetaCatalogService.PAGE_TYPE_PAGE.equals(page.getPageType()))
                .findFirst()
                .orElse(null);
        if (lowCode != null) {
            return pageView(appCode, lowCode.getPageCode(), preview);
        }
        if (entities.isEmpty()) {
            return null;
        }
        PageViewVO vo = new PageViewVO();
        vo.setAppCode(app.getCode());
        vo.setAppName(app.getName());
        vo.setAppKind(app.getAppKind() == null ? AppKind.ADMIN.code() : app.getAppKind());
        vo.setPublished(MetaAppDO.PUBLISHED.equals(app.getStatus()));
        vo.setCrudSchema(schema(appCode, entities.get(0).getCode(), preview));
        return vo;
    }

    public PageResult<Map<String, Object>> page(String appCode, String entityCode, PageQuery page,
                                                Map<String, String> query, boolean preview) {
        Loaded loaded = load(appCode, entityCode, preview, "list");
        return sqlManager.page(loaded.table, loaded.fields, page, query,
                query == null ? null : query.get("orderBy"),
                query == null ? null : query.get("orderDir"));
    }

    public Map<String, Object> get(String appCode, String entityCode, Long id, boolean preview) {
        Loaded loaded = load(appCode, entityCode, preview, "list");
        return sqlManager.get(loaded.table, id);
    }

    public Long create(String appCode, String entityCode, Map<String, Object> body) {
        Loaded loaded = load(appCode, entityCode, false, "create");
        String status = flowHook.initialStatus(appCode, entityCode);
        return sqlManager.insert(loaded.table, loaded.fields, body == null ? Map.of() : body, status);
    }

    public void update(String appCode, String entityCode, Long id, Map<String, Object> body) {
        Loaded loaded = load(appCode, entityCode, false, "update");
        Map<String, Object> row = sqlManager.get(loaded.table, id);
        flowHook.assertWritable(appCode, entityCode, stringVal(row.get("flow_status")));
        sqlManager.update(loaded.table, loaded.fields, id, body == null ? Map.of() : body);
    }

    public void delete(String appCode, String entityCode, Long id) {
        Loaded loaded = load(appCode, entityCode, false, "delete");
        Map<String, Object> row = sqlManager.get(loaded.table, id);
        flowHook.assertDeletable(appCode, entityCode, stringVal(row.get("flow_status")));
        sqlManager.logicDelete(loaded.table, id);
    }

    public void submit(String appCode, String entityCode, Long id) {
        load(appCode, entityCode, false, "submit");
        submitHook.submit(appCode, entityCode, id);
    }

    public Map<String, Object> getRow(String appCode, String entityCode, Long id) {
        Loaded loaded = load(appCode, entityCode, false, "list");
        return sqlManager.get(loaded.table, id);
    }

    public void updateStatus(String appCode, String entityCode, Long id, String status) {
        String table = Identifiers.tableName(appCode, entityCode);
        sqlManager.updateFlowStatus(table, id, status);
    }

    private Loaded load(String appCode, String entityCode, boolean preview, String action) {
        Identifiers.assertCode(appCode, "appCode");
        Identifiers.assertCode(entityCode, "entityCode");
        MetaAppDO app = catalogService.requireAppByCode(appCode);
        MetaEntityDO entity = catalogService.requireEntity(appCode, entityCode);
        LoginUser user = SecurityUtils.requireUser();
        if (preview) {
            if (!user.isDeveloper()) {
                throw new BizException(ResultCode.FORBIDDEN, "仅开发者可预览未发布应用");
            }
        } else if (!MetaAppDO.PUBLISHED.equals(app.getStatus()) && !user.isDeveloper()) {
            throw new BizException(ResultCode.FORBIDDEN, "应用未发布");
        }
        String perm = "app:" + appCode + ":" + entityCode + ":" + action;
        if (!preview && !user.hasPermission(perm) && !user.isDeveloper()) {
            throw new BizException(ResultCode.FORBIDDEN, "无权限");
        }
        return new Loaded(app, entity, Identifiers.tableName(appCode, entityCode),
                catalogService.listFields(entity.getId()));
    }

    private String stringVal(Object value) {
        return value == null ? "none" : String.valueOf(value);
    }

    private record Loaded(MetaAppDO app, MetaEntityDO entity, String table, List<MetaFieldDO> fields) {
    }
}
