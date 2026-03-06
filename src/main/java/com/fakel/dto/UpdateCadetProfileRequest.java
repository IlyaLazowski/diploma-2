package com.fakel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public class UpdateCadetProfileRequest {

    @Email(message = "Некорректный email")
    private String mail;

    @Pattern(regexp = "^\\+375\\d{9}$", message = "Телефон должен быть в формате +375XXXXXXXXX")
    private String phoneNumber;

    private BigDecimal weight;

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
}