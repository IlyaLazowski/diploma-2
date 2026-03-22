package com.fakel.controller;

import com.fakel.dto.UpdateTeacherProfileRequest;
import com.fakel.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private TeacherService teacherService;


    @PutMapping("/profile")
    @PreAuthorize("hasRole('TEACHER')")
    public void updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateTeacherProfileRequest request) {

        teacherService.updateProfile(userDetails, request);
    }
}