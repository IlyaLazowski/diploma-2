package com.fakel.dto;

import java.time.LocalDate;

public class GroupDto {
    private Long id;
    private String number;
    private Integer year;
    private String foundationDate;
    private String universityName;
    private Long universityId;
    private Integer studentCount;

    public GroupDto() {}

    public GroupDto(Long id, String number, LocalDate foundationDate,
                    String universityName, Long universityId, Integer studentCount) {
        this.id = id;
        this.number = number;
        this.year = foundationDate != null ? foundationDate.getYear() : 0;
        this.foundationDate = foundationDate != null ? foundationDate.toString() : null;
        this.universityName = universityName;
        this.universityId = universityId;
        this.studentCount = studentCount != null ? studentCount : 0;
    }

    // Геттеры
    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public Integer getYear() {
        return year;
    }

    public String getFoundationDate() {
        return foundationDate;
    }

    public String getUniversityName() {
        return universityName;
    }

    public Long getUniversityId() {
        return universityId;
    }

    public Integer getStudentCount() {
        return studentCount;
    }

    // Сеттеры
    public void setId(Long id) {
        this.id = id;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setFoundationDate(String foundationDate) {
        this.foundationDate = foundationDate;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }
}