package com.fakel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String qualification;

    @Column(name = "military_rank")
    private String militaryRank;

    private String post;

    // Геттеры и сеттеры
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getMilitaryRank() { return militaryRank; }
    public void setMilitaryRank(String militaryRank) { this.militaryRank = militaryRank; }

    public String getPost() { return post; }
    public void setPost(String post) { this.post = post; }

    // Университет берем из user
    public University getUniversity() {
        return user != null ? user.getUniversity() : null;
    }
}