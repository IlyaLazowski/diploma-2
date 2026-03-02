package com.fakel.service;

import com.fakel.dto.*;
import com.fakel.model.*;
import com.fakel.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingService {

    @Autowired
    private TrainingRepository trainingRepository;

    @Autowired
    private CadetRepository cadetRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ExerciseCatalogRepository exerciseCatalogRepository;

    @Autowired
    private ExerciseParameterRepository exerciseParameterRepository;

    @Autowired
    private ExercisesInTrainingRepository exercisesInTrainingRepository;

    @Autowired
    private ApproachRepository approachRepository;

    @Autowired
    private GroupRepository groupRepository;

    // ============= ПРОСМОТР ТРЕНИРОВОК =============

    @Transactional(readOnly = true)
    public Page<TrainingDto> getTrainings(UserDetails userDetails,
                                          LocalDate date,
                                          LocalDate dateFrom,
                                          LocalDate dateTo,
                                          String type,
                                          Pageable pageable) {

        Long cadetId = getCadetId(userDetails);
        Page<Training> trainings;

        if (type != null && dateFrom != null && dateTo != null) {
            trainings = trainingRepository.findByCadetIdAndTypeAndDateBetween(
                    cadetId, type, dateFrom, dateTo, pageable);
        } else if (type != null) {
            trainings = trainingRepository.findByCadetIdAndType(cadetId, type, pageable);
        } else if (date != null) {
            trainings = trainingRepository.findByCadetIdAndDate(cadetId, date, pageable);
        } else if (dateFrom != null && dateTo != null) {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, dateFrom, dateTo, pageable);
        } else {
            trainings = trainingRepository.findByCadetId(cadetId, pageable);
        }

        return trainings.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public TrainingDto getTrainingById(UserDetails userDetails, Long trainingId) {
        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Тренировка не найдена"));

        checkAccess(userDetails, training);

        return convertToDto(training);
    }

    // ============= РЕДАКТИРОВАНИЕ ТРЕНИРОВКИ =============

    @Transactional
    public void updateTraining(UserDetails userDetails, Long trainingId, UpdateTrainingRequest request) {

        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Тренировка не найдена"));

        checkAccess(userDetails, training);

        // Обновляем основные поля
        if (request.getDate() != null) {
            training.setDate(request.getDate());
        }
        if (request.getCurrentWeight() != null) {
            training.setCurrentWeight(request.getCurrentWeight());
        }
        if (request.getRestPeriod() != null) {
            training.setRestPeriod(request.getRestPeriod().shortValue());  // Integer → Short
        }

        // Обновляем упражнения
        if (request.getExercises() != null) {
            for (UpdateTrainingRequest.UpdateExerciseRequest exerciseReq : request.getExercises()) {
                ExercisesInTraining exercise = exercisesInTrainingRepository.findById(exerciseReq.getExerciseInTrainingId())
                        .orElseThrow(() -> new RuntimeException("Упражнение в тренировке не найдено"));

                if (!exercise.getTraining().getId().equals(trainingId)) {
                    throw new RuntimeException("Упражнение не принадлежит этой тренировке");
                }

                if (exerciseReq.getRestPeriod() != null) {
                    exercise.setRestPeriod(exerciseReq.getRestPeriod().shortValue());  // ← ИСПРАВЛЕНО: Integer → Short
                }

                if (exerciseReq.getApproaches() != null) {
                    for (UpdateTrainingRequest.UpdateApproachRequest approachReq : exerciseReq.getApproaches()) {
                        Approach approach = approachRepository.findById(approachReq.getApproachId())
                                .orElseThrow(() -> new RuntimeException("Подход не найден"));

                        if (!approach.getExerciseInTraining().getId().equals(exercise.getId())) {
                            throw new RuntimeException("Подход не принадлежит этому упражнению");
                        }

                        if (approachReq.getValue() != null) {
                            approach.setValue(approachReq.getValue());
                        }
                        approachRepository.save(approach);
                    }
                }
                exercisesInTrainingRepository.save(exercise);
            }
        }

        trainingRepository.save(training);
    }

    // ============= УДАЛЕНИЕ ТРЕНИРОВКИ (ТОЛЬКО ДЛЯ КУРСАНТА) =============

    @Transactional
    public void deleteTraining(UserDetails userDetails, Long trainingId) {

        // Проверяем, что пользователь - курсант
        if (userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            throw new RuntimeException("Только курсант может удалять свои тренировки");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        if (!trainingRepository.existsByIdAndCadetId(trainingId, cadet.getUserId())) {
            throw new RuntimeException("Тренировка не найдена или не принадлежит курсанту");
        }

        trainingRepository.deleteById(trainingId);
    }

    // ============= ПОЛУЧЕНИЕ УПРАЖНЕНИЙ ПО ТИПУ =============

    @Transactional(readOnly = true)
    public List<ExerciseCatalogDto> getExercisesByType(String type) {
        List<ExerciseCatalog> exercises = exerciseCatalogRepository.findByType(type);
        return exercises.stream()
                .map(this::convertExerciseToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExerciseCatalogDto getExerciseWithDefaults(String code) {
        ExerciseCatalog exercise = exerciseCatalogRepository.findByCodeWithParameters(code)
                .orElseThrow(() -> new RuntimeException("Упражнение не найдено"));
        return convertExerciseToDto(exercise);
    }

    // ============= МЕТОД ДЛЯ ПРЕПОДАВАТЕЛЯ =============

    @Transactional(readOnly = true)
    public Page<TrainingDto> getTrainingsByCadetId(UserDetails userDetails, Long cadetId,
                                                   LocalDate date, LocalDate dateFrom,
                                                   LocalDate dateTo, String type,
                                                   Pageable pageable) {
        // Проверяем, что преподаватель имеет доступ к этому курсанту
        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Cadet cadet = cadetRepository.findById(cadetId)
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        Group group = groupRepository.findById(cadet.getGroupId())
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к тренировкам этого курсанта");
        }

        // Используем те же методы репозитория
        Page<Training> trainings;
        if (type != null && dateFrom != null && dateTo != null) {
            trainings = trainingRepository.findByCadetIdAndTypeAndDateBetween(
                    cadetId, type, dateFrom, dateTo, pageable);
        } else if (type != null) {
            trainings = trainingRepository.findByCadetIdAndType(cadetId, type, pageable);
        } else if (date != null) {
            trainings = trainingRepository.findByCadetIdAndDate(cadetId, date, pageable);
        } else if (dateFrom != null && dateTo != null) {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, dateFrom, dateTo, pageable);
        } else {
            trainings = trainingRepository.findByCadetId(cadetId, pageable);
        }

        return trainings.map(this::convertToDto);
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =============

    private Long getCadetId(UserDetails userDetails) {
        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));
        return cadet.getUserId();
    }

    private void checkAccess(UserDetails userDetails, Training training) {
        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            // Курсант может смотреть только свои тренировки
            Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));
            if (!training.getCadetId().equals(cadet.getUserId())) {
                throw new RuntimeException("Нет доступа к этой тренировке");
            }
        } else {
            // Преподаватель может смотреть тренировки курсантов своей группы
            Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

            Cadet cadet = cadetRepository.findById(training.getCadetId())
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));

            Group group = groupRepository.findById(cadet.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Группа не найдена"));

            if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
                throw new RuntimeException("Нет доступа к тренировкам этого курсанта");
            }
        }
    }

    private TrainingDto convertToDto(Training training) {
        List<ExerciseInTrainingDto> exerciseDtos = training.getExercises().stream()
                .map(this::convertExerciseInTrainingToDto)
                .collect(Collectors.toList());

        return new TrainingDto(
                training.getId(),
                training.getDate(),
                training.getCurrentWeight() != null ? training.getCurrentWeight().doubleValue() : null,
                training.getType(),
                training.getRestPeriod() != null ? training.getRestPeriod().intValue() : null,  // ← ИСПРАВЛЕНО: Short → Integer
                exerciseDtos
        );
    }

    private ExerciseInTrainingDto convertExerciseInTrainingToDto(ExercisesInTraining exercise) {
        List<ApproachDto> approachDtos = exercise.getApproaches().stream()
                .map(this::convertApproachToDto)
                .collect(Collectors.toList());

        return new ExerciseInTrainingDto(
                exercise.getId(),
                exercise.getExerciseCatalog().getCode(),
                exercise.getExerciseCatalog().getDescription(),
                exercise.getRestPeriod() != null ? exercise.getRestPeriod().intValue() : null,  // ← ИСПРАВЛЕНО: Short → Integer
                approachDtos
        );
    }

    private ApproachDto convertApproachToDto(Approach approach) {
        ExerciseParameter param = approach.getExerciseParameter();
        return new ApproachDto(
                approach.getId(),
                approach.getApproachNumber().intValue(),  // Short → Integer
                param.getCode(),
                param.getCode(),
                param.getMeasurementUnit().getCode(),
                approach.getValue()
        );
    }

    private ExerciseCatalogDto convertExerciseToDto(ExerciseCatalog exercise) {
        List<ExerciseParameterDto> paramDtos = exercise.getParameters().stream()
                .map(this::convertParameterToDto)
                .collect(Collectors.toList());

        return new ExerciseCatalogDto(
                exercise.getId(),
                exercise.getCode(),
                exercise.getDescription(),
                exercise.getType(),
                paramDtos
        );
    }

    private ExerciseParameterDto convertParameterToDto(ExerciseParameter param) {
        return new ExerciseParameterDto(
                param.getId(),
                param.getCode(),
                param.getMeasurementUnit().getCode(),
                param.getDefaultIntValue() != null ?
                        param.getDefaultIntValue() :
                        (param.getDefaultTimeValue() != null ?
                                BigDecimal.valueOf(param.getDefaultTimeValue().getSeconds()) : null)
        );
    }
}