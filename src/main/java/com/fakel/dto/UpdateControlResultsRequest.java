package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class UpdateControlResultsRequest {

    @NotNull(message = "ID контроля обязателен")
    private Long controlId;

    @NotNull(message = "Список изменений обязателен")
    private List<UpdateControlResultRequest> updates;

    public Long getControlId() { return controlId; }
    public void setControlId(Long controlId) { this.controlId = controlId; }

    public List<UpdateControlResultRequest> getUpdates() { return updates; }
    public void setUpdates(List<UpdateControlResultRequest> updates) { this.updates = updates; }
}