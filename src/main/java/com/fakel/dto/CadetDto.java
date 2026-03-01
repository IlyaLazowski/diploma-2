package com.fakel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CadetDto {
    private Long id;
    private String fullName;
    private String login;
    private String mail;
    private String phoneNumber;
    private Long groupId;
    private LocalDate dateOfBirth;
    private BigDecimal weight;
    private String militaryRank;
    private String post;
    private Integer course;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public String getMilitaryRank() { return militaryRank; }
    public void setMilitaryRank(String militaryRank) { this.militaryRank = militaryRank; }

    public String getPost() { return post; }
    public void setPost(String post) { this.post = post; }

    public Integer getCourse() { return course; }
    public void setCourse(Integer course) { this.course = course; }
}