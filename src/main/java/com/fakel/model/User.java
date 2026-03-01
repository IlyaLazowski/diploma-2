package com.fakel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String patronymic;

    @Column(nullable = false)
    private String sex;

    // ⚠️⚠️⚠️ ВОТ ТУТ БЫЛА ОШИБКА — НЕ ХВАТАЛО ЭТОГО ПОЛЯ!
    @ManyToOne
    @JoinColumn(name = "university_id")
    private University university;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String mail;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPatronymic() { return patronymic; }
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public University getUniversity() { return university; }
    public void setUniversity(University university) { this.university = university; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}