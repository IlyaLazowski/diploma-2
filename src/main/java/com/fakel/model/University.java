package com.fakel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "universities")
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String code;

    @Column(precision = 4, scale = 2)
    private BigDecimal mark;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public BigDecimal getMark() { return mark; }
    public void setMark(BigDecimal mark) { this.mark = mark; }
}