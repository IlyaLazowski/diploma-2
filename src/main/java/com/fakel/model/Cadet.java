package com.fakel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cadets")
public class Cadet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private BigDecimal weight;

    @Column(name = "military_rank")
    private String militaryRank;

    private String post;

    private Short course;  // smallint в БД → Short в Java

    // Геттеры и сеттеры
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public Short getCourse() { return course; }
    public void setCourse(Short course) { this.course = course; }

    // Университет берем из user
    public University getUniversity() {
        return user != null ? user.getUniversity() : null;
    }
}