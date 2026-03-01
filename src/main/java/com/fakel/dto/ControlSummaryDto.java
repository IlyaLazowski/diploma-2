package com.fakel.dto;

import java.util.List;

public class ControlSummaryDto {
    private Long cadetId;
    private String fullName;
    private String militaryRank;
    private String post;
    private String status;
    private List<StandardResultDto> standards;
    private Short finalMark;

    public ControlSummaryDto() {}

    // Геттеры и сеттеры
    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getMilitaryRank() { return militaryRank; }
    public void setMilitaryRank(String militaryRank) { this.militaryRank = militaryRank; }

    public String getPost() { return post; }
    public void setPost(String post) { this.post = post; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<StandardResultDto> getStandards() { return standards; }
    public void setStandards(List<StandardResultDto> standards) { this.standards = standards; }

    public Short getFinalMark() { return finalMark; }
    public void setFinalMark(Short finalMark) { this.finalMark = finalMark; }
}