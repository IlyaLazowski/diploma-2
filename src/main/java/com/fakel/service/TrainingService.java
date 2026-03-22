package com.fakel.service;

import com.fakel.dto.*;
import com.fakel.model.*;
import com.fakel.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

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

    @Transactional(readOnly = true)
    public Page<TrainingDto> getTrainings(UserDetails userDetails,
                                          LocalDate date,
                                          LocalDate dateFrom,
                                          LocalDate dateTo,
                                          String type,
                                          Pageable pageable) {

        log.info("Получение тренировок курсанта: user={}, date={}, dateFrom={}, dateTo={}, type={}, page={}, size={}",
                userDetails != null ? userDetails.getUsername() : null, date, dateFrom, dateTo, type,
                pageable.getPageNumber(), pageable.getPageSize());

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения тренировок с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (pageable == null) {
            log.warn("Параметры пагинации null");
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            log.warn("Некорректный диапазон дат: dateFrom={} > dateTo={}", dateFrom, dateTo);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        if (type != null && !isValidTrainingType(type)) {
            log.warn("Некорректный тип тренировки: {}", type);
            throw new IllegalArgumentException("Некорректный тип тренировки: " + type);
        }

        Long cadetId = getCadetId(userDetails);
        log.debug("ID курсанта: {}", cadetId);

        Page<Training> trainings;

        try {
            if (type != null && dateFrom != null && dateTo != null) {
                log.debug("Поиск по типу {} и датам {} - {}", type, dateFrom, dateTo);
                trainings = trainingRepository.findByCadetIdAndTypeAndDateBetween(
                        cadetId, type, dateFrom, dateTo, pageable);
            } else if (type != null) {
                log.debug("Поиск по типу {}", type);
                trainings = trainingRepository.findByCadetIdAndType(cadetId, type, pageable);
            } else if (date != null) {
                log.debug("Поиск по дате {}", date);
                trainings = trainingRepository.findByCadetIdAndDate(cadetId, date, pageable);
            } else if (dateFrom != null && dateTo != null) {
                log.debug("Поиск по датам {} - {}", dateFrom, dateTo);
                trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, dateFrom, dateTo, pageable);
            } else {
                log.debug("Поиск всех тренировок");
                trainings = trainingRepository.findByCadetId(cadetId, pageable);
            }
        } catch (Exception e) {
            log.error("Ошибка при получении тренировок: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении тренировок: " + e.getMessage());
        }

        log.info("Найдено {} тренировок для курсанта {}", trainings.getTotalElements(), cadetId);
        log.debug("Всего страниц: {}", trainings.getTotalPages());

        return trainings.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public TrainingDto getTrainingById(UserDetails userDetails, Long trainingId) {

        log.info("Получение тренировки по ID: {}, пользователь: {}", trainingId,
                userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения тренировки с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (trainingId == null || trainingId <= 0) {
            log.warn("Некорректный ID тренировки: {}", trainingId);
            throw new IllegalArgumentException("ID тренировки должен быть положительным числом");
        }

        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> {
                    log.warn("Тренировка не найдена с id: {}", trainingId);
                    return new RuntimeException("Тренировка не найдена с id: " + trainingId);
                });

        checkAccess(userDetails, training);
        log.debug("Тренировка найдена: дата={}, тип={}, вес={}",
                training.getDate(), training.getType(), training.getCurrentWeight());

        return convertToDto(training);
    }

    @Transactional
    public void updateTraining(UserDetails userDetails, Long trainingId, UpdateTrainingRequest request) {

        log.info("Обновление тренировки: trainingId={}, user={}", trainingId,
                userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка обновления тренировки с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (trainingId == null || trainingId <= 0) {
            log.warn("Некорректный ID тренировки: {}", trainingId);
            throw new IllegalArgumentException("ID тренировки должен быть положительным числом");
        }

        if (request == null) {
            log.warn("Попытка обновления тренировки с null request");
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }

        Training training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> {
                    log.warn("Тренировка не найдена с id: {}", trainingId);
                    return new RuntimeException("Тренировка не найдена с id: " + trainingId);
                });

        checkAccess(userDetails, training);
        log.debug("Текущие данные тренировки: дата={}, тип={}, вес={}, отдых={}",
                training.getDate(), training.getType(), training.getCurrentWeight(), training.getRestPeriod());

        if (request.getDate() != null) {
            log.debug("Обновление даты: {} -> {}", training.getDate(), request.getDate());
            if (request.getDate().isAfter(LocalDate.now())) {
                log.warn("Попытка установить дату в будущем: {}", request.getDate());
                throw new IllegalArgumentException("Дата тренировки не может быть в будущем");
            }
            training.setDate(request.getDate());
        }

        if (request.getCurrentWeight() != null) {
            log.debug("Обновление веса: {} -> {}", training.getCurrentWeight(), request.getCurrentWeight());
            if (request.getCurrentWeight().compareTo(BigDecimal.valueOf(30)) < 0 ||
                    request.getCurrentWeight().compareTo(BigDecimal.valueOf(180)) > 0) {
                log.warn("Некорректный вес: {}", request.getCurrentWeight());
                throw new IllegalArgumentException("Вес должен быть от 30 до 180 кг");
            }
            training.setCurrentWeight(request.getCurrentWeight());
        }

        if (request.getRestPeriod() != null) {
            log.debug("Обновление периода отдыха: {} -> {}", training.getRestPeriod(), request.getRestPeriod());
            if (request.getRestPeriod() < 0 || request.getRestPeriod() > 3600) {
                log.warn("Некорректный период отдыха: {}", request.getRestPeriod());
                throw new IllegalArgumentException("Период отдыха должен быть от 0 до 3600 секунд");
            }
            training.setRestPeriod(request.getRestPeriod().shortValue());
        }

        if (request.getExercises() != null && !request.getExercises().isEmpty()) {
            log.debug("Обновление {} упражнений", request.getExercises().size());
            int exerciseCounter = 0;

            for (UpdateTrainingRequest.UpdateExerciseRequest exerciseReq : request.getExercises()) {
                exerciseCounter++;

                if (exerciseReq.getExerciseInTrainingId() == null || exerciseReq.getExerciseInTrainingId() <= 0) {
                    log.warn("Некорректный ID упражнения в запросе #{}", exerciseCounter);
                    throw new IllegalArgumentException("ID упражнения в тренировке должен быть положительным числом");
                }

                ExercisesInTraining exercise = exercisesInTrainingRepository.findById(exerciseReq.getExerciseInTrainingId())
                        .orElseThrow(() -> {
                            log.warn("Упражнение в тренировке не найдено с id: {}", exerciseReq.getExerciseInTrainingId());
                            return new RuntimeException("Упражнение в тренировке не найдено с id: " +
                                    exerciseReq.getExerciseInTrainingId());
                        });

                if (!exercise.getTraining().getId().equals(trainingId)) {
                    log.warn("Упражнение {} не принадлежит тренировке {}", exerciseReq.getExerciseInTrainingId(), trainingId);
                    throw new RuntimeException("Упражнение не принадлежит этой тренировке");
                }

                if (exerciseReq.getRestPeriod() != null) {
                    log.debug("Упражнение {}: обновление отдыха -> {}", exercise.getId(), exerciseReq.getRestPeriod());
                    if (exerciseReq.getRestPeriod() < 0 || exerciseReq.getRestPeriod() > 3600) {
                        log.warn("Некорректный период отдыха для упражнения: {}", exerciseReq.getRestPeriod());
                        throw new IllegalArgumentException("Период отдыха между подходами должен быть от 0 до 3600 секунд");
                    }
                    exercise.setRestPeriod(exerciseReq.getRestPeriod().shortValue());
                }

                if (exerciseReq.getApproaches() != null && !exerciseReq.getApproaches().isEmpty()) {
                    log.debug("Упражнение {}: обновление {} подходов", exercise.getId(), exerciseReq.getApproaches().size());
                    int approachCounter = 0;

                    for (UpdateTrainingRequest.UpdateApproachRequest approachReq : exerciseReq.getApproaches()) {
                        approachCounter++;

                        if (approachReq.getApproachId() == null || approachReq.getApproachId() <= 0) {
                            log.warn("Некорректный ID подхода в упражнении #{}", approachCounter);
                            throw new IllegalArgumentException("ID подхода должен быть положительным числом");
                        }

                        Approach approach = approachRepository.findById(approachReq.getApproachId())
                                .orElseThrow(() -> {
                                    log.warn("Подход не найден с id: {}", approachReq.getApproachId());
                                    return new RuntimeException("Подход не найден с id: " +
                                            approachReq.getApproachId());
                                });

                        if (!approach.getExerciseInTraining().getId().equals(exercise.getId())) {
                            log.warn("Подход {} не принадлежит упражнению {}", approachReq.getApproachId(), exercise.getId());
                            throw new RuntimeException("Подход не принадлежит этому упражнению");
                        }

                        if (approachReq.getValue() != null) {
                            log.debug("Подход {}: обновление значения: {} -> {}",
                                    approach.getId(), approach.getValue(), approachReq.getValue());
                            if (approachReq.getValue().compareTo(BigDecimal.ZERO) < 0) {
                                log.warn("Отрицательное значение подхода: {}", approachReq.getValue());
                                throw new IllegalArgumentException("Значение подхода не может быть отрицательным");
                            }
                            approach.setValue(approachReq.getValue());
                        }
                        approachRepository.save(approach);
                        log.trace("Подход {} сохранен", approach.getId());
                    }
                }
                exercisesInTrainingRepository.save(exercise);
                log.trace("Упражнение {} сохранено", exercise.getId());
            }
        }

        trainingRepository.save(training);
        log.info("Тренировка {} успешно обновлена", trainingId);
    }

    @Transactional
    public void deleteTraining(UserDetails userDetails, Long trainingId) {

        log.info("Удаление тренировки: trainingId={}, user={}", trainingId,
                userDetails != null ? userDetails.getUsername() : null);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка удаления тренировки с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (trainingId == null || trainingId <= 0) {
            log.warn("Некорректный ID тренировки: {}", trainingId);
            throw new IllegalArgumentException("ID тренировки должен быть положительным числом");
        }

        if (userDetails.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            log.warn("Попытка удаления тренировки пользователем без роли CADET");
            throw new RuntimeException("Только курсант может удалять свои тренировки");
        }

        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        if (!trainingRepository.existsByIdAndCadetId(trainingId, cadet.getUserId())) {
            log.warn("Тренировка {} не найдена или не принадлежит курсанту {}", trainingId, cadet.getUserId());
            throw new RuntimeException("Тренировка не найдена или не принадлежит курсанту");
        }

        try {
            trainingRepository.deleteById(trainingId);
            log.info("Тренировка {} успешно удалена", trainingId);
        } catch (Exception e) {
            log.error("Ошибка при удалении тренировки {}: {}", trainingId, e.getMessage(), e);
            throw new RuntimeException("Ошибка при удалении тренировки: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ExerciseCatalogDto> getExercisesByType(String type) {

        log.info("Получение упражнений по типу: {}", type);

        if (type == null) {
            log.warn("Тип упражнения null");
            throw new IllegalArgumentException("Тип упражнения не может быть null");
        }

        if (!isValidTrainingType(type)) {
            log.warn("Некорректный тип упражнения: {}", type);
            throw new IllegalArgumentException("Некорректный тип упражнения: " + type);
        }

        List<ExerciseCatalog> exercises;
        try {
            exercises = exerciseCatalogRepository.findByType(type);
            log.debug("Получено {} упражнений типа {}", exercises != null ? exercises.size() : 0, type);
        } catch (Exception e) {
            log.error("Ошибка при получении упражнений: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении упражнений: " + e.getMessage());
        }

        if (exercises == null || exercises.isEmpty()) {
            log.debug("Упражнения типа {} не найдены", type);
            return new ArrayList<>();
        }

        List<ExerciseCatalogDto> result = exercises.stream()
                .filter(e -> e != null)
                .map(this::convertExerciseToDto)
                .collect(Collectors.toList());

        log.info("Возвращаем {} упражнений типа {}", result.size(), type);
        return result;
    }

    @Transactional(readOnly = true)
    public ExerciseCatalogDto getExerciseWithDefaults(String code) {

        log.info("Получение упражнения с параметрами по коду: {}", code);

        if (code == null || code.trim().isEmpty()) {
            log.warn("Код упражнения пустой");
            throw new IllegalArgumentException("Код упражнения не может быть пустым");
        }

        String trimmedCode = code.trim();
        log.debug("Код после trim: '{}'", trimmedCode);

        ExerciseCatalog exercise;
        try {
            exercise = exerciseCatalogRepository.findByCodeWithParameters(trimmedCode)
                    .orElseThrow(() -> {
                        log.warn("Упражнение не найдено с кодом: {}", trimmedCode);
                        return new RuntimeException("Упражнение не найдено с кодом: " + code);
                    });
            log.debug("Упражнение найдено: id={}, описание={}", exercise.getId(), exercise.getDescription());
        } catch (Exception e) {
            log.error("Ошибка при получении упражнения: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении упражнения: " + e.getMessage());
        }

        return convertExerciseToDto(exercise);
    }

    @Transactional(readOnly = true)
    public Page<TrainingDto> getTrainingsByCadetId(UserDetails userDetails, Long cadetId,
                                                   LocalDate date, LocalDate dateFrom,
                                                   LocalDate dateTo, String type,
                                                   Pageable pageable) {

        log.info("Получение тренировок курсанта преподавателем: cadetId={}, user={}, date={}, dateFrom={}, dateTo={}, type={}",
                cadetId, userDetails != null ? userDetails.getUsername() : null, date, dateFrom, dateTo, type);

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения тренировок с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (cadetId == null || cadetId <= 0) {
            log.warn("Некорректный ID курсанта: {}", cadetId);
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }

        if (pageable == null) {
            log.warn("Параметры пагинации null");
            throw new IllegalArgumentException("Параметры пагинации не могут быть пустыми");
        }

        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            log.warn("Некорректный диапазон дат: dateFrom={} > dateTo={}", dateFrom, dateTo);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        if (type != null && !isValidTrainingType(type)) {
            log.warn("Некорректный тип тренировки: {}", type);
            throw new IllegalArgumentException("Некорректный тип тренировки: " + type);
        }

        Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Преподаватель не найден");
                });

        Cadet cadet = cadetRepository.findById(cadetId)
                .orElseThrow(() -> {
                    log.warn("Курсант не найден с id: {}", cadetId);
                    return new RuntimeException("Курсант не найден с id: " + cadetId);
                });

        if (cadet.getGroupId() == null) {
            log.warn("У курсанта {} не указана группа", cadetId);
            throw new RuntimeException("У курсанта не указана группа");
        }

        Group group = groupRepository.findById(cadet.getGroupId())
                .orElseThrow(() -> {
                    log.warn("Группа не найдена: {}", cadet.getGroupId());
                    return new RuntimeException("Группа не найдена");
                });

        if (teacher.getUniversity() == null) {
            log.warn("У преподавателя {} не указан университет", teacher.getUserId());
            throw new RuntimeException("У преподавателя не указан университет");
        }

        if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
            log.warn("Нет доступа к тренировкам курсанта {} для преподавателя {} (университеты: {} vs {})",
                    cadetId, teacher.getUserId(), group.getUniversity().getId(), teacher.getUniversity().getId());
            throw new RuntimeException("Нет доступа к тренировкам этого курсанта");
        }

        log.debug("Доступ разрешен, получение тренировок курсанта {}", cadetId);

        Page<Training> trainings;
        try {
            if (type != null && dateFrom != null && dateTo != null) {
                log.debug("Поиск по типу {} и датам {} - {}", type, dateFrom, dateTo);
                trainings = trainingRepository.findByCadetIdAndTypeAndDateBetween(
                        cadetId, type, dateFrom, dateTo, pageable);
            } else if (type != null) {
                log.debug("Поиск по типу {}", type);
                trainings = trainingRepository.findByCadetIdAndType(cadetId, type, pageable);
            } else if (date != null) {
                log.debug("Поиск по дате {}", date);
                trainings = trainingRepository.findByCadetIdAndDate(cadetId, date, pageable);
            } else if (dateFrom != null && dateTo != null) {
                log.debug("Поиск по датам {} - {}", dateFrom, dateTo);
                trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, dateFrom, dateTo, pageable);
            } else {
                log.debug("Поиск всех тренировок");
                trainings = trainingRepository.findByCadetId(cadetId, pageable);
            }
        } catch (Exception e) {
            log.error("Ошибка при получении тренировок: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении тренировок: " + e.getMessage());
        }

        log.info("Найдено {} тренировок для курсанта {}", trainings.getTotalElements(), cadetId);
        log.debug("Всего страниц: {}", trainings.getTotalPages());

        return trainings.map(this::convertToDto);
    }

    private Long getCadetId(UserDetails userDetails) {
        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Попытка получения ID курсанта с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        log.debug("Поиск курсанта по логину: {}", userDetails.getUsername());
        Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.warn("Курсант не найден: {}", userDetails.getUsername());
                    return new RuntimeException("Курсант не найден");
                });

        if (cadet.getUserId() == null) {
            log.warn("У курсанта {} отсутствует ID", userDetails.getUsername());
            throw new RuntimeException("У курсанта отсутствует ID");
        }

        log.debug("Найден курсант с ID: {}", cadet.getUserId());
        return cadet.getUserId();
    }

    private void checkAccess(UserDetails userDetails, Training training) {
        log.debug("Проверка доступа к тренировке {} для пользователя {}", training.getId(), userDetails.getUsername());

        if (userDetails == null || userDetails.getUsername() == null) {
            log.warn("Проверка доступа с null userDetails");
            throw new IllegalArgumentException("Данные пользователя не могут быть пустыми");
        }

        if (training == null) {
            log.warn("Проверка доступа с null тренировкой");
            throw new IllegalArgumentException("Тренировка не может быть null");
        }

        if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CADET"))) {
            log.debug("Проверка доступа для курсанта");
            Cadet cadet = cadetRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> {
                        log.warn("Курсант не найден: {}", userDetails.getUsername());
                        return new RuntimeException("Курсант не найден");
                    });

            if (cadet.getUserId() == null) {
                log.warn("У курсанта {} отсутствует ID", userDetails.getUsername());
                throw new RuntimeException("У курсанта отсутствует ID");
            }

            if (!training.getCadetId().equals(cadet.getUserId())) {
                log.warn("Курсант {} пытается получить доступ к чужой тренировке {}", cadet.getUserId(), training.getId());
                throw new RuntimeException("Нет доступа к этой тренировке");
            }
            log.debug("Доступ курсанта разрешен");
        } else {
            log.debug("Проверка доступа для преподавателя");
            Teacher teacher = teacherRepository.findByUserLogin(userDetails.getUsername())
                    .orElseThrow(() -> {
                        log.warn("Преподаватель не найден: {}", userDetails.getUsername());
                        return new RuntimeException("Преподаватель не найден");
                    });

            if (teacher.getUniversity() == null) {
                log.warn("У преподавателя {} не указан университет", teacher.getUserId());
                throw new RuntimeException("У преподавателя не указан университет");
            }

            Cadet cadet = cadetRepository.findById(training.getCadetId())
                    .orElseThrow(() -> {
                        log.warn("Курсант не найден: {}", training.getCadetId());
                        return new RuntimeException("Курсант не найден");
                    });

            if (cadet.getGroupId() == null) {
                log.warn("У курсанта {} не указана группа", cadet.getUserId());
                throw new RuntimeException("У курсанта не указана группа");
            }

            Group group = groupRepository.findById(cadet.getGroupId())
                    .orElseThrow(() -> {
                        log.warn("Группа не найдена: {}", cadet.getGroupId());
                        return new RuntimeException("Группа не найдена");
                    });

            if (!group.getUniversity().getId().equals(teacher.getUniversity().getId())) {
                log.warn("Преподаватель {} пытается получить доступ к курсанту из другого университета",
                        teacher.getUserId());
                throw new RuntimeException("Нет доступа к тренировкам этого курсанта");
            }
            log.debug("Доступ преподавателя разрешен");
        }
    }

    private boolean isValidTrainingType(String type) {
        if (type == null) return false;
        List<String> validTypes = List.of("Сила", "Скорость", "Выносливость");
        boolean isValid = validTypes.contains(type);
        log.trace("Проверка типа тренировки '{}': {}", type, isValid);
        return isValid;
    }

    private TrainingDto convertToDto(Training training) {
        if (training == null) {
            log.trace("Конвертация null тренировки в null DTO");
            return null;
        }

        log.trace("Конвертация тренировки {} в DTO", training.getId());

        List<ExerciseInTrainingDto> exerciseDtos = new ArrayList<>();
        if (training.getExercises() != null) {
            exerciseDtos = training.getExercises().stream()
                    .filter(e -> e != null)
                    .map(this::convertExerciseInTrainingToDto)
                    .collect(Collectors.toList());
            log.trace("Добавлено {} упражнений", exerciseDtos.size());
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
        if (exercise == null) {
            log.trace("Конвертация null упражнения в null DTO");
            return null;
        }

        log.trace("Конвертация упражнения {} в DTO", exercise.getId());

        List<ApproachDto> approachDtos = new ArrayList<>();
        if (exercise.getApproaches() != null) {
            approachDtos = exercise.getApproaches().stream()
                    .filter(a -> a != null)
                    .map(this::convertApproachToDto)
                    .collect(Collectors.toList());
            log.trace("Добавлено {} подходов", approachDtos.size());
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
        if (approach == null) {
            log.trace("Конвертация null подхода в null DTO");
            return null;
        }

        log.trace("Конвертация подхода {} в DTO", approach.getId());

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
        if (exercise == null) {
            log.trace("Конвертация null каталога упражнений в null DTO");
            return null;
        }

        log.trace("Конвертация каталога упражнений {} в DTO", exercise.getId());

        List<ExerciseParameterDto> paramDtos = new ArrayList<>();
        if (exercise.getParameters() != null) {
            paramDtos = exercise.getParameters().stream()
                    .filter(p -> p != null)
                    .map(this::convertParameterToDto)
                    .collect(Collectors.toList());
            log.trace("Добавлено {} параметров", paramDtos.size());
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
        if (param == null) {
            log.trace("Конвертация null параметра в null DTO");
            return null;
        }

        log.trace("Конвертация параметра {} в DTO", param.getId());

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