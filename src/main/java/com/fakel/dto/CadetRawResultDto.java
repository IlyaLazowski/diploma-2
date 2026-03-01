package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CadetRawResultDto {

    @NotNull(message = "ID курсанта обязателен")
    private Long cadetId;

    private String status;

    @NotNull(message = "Результаты по нормативам обязательны")
    private List<StandardRawResultDto> rawResults;

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<StandardRawResultDto> getRawResults() { return rawResults; }
    public void setRawResults(List<StandardRawResultDto> rawResults) { this.rawResults = rawResults; }
}