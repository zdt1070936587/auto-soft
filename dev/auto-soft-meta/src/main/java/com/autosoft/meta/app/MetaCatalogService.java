package com.autosoft.meta.app;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.common.utils.AssertUtils;
import com.autosoft.meta.ddl.Identifiers;
import com.autosoft.meta.dto.MetaAppSaveDTO;
import com.autosoft.meta.dto.MetaEntitySaveDTO;
import com.autosoft.meta.dto.MetaFieldSaveDTO;
import com.autosoft.meta.dto.PageSchemaDTO;
import com.autosoft.meta.entity.MetaAppDO;
import com.autosoft.meta.entity.MetaEntityDO;
import com.autosoft.meta.entity.MetaFieldDO;
import com.autosoft.meta.entity.MetaPageDO;
import com.autosoft.meta.field.FieldTypes;
import com.autosoft.meta.mapper.MetaAppMapper;
import com.autosoft.meta.mapper.MetaEntityMapper;
import com.autosoft.meta.mapper.MetaFieldMapper;
import com.autosoft.meta.mapper.MetaPageMapper;
import com.autosoft.meta.page.LowCodeSchemaValidator;
import com.autosoft.meta.vo.MetaAppVO;
import com.autosoft.meta.vo.MetaEntityVO;
import com.autosoft.meta.vo.MetaFieldVO;
import com.autosoft.meta.vo.MetaPageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 元数据维护。发布后 code 不可改。
 */
@Service
public class MetaCatalogService {

    public static final String PAGE_TYPE_PAGE = "PAGE";

    private final MetaAppMapper appMapper;
    private final MetaEntityMapper entityMapper;
    private final MetaFieldMapper fieldMapper;
    private final MetaPageMapper pageMapper;

    public MetaCatalogService(MetaAppMapper appMapper, MetaEntityMapper entityMapper,
                              MetaFieldMapper fieldMapper, MetaPageMapper pageMapper) {
        this.appMapper = appMapper;
        this.entityMapper = entityMapper;
        this.fieldMapper = fieldMapper;
        this.pageMapper = pageMapper;
    }

    public List<MetaAppVO> listApps() {
        return appMapper.selectList(new LambdaQueryWrapper<MetaAppDO>().orderByDesc(MetaAppDO::getId))
                .stream().map(this::toAppVo).toList();
    }

    public MetaAppVO getAppSchema(Long appId) {
        MetaAppDO app = requireApp(appId);
        MetaAppVO vo = toAppVo(app);
        vo.setEntities(listEntities(appId).stream().map(entity -> {
            MetaEntityVO ev = toEntityVo(entity);
            ev.setFields(listFields(entity.getId()).stream().map(this::toFieldVo).toList());
            return ev;
        }).toList());
        vo.setPages(listAppPages(appId).stream().map(this::toPageVo).toList());
        return vo;
    }

    public MetaAppDO requireApp(Long id) {
        MetaAppDO app = appMapper.selectById(id);
        if (app == null) {
            throw new BizException(ResultCode.NOT_FOUND, "应用不存在");
        }
        return app;
    }

    public MetaAppDO requireAppByCode(String code) {
        MetaAppDO app = appMapper.selectOne(new LambdaQueryWrapper<MetaAppDO>().eq(MetaAppDO::getCode, code));
        if (app == null) {
            throw new BizException(ResultCode.NOT_FOUND, "应用不存在");
        }
        return app;
    }

    public MetaEntityDO requireEntity(Long id) {
        MetaEntityDO entity = entityMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "实体不存在");
        }
        return entity;
    }

    public MetaEntityDO requireEntity(String appCode, String entityCode) {
        MetaAppDO app = requireAppByCode(appCode);
        MetaEntityDO entity = entityMapper.selectOne(new LambdaQueryWrapper<MetaEntityDO>()
                .eq(MetaEntityDO::getAppId, app.getId()).eq(MetaEntityDO::getCode, entityCode));
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "实体不存在");
        }
        return entity;
    }

    public List<MetaEntityDO> listEntities(Long appId) {
        return entityMapper.selectList(new LambdaQueryWrapper<MetaEntityDO>()
                .eq(MetaEntityDO::getAppId, appId).orderByAsc(MetaEntityDO::getId));
    }

    public List<MetaPageDO> listAppPages(Long appId) {
        return pageMapper.selectList(new LambdaQueryWrapper<MetaPageDO>()
                .eq(MetaPageDO::getAppId, appId)
                .orderByAsc(MetaPageDO::getId));
    }

    public MetaPageDO requirePage(String appCode, String pageCode) {
        MetaAppDO app = requireAppByCode(appCode);
        MetaPageDO page = pageMapper.selectOne(new LambdaQueryWrapper<MetaPageDO>()
                .eq(MetaPageDO::getAppId, app.getId())
                .eq(MetaPageDO::getPageCode, pageCode));
        if (page == null) {
            throw new BizException(ResultCode.NOT_FOUND, "页面不存在");
        }
        return page;
    }

    public List<MetaFieldDO> listFields(Long entityId) {
        return fieldMapper.selectList(new LambdaQueryWrapper<MetaFieldDO>()
                .eq(MetaFieldDO::getEntityId, entityId).orderByAsc(MetaFieldDO::getSort).orderByAsc(MetaFieldDO::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createApp(MetaAppSaveDTO dto) {
        Identifiers.assertCode(dto.getCode(), "appCode");
        if (appMapper.selectOne(new LambdaQueryWrapper<MetaAppDO>().eq(MetaAppDO::getCode, dto.getCode())) != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "应用编码已存在");
        }
        MetaAppDO app = new MetaAppDO();
        app.setCode(dto.getCode());
        app.setName(dto.getName());
        app.setStatus(MetaAppDO.DRAFT);
        app.setVersion(0);
        app.setGrantRoles(dto.getGrantRoles() == null || dto.getGrantRoles().isBlank() ? "USER" : dto.getGrantRoles());
        app.setRemark(dto.getRemark());
        app.setAppKind(AppKind.from(dto.getAppKind()).code());
        appMapper.insert(app);
        return app.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateApp(Long id, MetaAppSaveDTO dto) {
        MetaAppDO app = requireApp(id);
        app.setName(dto.getName());
        if (dto.getGrantRoles() != null && !dto.getGrantRoles().isBlank()) {
            app.setGrantRoles(dto.getGrantRoles());
        }
        app.setRemark(dto.getRemark());
        appMapper.updateById(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteApp(Long id) {
        MetaAppDO app = requireApp(id);
        if (MetaAppDO.PUBLISHED.equals(app.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "已发布应用请先取消发布再删除");
        }
        for (MetaEntityDO entity : listEntities(id)) {
            fieldMapper.delete(new LambdaQueryWrapper<MetaFieldDO>().eq(MetaFieldDO::getEntityId, entity.getId()));
            pageMapper.delete(new LambdaQueryWrapper<MetaPageDO>().eq(MetaPageDO::getEntityId, entity.getId()));
            entityMapper.deleteById(entity.getId());
        }
        pageMapper.delete(new LambdaQueryWrapper<MetaPageDO>().eq(MetaPageDO::getAppId, id));
        appMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createEntity(Long appId, MetaEntitySaveDTO dto) {
        requireApp(appId);
        Identifiers.assertCode(dto.getCode(), "entityCode");
        if (entityMapper.selectOne(new LambdaQueryWrapper<MetaEntityDO>()
                .eq(MetaEntityDO::getAppId, appId).eq(MetaEntityDO::getCode, dto.getCode())) != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "实体编码已存在");
        }
        MetaEntityDO entity = new MetaEntityDO();
        entity.setAppId(appId);
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setRemark(dto.getRemark());
        entityMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateEntity(Long id, MetaEntitySaveDTO dto) {
        MetaEntityDO entity = requireEntity(id);
        entity.setName(dto.getName());
        entity.setRemark(dto.getRemark());
        entityMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteEntity(Long id) {
        requireEntity(id);
        entityMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addField(Long entityId, MetaFieldSaveDTO dto) {
        requireEntity(entityId);
        Identifiers.assertCode(dto.getCode(), "fieldCode");
        AssertUtils.isTrue(!Identifiers.SYSTEM_COLUMNS.contains(dto.getCode()), "不能使用系统列名");
        FieldTypes.require(dto.getFieldType());
        if (fieldMapper.selectOne(new LambdaQueryWrapper<MetaFieldDO>()
                .eq(MetaFieldDO::getEntityId, entityId).eq(MetaFieldDO::getCode, dto.getCode())) != null) {
            throw new BizException(ResultCode.BAD_REQUEST, "字段编码已存在");
        }
        MetaFieldDO field = fromDto(entityId, dto);
        fieldMapper.insert(field);
        return field.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateField(Long fieldId, MetaFieldSaveDTO dto) {
        MetaFieldDO field = fieldMapper.selectById(fieldId);
        if (field == null) {
            throw new BizException(ResultCode.NOT_FOUND, "字段不存在");
        }
        FieldTypes.require(dto.getFieldType());
        field.setName(dto.getName());
        field.setFieldType(FieldTypes.require(dto.getFieldType()));
        field.setLength(dto.getLength());
        field.setNullableFlag(dto.getNullableFlag());
        field.setDefaultValue(dto.getDefaultValue());
        field.setOptionsJson(dto.getOptionsJson());
        field.setRefApp(dto.getRefApp());
        field.setRefEntity(dto.getRefEntity());
        field.setSort(dto.getSort() == null ? field.getSort() : dto.getSort());
        field.setQueryable(dto.getQueryable());
        field.setListed(dto.getListed());
        field.setRequiredFlag(dto.getRequiredFlag());
        fieldMapper.updateById(field);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteField(Long fieldId) {
        if (fieldMapper.selectById(fieldId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "字段不存在");
        }
        fieldMapper.deleteById(fieldId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAppPage(Long appId, String pageCode, String layout, String schemaJson) {
        requireApp(appId);
        Identifiers.assertCode(pageCode, "pageCode");
        AssertUtils.isTrue(layout != null && !layout.isBlank(), "layout 不能为空");
        LowCodeSchemaValidator.validate(schemaJson);
        MetaPageDO page = pageMapper.selectOne(new LambdaQueryWrapper<MetaPageDO>()
                .eq(MetaPageDO::getAppId, appId)
                .eq(MetaPageDO::getPageCode, pageCode));
        if (page == null) {
            page = new MetaPageDO();
            page.setAppId(appId);
            page.setPageCode(pageCode);
            page.setPageType(PAGE_TYPE_PAGE);
            page.setLayout(layout);
            page.setSchemaJson(schemaJson);
            pageMapper.insert(page);
            return;
        }
        page.setLayout(layout);
        page.setSchemaJson(schemaJson);
        pageMapper.updateById(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePage(Long entityId, String pageType, PageSchemaDTO dto) {
        requireEntity(entityId);
        AssertUtils.isTrue("LIST".equals(pageType) || "FORM".equals(pageType) || "DETAIL".equals(pageType), "页面类型非法");
        MetaPageDO page = pageMapper.selectOne(new LambdaQueryWrapper<MetaPageDO>()
                .eq(MetaPageDO::getEntityId, entityId).eq(MetaPageDO::getPageType, pageType));
        if (page == null) {
            page = new MetaPageDO();
            page.setEntityId(entityId);
            page.setPageType(pageType);
            page.setSchemaJson(dto.getSchemaJson());
            pageMapper.insert(page);
            return;
        }
        page.setSchemaJson(dto.getSchemaJson());
        pageMapper.updateById(page);
    }

    public MetaFieldVO toFieldVo(MetaFieldDO source) {
        MetaFieldVO vo = new MetaFieldVO();
        vo.setId(source.getId());
        vo.setEntityId(source.getEntityId());
        vo.setCode(source.getCode());
        vo.setName(source.getName());
        vo.setFieldType(source.getFieldType());
        vo.setLength(source.getLength());
        vo.setNullableFlag(source.getNullableFlag());
        vo.setDefaultValue(source.getDefaultValue());
        vo.setOptionsJson(source.getOptionsJson());
        vo.setRefApp(source.getRefApp());
        vo.setRefEntity(source.getRefEntity());
        vo.setSort(source.getSort());
        vo.setQueryable(source.getQueryable());
        vo.setListed(source.getListed());
        vo.setRequiredFlag(source.getRequiredFlag());
        return vo;
    }

    private MetaFieldDO fromDto(Long entityId, MetaFieldSaveDTO dto) {
        MetaFieldDO field = new MetaFieldDO();
        field.setEntityId(entityId);
        field.setCode(dto.getCode());
        field.setName(dto.getName());
        field.setFieldType(FieldTypes.require(dto.getFieldType()));
        field.setLength(dto.getLength());
        field.setNullableFlag(dto.getNullableFlag() == null ? 1 : dto.getNullableFlag());
        field.setDefaultValue(dto.getDefaultValue());
        field.setOptionsJson(dto.getOptionsJson());
        field.setRefApp(dto.getRefApp());
        field.setRefEntity(dto.getRefEntity());
        field.setSort(dto.getSort() == null ? 0 : dto.getSort());
        field.setQueryable(dto.getQueryable() == null ? 0 : dto.getQueryable());
        field.setListed(dto.getListed() == null ? 1 : dto.getListed());
        field.setRequiredFlag(dto.getRequiredFlag() == null ? 0 : dto.getRequiredFlag());
        return field;
    }

    private MetaAppVO toAppVo(MetaAppDO source) {
        MetaAppVO vo = new MetaAppVO();
        vo.setId(source.getId());
        vo.setCode(source.getCode());
        vo.setName(source.getName());
        vo.setStatus(source.getStatus());
        vo.setVersion(source.getVersion());
        vo.setGrantRoles(source.getGrantRoles());
        vo.setRemark(source.getRemark());
        vo.setAppKind(source.getAppKind() == null ? AppKind.ADMIN.code() : source.getAppKind());
        return vo;
    }

    private MetaPageVO toPageVo(MetaPageDO source) {
        MetaPageVO vo = new MetaPageVO();
        vo.setId(source.getId());
        vo.setAppId(source.getAppId());
        vo.setEntityId(source.getEntityId());
        vo.setPageCode(source.getPageCode());
        vo.setPageType(source.getPageType());
        vo.setLayout(source.getLayout());
        vo.setSchemaJson(source.getSchemaJson());
        return vo;
    }

    private MetaEntityVO toEntityVo(MetaEntityDO source) {
        MetaEntityVO vo = new MetaEntityVO();
        vo.setId(source.getId());
        vo.setAppId(source.getAppId());
        vo.setCode(source.getCode());
        vo.setName(source.getName());
        vo.setRemark(source.getRemark());
        return vo;
    }
}
