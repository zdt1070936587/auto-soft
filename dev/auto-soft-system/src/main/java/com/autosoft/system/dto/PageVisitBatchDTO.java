package com.autosoft.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PageVisitBatchDTO {

    @NotEmpty(message = "visits 不能为空")
    @Size(max = 50, message = "单次最多上报 50 条")
    @Valid
    private List<PageVisitItemDTO> visits;

    public List<PageVisitItemDTO> getVisits() {
        return visits;
    }

    public void setVisits(List<PageVisitItemDTO> visits) {
        this.visits = visits;
    }
}
