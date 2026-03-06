package com.fakel.controller;

import com.fakel.dto.FileUploadResponse;
import com.fakel.service.YandexDiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Autowired
    private YandexDiskService yandexDiskService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new FileUploadResponse(null, null, null, 0, "Файл пустой"));
            }

            String fileUrl = yandexDiskService.uploadFile(file);

            FileUploadResponse response = new FileUploadResponse(
                    fileUrl,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    "Файл успешно загружен"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new FileUploadResponse(null, null, null, 0, "Ошибка загрузки: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-multiple")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<FileUploadResponse> responses = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (!file.isEmpty()) {
                    String fileUrl = yandexDiskService.uploadFile(file);

                    responses.add(new FileUploadResponse(
                            fileUrl,
                            file.getOriginalFilename(),
                            file.getContentType(),
                            file.getSize(),
                            "Успешно загружен"
                    ));
                }
            } catch (Exception e) {
                errors.add("Ошибка загрузки файла " + file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("uploaded", responses);
        result.put("errors", errors);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> deleteFile(@RequestParam String filePath) {
        try {
            yandexDiskService.deleteFile(filePath);
            return ResponseEntity.ok("Файл успешно удален");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка удаления: " + e.getMessage());
        }
    }
}