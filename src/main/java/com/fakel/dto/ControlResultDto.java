package com.fakel.dto;

public class ControlResultDto {
    private Long id;
    private Long cadetId;
    private String cadetName;
    private String cadetRank;
    private String status;
    private String standardName;
    private Integer standardNumber;
    private Short mark;

    public ControlResultDto() {}

    public ControlResultDto(Long id, Long cadetId, String cadetName, String cadetRank,
                            String status, String standardName, Integer standardNumber, Short mark) {
        this.id = id;
        this.cadetId = cadetId;
        this.cadetName = cadetName;
        this.cadetRank = cadetRank;
        this.status = status;
        this.standardName = standardName;
        this.standardNumber = standardNumber;
        this.mark = mark;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public String getCadetName() { return cadetName; }
    public void setCadetName(String cadetName) { this.cadetName = cadetName; }

    public String getCadetRank() { return cadetRank; }
    public void setCadetRank(String cadetRank) { this.cadetRank = cadetRank; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStandardName() { return standardName; }
    public void setStandardName(String standardName) { this.standardName = standardName; }

    public Integer getStandardNumber() { return standardNumber; }
    public void setStandardNumber(Integer standardNumber) { this.standardNumber = standardNumber; }

    public Short getMark() { return mark; }
    public void setMark(Short mark) { this.mark = mark; }
}