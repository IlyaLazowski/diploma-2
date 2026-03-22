package com.fakel.controller;

import com.fakel.dto.UpdateCadetProfileRequest;
import com.fakel.service.CadetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cadet")
public class CadetController {

    @Autowired
    private CadetService cadetService;


    @PutMapping("/profile")
    @PreAuthorize("hasRole('CADET')")
    public void updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateCadetProfileRequest request) {

        cadetService.updateProfile(userDetails, request);
    }
}