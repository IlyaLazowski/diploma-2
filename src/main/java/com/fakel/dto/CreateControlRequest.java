package com.fakel.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class CreateControlRequest {

    @NotNull(message = "ID группы обязателен")
    private Long groupId;

    @NotNull(message = "Тип контроля обязателен")
    private String type;

    @NotNull(message = "Дата обязательна")
    private LocalDate date;

    @NotNull(message = "Список номеров нормативов обязателен")
    @Size(min = 1, message = "Должен быть хотя бы один норматив")
    private List<Short> standardNumbers;

    private List<Long> inspectorIds;

    // Геттеры и сеттеры
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<Short> getStandardNumbers() { return standardNumbers; }
    public void setStandardNumbers(List<Short> standardNumbers) { this.standardNumbers = standardNumbers; }

    public List<Long> getInspectorIds() { return inspectorIds; }
    public void setInspectorIds(List<Long> inspectorIds) { this.inspectorIds = inspectorIds; }
}