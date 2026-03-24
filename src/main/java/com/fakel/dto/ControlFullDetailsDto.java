package com.fakel.dto;

import java.time.LocalDate;
import java.util.List;

public class ControlFullDetailsDto {
    // Информация о контроле
    private Long controlId;
    private String controlType;
    private LocalDate controlDate;

    // Информация о группе
    private Long groupId;
    private String groupNumber;
    private LocalDate groupFoundationDate;

    // Информация об инспекторах
    private List<InspectorInfoDto> inspectors;

    // Результаты курсантов
    private List<CadetFullResultDto> cadetResults;

    // Статистика
    private ControlStatisticsDto statistics;

    // Геттеры и сеттеры
    public Long getControlId() { return controlId; }
    public void setControlId(Long controlId) { this.controlId = controlId; }

    public String getControlType() { return controlType; }
    public void setControlType(String controlType) { this.controlType = controlType; }

    public LocalDate getControlDate() { return controlDate; }
    public void setControlDate(LocalDate controlDate) { this.controlDate = controlDate; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupNumber() { return groupNumber; }
    public void setGroupNumber(String groupNumber) { this.groupNumber = groupNumber; }

    public LocalDate getGroupFoundationDate() { return groupFoundationDate; }
    public void setGroupFoundationDate(LocalDate groupFoundationDate) { this.groupFoundationDate = groupFoundationDate; }

    public List<InspectorInfoDto> getInspectors() { return inspectors; }
    public void setInspectors(List<InspectorInfoDto> inspectors) { this.inspectors = inspectors; }

    public List<CadetFullResultDto> getCadetResults() { return cadetResults; }
    public void setCadetResults(List<CadetFullResultDto> cadetResults) { this.cadetResults = cadetResults; }

    public ControlStatisticsDto getStatistics() { return statistics; }
    public void setStatistics(ControlStatisticsDto statistics) { this.statistics = statistics; }

    // Вложенный DTO для инспектора
    public static class InspectorInfoDto {
        private Long id;
        private String fullName;
        private Boolean external;
        private String rank; // воинское звание для преподавателей

        public InspectorInfoDto() {}

        public InspectorInfoDto(Long id, String fullName, Boolean external, String rank) {
            this.id = id;
            this.fullName = fullName;
            this.external = external;
            this.rank = rank;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public Boolean getExternal() { return external; }
        public void setExternal(Boolean external) { this.external = external; }

        public String getRank() { return rank; }
        public void setRank(String rank) { this.rank = rank; }
    }

    // Вложенный DTO для статистики
    public static class ControlStatisticsDto {
        private Integer totalCadets;
        private Integer presentCount;
        private Integer absentCount;
        private Integer sickCount;
        private Integer dutyCount;
        private Integer guardhouseCount;
        private Double averageMark;
        private Integer excellentCount; // 8-10 баллов
        private Integer goodCount;       // 6-7 баллов
        private Integer satisfactoryCount; // 4-5 баллов
        private Integer unsatisfactoryCount; // 1-3 балла

        public Integer getTotalCadets() { return totalCadets; }
        public void setTotalCadets(Integer totalCadets) { this.totalCadets = totalCadets; }

        public Integer getPresentCount() { return presentCount; }
        public void setPresentCount(Integer presentCount) { this.presentCount = presentCount; }

        public Integer getAbsentCount() { return absentCount; }
        public void setAbsentCount(Integer absentCount) { this.absentCount = absentCount; }

        public Integer getSickCount() { return sickCount; }
        public void setSickCount(Integer sickCount) { this.sickCount = sickCount; }

        public Integer getDutyCount() { return dutyCount; }
        public void setDutyCount(Integer dutyCount) { this.dutyCount = dutyCount; }

        public Integer getGuardhouseCount() { return guardhouseCount; }
        public void setGuardhouseCount(Integer guardhouseCount) { this.guardhouseCount = guardhouseCount; }

        public Double getAverageMark() { return averageMark; }
        public void setAverageMark(Double averageMark) { this.averageMark = averageMark; }

        public Integer getExcellentCount() { return excellentCount; }
        public void setExcellentCount(Integer excellentCount) { this.excellentCount = excellentCount; }

        public Integer getGoodCount() { return goodCount; }
        public void setGoodCount(Integer goodCount) { this.goodCount = goodCount; }

        public Integer getSatisfactoryCount() { return satisfactoryCount; }
        public void setSatisfactoryCount(Integer satisfactoryCount) { this.satisfactoryCount = satisfactoryCount; }

        public Integer getUnsatisfactoryCount() { return unsatisfactoryCount; }
        public void setUnsatisfactoryCount(Integer unsatisfactoryCount) { this.unsatisfactoryCount = unsatisfactoryCount; }
    }
}