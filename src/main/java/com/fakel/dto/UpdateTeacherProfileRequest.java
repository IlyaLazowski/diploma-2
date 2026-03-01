package com.fakel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public class UpdateTeacherProfileRequest {

    @Email(message = "Некорректный email")
    private String mail;

    @Pattern(regexp = "^\\+375\\d{9}$", message = "Телефон должен быть в формате +375XXXXXXXXX")
    private String phoneNumber;

    private String qualification; // ученая степень/звание

    private String post; // должность

    // Геттеры и сеттеры
    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getPost() { return post; }
    public void setPost(String post) { this.post = post; }
}