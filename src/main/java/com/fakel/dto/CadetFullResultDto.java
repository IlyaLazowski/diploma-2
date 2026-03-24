package com.fakel.dto;

import java.util.List;

public class CadetFullResultDto {
    private Long cadetId;
    private String fullName;
    private String militaryRank;
    private String post;
    private Integer course;
    private String status;
    private List<StandardResultDetailDto> standards;
    private Short finalMark;
    private String finalGrade;

    public CadetFullResultDto() {}

    public Long getCadetId() { return cadetId; }
    public void setCadetId(Long cadetId) { this.cadetId = cadetId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getMilitaryRank() { return militaryRank; }
    public void setMilitaryRank(String militaryRank) { this.militaryRank = militaryRank; }

    public String getPost() { return post; }
    public void setPost(String post) { this.post = post; }

    public Integer getCourse() { return course; }
    public void setCourse(Integer course) { this.course = course; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<StandardResultDetailDto> getStandards() { return standards; }
    public void setStandards(List<StandardResultDetailDto> standards) { this.standards = standards; }

    public Short getFinalMark() { return finalMark; }
    public void setFinalMark(Short finalMark) { this.finalMark = finalMark; }

    public String getFinalGrade() { return finalGrade; }
    public void setFinalGrade(String finalGrade) { this.finalGrade = finalGrade; }
}