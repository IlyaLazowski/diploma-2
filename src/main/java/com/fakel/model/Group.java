package com.fakel.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String number;

    @Column(name = "foundation_date", nullable = false)
    private LocalDate foundationDate;

    @ManyToOne
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public LocalDate getFoundationDate() { return foundationDate; }
    public void setFoundationDate(LocalDate foundationDate) { this.foundationDate = foundationDate; }

    public University getUniversity() { return university; }
    public void setUniversity(University university) { this.university = university; }
}