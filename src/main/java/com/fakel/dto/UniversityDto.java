package com.fakel.dto;

import java.math.BigDecimal;

public class UniversityDto {
    private Long id;
    private String name;        // code из таблицы
    private BigDecimal mark;    // оценка

    public UniversityDto(Long id, String name, BigDecimal mark) {
        this.id = id;
        this.name = name;
        this.mark = mark;
    }

    // Геттеры
    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getMark() { return mark; }
}