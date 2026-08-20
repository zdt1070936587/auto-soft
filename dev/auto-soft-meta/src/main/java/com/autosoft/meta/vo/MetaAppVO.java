package com.autosoft.meta.vo;

import java.util.ArrayList;
import java.util.List;

public class MetaAppVO {
    private Long id;
    private String code;
    private String name;
    private String status;
    private Integer version;
    private String grantRoles;
    private String remark;
    private List<MetaEntityVO> entities = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getGrantRoles() { return grantRoles; }
    public void setGrantRoles(String grantRoles) { this.grantRoles = grantRoles; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<MetaEntityVO> getEntities() { return entities; }
    public void setEntities(List<MetaEntityVO> entities) { this.entities = entities; }
}
