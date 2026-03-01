package com.fakel.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Это публичный endpoint, доступен всем";
    }

    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public String userEndpoint() {
        return "Это защищенный endpoint, доступен авторизованным пользователям";
    }
}