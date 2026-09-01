package com.autosoft.meta.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * MetaEntity视图对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class MetaEntityVO {
    private Long id;
    private Long appId;
    private String code;
    private String name;
    private String remark;
    private List<MetaFieldVO> fields = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<MetaFieldVO> getFields() { return fields; }
    public void setFields(List<MetaFieldVO> fields) { this.fields = fields; }
}
