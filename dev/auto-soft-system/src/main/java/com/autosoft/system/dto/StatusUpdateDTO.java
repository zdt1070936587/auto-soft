package com.autosoft.system.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 启用/停用。
 */
public class StatusUpdateDTO {

    @NotNull
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
