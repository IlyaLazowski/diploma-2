package com.fakel.controller;

import com.fakel.model.Cadet;
import com.fakel.repository.CadetRepository;
import com.fakel.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final CadetRepository cadetRepository;

    @Autowired
    public ReportController(ReportService reportService, CadetRepository cadetRepository) {
        this.reportService = reportService;
        this.cadetRepository = cadetRepository;
    }

    @GetMapping(value = "/cadet/{cadetId}/training/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'CADET')")
    public ResponseEntity<byte[]> generateTrainingReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cadetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) List<String> types
    ) throws Exception {

        // Если курсант - проверяем, что запрашивает свой отчет
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));

            if (!cadet.getUserId().equals(cadetId)) {
                throw new RuntimeException("Нет доступа к отчетам другого курсанта");
            }
        }

        byte[] pdfBytes = reportService.generateTrainingReport(cadetId, dateFrom, dateTo, types);

        String filename = String.format("training_report_%d_%s_%s.pdf",
                cadetId, dateFrom.toString(), dateTo.toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}