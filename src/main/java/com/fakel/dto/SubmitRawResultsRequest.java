package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class SubmitRawResultsRequest {

    @NotNull(message = "ID контроля обязателен")
    private Long controlId;

    @NotNull(message = "Результаты обязательны")
    private List<CadetRawResultDto> results;

    public Long getControlId() { return controlId; }
    public void setControlId(Long controlId) { this.controlId = controlId; }

    public List<CadetRawResultDto> getResults() { return results; }
    public void setResults(List<CadetRawResultDto> results) { this.results = results; }
}