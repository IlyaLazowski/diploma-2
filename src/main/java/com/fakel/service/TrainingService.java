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

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (pageable == null) {
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        if (type != null && !isValidTrainingType(type)) {
            throw new IllegalArgumentException("Некорректный тип тренировки: " + type);
        }

        Long cadetId = getCadetId(userDetails);
        Page<Training> trainings;

        try {
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
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении тренировок: " + e.getMessage());
        }

        return trainings.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public TrainingDto getTrainingById(UserDetails userDetails, Long trainingId) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (trainingId == null || trainingId <= 0) {
            throw new IllegalArgumentException("ID тренировки должен быть положительным числом");
        }

        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Тренировка не найдена с id: " + trainingId));

        checkAccess(userDetails, training);

        return convertToDto(training);
    }

    // ============= РЕДАКТИРОВАНИЕ ТРЕНИРОВКИ =============

    @Transactional
    public void updateTraining(UserDetails userDetails, Long trainingId, UpdateTrainingRequest request) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (trainingId == null || trainingId <= 0) {
            throw new IllegalArgumentException("ID тренировки должен быть положительным числом");
        }

        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Тренировка не найдена с id: " + trainingId));

        checkAccess(userDetails, training);

        // Обновляем основные поля
        if (request.getDate() != null) {
            if (request.getDate().isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Дата тренировки не может быть в будущем");
            }
            training.setDate(request.getDate());
        }

        if (request.getCurrentWeight() != null) {
            if (request.getCurrentWeight().compareTo(BigDecimal.valueOf(30)) < 0 ||
                    request.getCurrentWeight().compareTo(BigDecimal.valueOf(180)) > 0) {
                throw new IllegalArgumentException("Вес должен быть от 30 до 180 кг");
            }
            training.setCurrentWeight(request.getCurrentWeight());
        }

        if (request.getRestPeriod() != null) {
            if (request.getRestPeriod() < 0 || request.getRestPeriod() > 3600) {
                throw new IllegalArgumentException("Период отдыха должен быть от 0 до 3600 секунд");
            }
            training.setRestPeriod(request.getRestPeriod().shortValue());
        }

        // Обновляем упражнения
        if (request.getExercises() != null && !request.getExercises().isEmpty()) {
            for (UpdateTrainingRequest.UpdateExerciseRequest exerciseReq : request.getExercises()) {

                if (exerciseReq.getExerciseInTrainingId() == null || exerciseReq.getExerciseInTrainingId() <= 0) {
                    throw new IllegalArgumentException("ID упражнения в тренировке должен быть положительным числом");
                }

                ExercisesInTraining exercise = exercisesInTrainingRepository.findById(exerciseReq.getExerciseInTrainingId())
                        .orElseThrow(() -> new RuntimeException("Упражнение в тренировке не найдено с id: " +
                                exerciseReq.getExerciseInTrainingId()));

                if (!exercise.getTraining().getId().equals(trainingId)) {
                    throw new RuntimeException("Упражнение не принадлежит этой тренировке");
                }

                if (exerciseReq.getRestPeriod() != null) {
                    if (exerciseReq.getRestPeriod() < 0 || exerciseReq.getRestPeriod() > 3600) {
                        throw new IllegalArgumentException("Период отдыха между подходами должен быть от 0 до 3600 секунд");
                    }
                    exercise.setRestPeriod(exerciseReq.getRestPeriod().shortValue());
                }

                if (exerciseReq.getApproaches() != null && !exerciseReq.getApproaches().isEmpty()) {
                    for (UpdateTrainingRequest.UpdateApproachRequest approachReq : exerciseReq.getApproaches()) {

                        if (approachReq.getApproachId() == null || approachReq.getApproachId() <= 0) {
                            throw new IllegalArgumentException("ID подхода должен быть положительным числом");
                        }

                        Approach approach = approachRepository.findById(approachReq.getApproachId())
                                .orElseThrow(() -> new RuntimeException("Подход не найден с id: " +
                                        approachReq.getApproachId()));

                        if (!approach.getExerciseInTraining().getId().equals(exercise.getId())) {
                            throw new RuntimeException("Подход не принадлежит этому упражнению");
                        }

                        if (approachReq.getValue() != null) {
                            if (approachReq.getValue().compareTo(BigDecimal.ZERO) < 0) {
                                throw new IllegalArgumentException("Значение подхода не может быть отрицательным");
                            }
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

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (trainingId == null || trainingId <= 0) {
            throw new IllegalArgumentException("ID тренировки должен быть положительным числом");
        }

        // Проверяем, что пользователь - курсант
        if (userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            throw new RuntimeException("Только курсант может удалять свои тренировки");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        if (!trainingRepository.existsByIdAndCadetId(trainingId, cadet.getUserId())) {
            throw new RuntimeException("Тренировка не найдена или не принадлежит курсанту");
        }

        try {
            trainingRepository.deleteById(trainingId);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при удалении тренировки: " + e.getMessage());
        }
    }

    // ============= ПОЛУЧЕНИЕ УПРАЖНЕНИЙ ПО ТИПУ =============

    @Transactional(readOnly = true)
    public List<ExerciseCatalogDto> getExercisesByType(String type) {

        if (type == null) {
            throw new IllegalArgumentException("Тип упражнения не может быть null");
        }

        if (!isValidTrainingType(type)) {
            throw new IllegalArgumentException("Некорректный тип упражнения: " + type);
        }

        List<ExerciseCatalog> exercises;
        try {
            exercises = exerciseCatalogRepository.findByType(type);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении упражнений: " + e.getMessage());
        }

        if (exercises == null) {
            return new ArrayList<>();
        }

        return exercises.stream()
                .filter(e -> e != null)
                .map(this::convertExerciseToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExerciseCatalogDto getExerciseWithDefaults(String code) {

        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Код упражнения не может быть пустым");
        }

        ExerciseCatalog exercise;
        try {
            exercise = exerciseCatalogRepository.findByCodeWithParameters(code.trim())
                    .orElseThrow(() -> new RuntimeException("Упражнение не найдено с кодом: " + code));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении упражнения: " + e.getMessage());
        }

        return convertExerciseToDto(exercise);
    }

    // ============= МЕТОД ДЛЯ ПРЕПОДАВАТЕЛЯ =============

    @Transactional(readOnly = true)
    public Page<TrainingDto> getTrainingsByCadetId(UserDetails userDetails, Long cadetId,
                                                   LocalDate date, LocalDate dateFrom,
                                                   LocalDate dateTo, String type,
                                                   Pageable pageable) {

        // Проверка входных данных
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (cadetId == null || cadetId <= 0) {
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }

        if (pageable == null) {
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        if (type != null && !isValidTrainingType(type)) {
            throw new IllegalArgumentException("Некорректный тип тренировки: " + type);
        }

        // Проверяем, что преподаватель имеет доступ к этому курсанту
        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

        Cadet cadet = cadetRepository.findById(cadetId)
                .orElseThrow(() -> new RuntimeException("Курсант не найден с id: " + cadetId));

        if (cadet.getGroupId() == null) {
            throw new RuntimeException("У курсанта не указана группа");
        }

        Group group = groupRepository.findById(cadet.getGroupId())
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (teacher.getUniversity() == null) {
            throw new RuntimeException("У преподавателя не указан университет");
        }

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            throw new RuntimeException("Нет доступа к тренировкам этого курсанта");
        }

        // Используем те же методы репозитория
        Page<Training> trainings;
        try {
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
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении тренировок: " + e.getMessage());
        }

        return trainings.map(this::convertToDto);
    }

    // ============= ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =============

    private Long getCadetId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        if (cadet.getUserId() == null) {
            throw new RuntimeException("У курсанта отсутствует ID");
        }

        return cadet.getUserId();
    }

    private void checkAccess(UserDetails userDetails, Training training) {
        if (userDetails == null || userDetails.getUsername() == null) {
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (training == null) {
            throw new IllegalArgumentException("Тренировка не может быть null");
        }

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            // Курсант может смотреть только свои тренировки
            Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));

            if (cadet.getUserId() == null) {
                throw new RuntimeException("У курсанта отсутствует ID");
            }

            if (!training.getCadetId().equals(cadet.getUserId())) {
                throw new RuntimeException("Нет доступа к этой тренировке");
            }
        } else {
            // Преподаватель может смотреть тренировки курсантов своей группы
            Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));

            if (teacher.getUniversity() == null) {
                throw new RuntimeException("У преподавателя не указан университет");
            }

            Cadet cadet = cadetRepository.findById(training.getCadetId())
                    .orElseThrow(() -> new RuntimeException("Курсант не найден"));

            if (cadet.getGroupId() == null) {
                throw new RuntimeException("У курсанта не указана группа");
            }

            Group group = groupRepository.findById(cadet.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Группа не найдена"));

            if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
                throw new RuntimeException("Нет доступа к тренировкам этого курсанта");
            }
        }
    }

    private boolean isValidTrainingType(String type) {
        if (type == null) return false;
        List<String> validTypes = List.of("Сила", "Скорость", "Выносливость");
        return validTypes.contains(type);
    }

    private TrainingDto convertToDto(Training training) {
        if (training == null) return null;

        List<ExerciseInTrainingDto> exerciseDtos = new ArrayList<>();
        if (training.getExercises() != null) {
            exerciseDtos = training.getExercises().stream()
                    .filter(e -> e != null)
                    .map(this::convertExerciseInTrainingToDto)
                    .collect(Collectors.toList());
        }

        return new TrainingDto(
                training.getId(),
                training.getDate(),
                training.getCurrentWeight() != null ? training.getCurrentWeight().doubleValue() : null,
                training.getType(),
                training.getRestPeriod() != null ? training.getRestPeriod().intValue() : null,
                exerciseDtos
        );
    }

    private ExerciseInTrainingDto convertExerciseInTrainingToDto(ExercisesInTraining exercise) {
        if (exercise == null) return null;

        List<ApproachDto> approachDtos = new ArrayList<>();
        if (exercise.getApproaches() != null) {
            approachDtos = exercise.getApproaches().stream()
                    .filter(a -> a != null)
                    .map(this::convertApproachToDto)
                    .collect(Collectors.toList());
        }

        return new ExerciseInTrainingDto(
                exercise.getId(),
                exercise.getExerciseCatalog() != null ? exercise.getExerciseCatalog().getCode() : null,
                exercise.getExerciseCatalog() != null ? exercise.getExerciseCatalog().getDescription() : null,
                exercise.getRestPeriod() != null ? exercise.getRestPeriod().intValue() : null,
                approachDtos
        );
    }

    private ApproachDto convertApproachToDto(Approach approach) {
        if (approach == null) return null;

        ExerciseParameter param = approach.getExerciseParameter();
        return new ApproachDto(
                approach.getId(),
                approach.getApproachNumber() != null ? approach.getApproachNumber().intValue() : null,
                param != null ? param.getCode() : null,
                param != null ? param.getCode() : null,
                param != null && param.getMeasurementUnit() != null ? param.getMeasurementUnit().getCode() : null,
                approach.getValue()
        );
    }

    private ExerciseCatalogDto convertExerciseToDto(ExerciseCatalog exercise) {
        if (exercise == null) return null;

        List<ExerciseParameterDto> paramDtos = new ArrayList<>();
        if (exercise.getParameters() != null) {
            paramDtos = exercise.getParameters().stream()
                    .filter(p -> p != null)
                    .map(this::convertParameterToDto)
                    .collect(Collectors.toList());
        }

        return new ExerciseCatalogDto(
                exercise.getId(),
                exercise.getCode(),
                exercise.getDescription(),
                exercise.getType(),
                paramDtos
        );
    }

    private ExerciseParameterDto convertParameterToDto(ExerciseParameter param) {
        if (param == null) return null;

        BigDecimal defaultValue = null;
        if (param.getDefaultIntValue() != null) {
            defaultValue = param.getDefaultIntValue();
        } else if (param.getDefaultTimeValue() != null) {
            defaultValue = BigDecimal.valueOf(param.getDefaultTimeValue().getSeconds());
        }

        return new ExerciseParameterDto(
                param.getId(),
                param.getCode(),
                param.getMeasurementUnit() != null ? param.getMeasurementUnit().getCode() : null,
                defaultValue
        );
    }
}