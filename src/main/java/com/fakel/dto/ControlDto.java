package com.fakel.dto;

import java.time.LocalDate;
import java.util.List;

public class ControlDto {
    private Long id;
    private String type;
    private LocalDate date;
    private String groupNumber;
    private Long groupId;
    private Integer studentsCount;
    private Integer passedCount;
    private Double averageMark;
    private List<InspectorDto> inspectors;
    private Integer inspectorsCount;
    private List<StandardDto> standards;  // добавили поле

    // Пустой конструктор
    public ControlDto() {}

    // Конструктор со всеми полями (включая standards)
    public ControlDto(
            Long id,
            String type,
            LocalDate date,
            String groupNumber,
            Long groupId,
            Integer studentsCount,
            Integer passedCount,
            Double averageMark,
            List<InspectorDto> inspectors,
            Integer inspectorsCount,
            List<StandardDto> standards) {
        this.id = id;
        this.type = type;
        this.date = date;
        this.groupNumber = groupNumber;
        this.groupId = groupId;
        this.studentsCount = studentsCount;
        this.passedCount = passedCount;
        this.averageMark = averageMark;
        this.inspectors = inspectors;
        this.inspectorsCount = inspectorsCount;
        this.standards = standards;
    }

    // Геттеры
    public Long getId() { return id; }
    public String getType() { return type; }
    public LocalDate getDate() { return date; }
    public String getGroupNumber() { return groupNumber; }
    public Long getGroupId() { return groupId; }
    public Integer getStudentsCount() { return studentsCount; }
    public Integer getPassedCount() { return passedCount; }
    public Double getAverageMark() { return averageMark; }
    public List<InspectorDto> getInspectors() { return inspectors; }
    public Integer getInspectorsCount() { return inspectorsCount; }
    public List<StandardDto> getStandards() { return standards; }  // новый геттер

    // Сеттеры
    public void setId(Long id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setGroupNumber(String groupNumber) { this.groupNumber = groupNumber; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public void setStudentsCount(Integer studentsCount) { this.studentsCount = studentsCount; }
    public void setPassedCount(Integer passedCount) { this.passedCount = passedCount; }
    public void setAverageMark(Double averageMark) { this.averageMark = averageMark; }
    public void setInspectors(List<InspectorDto> inspectors) { this.inspectors = inspectors; }
    public void setInspectorsCount(Integer inspectorsCount) { this.inspectorsCount = inspectorsCount; }
    public void setStandards(List<StandardDto> standards) { this.standards = standards; }  // новый сеттер
}