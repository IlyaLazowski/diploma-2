package com.fakel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "inspectors")
public class Inspector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "control_id", nullable = false)
    private Control control;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String patronymic;

    @Column(nullable = false)
    private Boolean external = false;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Control getControl() { return control; }
    public void setControl(Control control) { this.control = control; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPatronymic() { return patronymic; }
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }

    public Boolean getExternal() { return external; }
    public void setExternal(Boolean external) { this.external = external; }

    // Полное имя инспектора
    public String getFullName() {
        String fullName = lastName + " " + firstName;
        if (patronymic != null && !patronymic.isEmpty()) {
            fullName += " " + patronymic;
        }
        return fullName;
    }
}