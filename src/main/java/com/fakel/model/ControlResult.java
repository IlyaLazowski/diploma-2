package com.fakel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "control_results")
public class ControlResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @ManyToOne
    @JoinColumn(name = "cadet_id", nullable = false)
    private Cadet cadet;

    @Column(nullable = false)
    private String status;  // Присутствует, Болен, Командировка, Гауптвахта

    @ManyToOne
    @JoinColumn(name = "standard_id", nullable = false)
    private Standard standard;

    private Short mark;  // оценка от 0 до 5

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Control getControl() { return control; }
    public void setControl(Control control) { this.control = control; }

    public Cadet getCadet() { return cadet; }
    public void setCadet(Cadet cadet) { this.cadet = cadet; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Standard getStandard() { return standard; }
    public void setStandard(Standard standard) { this.standard = standard; }

    public Short getMark() { return mark; }
    public void setMark(Short mark) { this.mark = mark; }
}