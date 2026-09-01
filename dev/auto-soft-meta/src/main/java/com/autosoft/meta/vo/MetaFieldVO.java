package com.autosoft.meta.vo;

/**
 * MetaField视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class MetaFieldVO {
    private Long id;
    private Long entityId;
    private String code;
    private String name;
    private String fieldType;
    private Integer length;
    private Integer nullableFlag;
    private String defaultValue;
    private String optionsJson;
    private String refApp;
    private String refEntity;
    private Integer sort;
    private Integer queryable;
    private Integer listed;
    private Integer requiredFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }
    public Integer getNullableFlag() { return nullableFlag; }
    public void setNullableFlag(Integer nullableFlag) { this.nullableFlag = nullableFlag; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
    public String getRefApp() { return refApp; }
    public void setRefApp(String refApp) { this.refApp = refApp; }
    public String getRefEntity() { return refEntity; }
    public void setRefEntity(String refEntity) { this.refEntity = refEntity; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getQueryable() { return queryable; }
    public void setQueryable(Integer queryable) { this.queryable = queryable; }
    public Integer getListed() { return listed; }
    public void setListed(Integer listed) { this.listed = listed; }
    public Integer getRequiredFlag() { return requiredFlag; }
    public void setRequiredFlag(Integer requiredFlag) { this.requiredFlag = requiredFlag; }
}
