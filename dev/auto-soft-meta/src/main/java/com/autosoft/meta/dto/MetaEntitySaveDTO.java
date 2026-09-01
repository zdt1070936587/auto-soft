package com.autosoft.meta.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * MetaEntitySave传输对象。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
public class MetaEntitySaveDTO {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String remark;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
