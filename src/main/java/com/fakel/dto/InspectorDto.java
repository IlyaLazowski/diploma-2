package com.fakel.dto;

public class InspectorDto {
    private Long id;
    private String fullName;
    private Boolean external;
    private Long teacherId;

    public InspectorDto() {}

    public InspectorDto(Long id, String fullName, Boolean external, Long teacherId) {
        this.id = id;
        this.fullName = fullName;
        this.external = external;
        this.teacherId = teacherId;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Boolean getExternal() { return external; }
    public void setExternal(Boolean external) { this.external = external; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
}