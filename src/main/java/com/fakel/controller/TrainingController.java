package com.fakel.controller;

import com.fakel.dto.ExerciseCatalogDto;
import com.fakel.dto.TrainingDto;
import com.fakel.dto.UpdateTrainingRequest;
import com.fakel.service.TrainingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TrainingController {

    @Autowired
    private TrainingService trainingService;

    // ============= ПРОСМОТР ТРЕНИРОВОК =============

    /**
     * GET /api/my/trainings?page=0&size=10
     * GET /api/my/trainings?type=Сила
     * GET /api/my/trainings?date=2026-02-02
     * GET /api/my/trainings?dateFrom=2026-02-01&dateTo=2026-02-28
     * GET /api/my/trainings?type=Сила&dateFrom=2026-02-01&dateTo=2026-02-28
     */
    @GetMapping("/my/trainings")
    @PreAuthorize("hasRole('CADET')")
    public Page<TrainingDto> getMyTrainings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by("date").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return trainingService.getTrainings(userDetails, date, dateFrom, dateTo, type, pageable);
    }

    /**
     * GET /api/cadets/{cadetId}/trainings?page=0&size=10
     * Для преподавателя - просмотр тренировок конкретного курсанта
     */
    @GetMapping("/cadets/{cadetId}/trainings")
    @PreAuthorize("hasRole('TEACHER')")
    public Page<TrainingDto> getCadetTrainings(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cadetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by("date").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // В сервисе нужно будет добавить метод для получения тренировок по ID курсанта
        return trainingService.getTrainingsByCadetId(userDetails, cadetId, date, dateFrom, dateTo, type, pageable);
    }

    /**
     * GET /api/my/trainings/{trainingId}
     */
    @GetMapping("/my/trainings/{trainingId}")
    @PreAuthorize("hasAnyRole('CADET', 'TEACHER')")
    public TrainingDto getTrainingById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long trainingId) {
        return trainingService.getTrainingById(userDetails, trainingId);
    }

    // ============= РЕДАКТИРОВАНИЕ ТРЕНИРОВКИ =============

    /**
     * PUT /api/my/trainings/{trainingId}
     * Редактирование тренировки (только для курсанта)
     */
    @PutMapping("/my/trainings/{trainingId}")
    @PreAuthorize("hasRole('CADET')")
    public void updateTraining(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long trainingId,
            @Valid @RequestBody UpdateTrainingRequest request) {
        trainingService.updateTraining(userDetails, trainingId, request);
    }

    // ============= УДАЛЕНИЕ ТРЕНИРОВКИ =============

    /**
     * DELETE /api/my/trainings/{trainingId}
     * Удаление тренировки (только для курсанта)
     */
    @DeleteMapping("/my/trainings/{trainingId}")
    @PreAuthorize("hasRole('CADET')")
    public void deleteTraining(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long trainingId) {
        trainingService.deleteTraining(userDetails, trainingId);
    }

    // ============= ПОЛУЧЕНИЕ УПРАЖНЕНИЙ ПО ТИПУ =============

    /**
     * GET /api/exercises/types?type=Сила
     * GET /api/exercises/types?type=Скорость
     * GET /api/exercises/types?type=Выносливость
     */
    @GetMapping("/exercises/types")
    @PreAuthorize("hasAnyRole('CADET', 'TEACHER')")
    public List<ExerciseCatalogDto> getExercisesByType(@RequestParam String type) {
        return trainingService.getExercisesByType(type);
    }

    /**
     * GET /api/exercises/{code}/defaults
     * Получить упражнение с дефолтными параметрами
     */
    @GetMapping("/exercises/{code}/defaults")
    @PreAuthorize("hasAnyRole('CADET', 'TEACHER')")
    public ExerciseCatalogDto getExerciseWithDefaults(@PathVariable String code) {
        return trainingService.getExerciseWithDefaults(code);
    }
}