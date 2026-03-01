package com.fakel.dto;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String role;

    public AuthResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }

    // Геттеры
    public String getToken() { return token; }
    public String getType() { return type; }
    public String getRole() { return role; }

    // Сеттеры
    public void setToken(String token) { this.token = token; }
    public void setType(String type) { this.type = type; }
    public void setRole(String role) { this.role = role; }
}