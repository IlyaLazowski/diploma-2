package com.fakel.controller;

import com.fakel.dto.UniversityDto;
import com.fakel.service.UniversityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/universities")
public class UniversityController {

    @Autowired
    private UniversityService universityService;

    // GET http://localhost:8080/api/universities
    @GetMapping
    public List<UniversityDto> getAllUniversities() {
        return universityService.getAllUniversities();
    }
}