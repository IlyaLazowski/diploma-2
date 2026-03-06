package com.fakel.service;

import com.fakel.model.Approach;
import com.fakel.model.ExercisesInTraining;
import com.fakel.model.Training;
import com.fakel.repository.TrainingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TrainingAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(TrainingAnalyticsService.class);

    private final TrainingRepository trainingRepository;

    @Autowired
    public TrainingAnalyticsService(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    public Map<LocalDate, Double> getWeightProgress(Long cadetId, LocalDate from, LocalDate to) {
        log.info("Получение прогресса веса: cadetId={}, from={}, to={}", cadetId, from, to);

        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            log.warn("Некорректный ID курсанта: {}", cadetId);
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            log.warn("Дата начала null");
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            log.warn("Дата окончания null");
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            log.warn("Некорректный диапазон дат: from={} after to={}", from, to);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Object[]> rows;
        try {
            rows = trainingRepository.getWeightProgress(cadetId, from, to);
            log.debug("Получено {} строк из репозитория", rows != null ? rows.size() : 0);
        } catch (Exception e) {
            log.error("Ошибка при получении данных о весе: {}", e.getMessage(), e);
            return new LinkedHashMap<>();
        }

        if (rows == null || rows.isEmpty()) {
            log.debug("Нет данных о весе за указанный период");
            return new LinkedHashMap<>();
        }

        Map<LocalDate, Double> result = new LinkedHashMap<>();
        int validRows = 0;

        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                log.trace("Пропуск некорректной строки: {}", Arrays.toString(row));
                continue;
            }

            LocalDate date = extractDate(row[0]);
            Double weight = extractDouble(row[1]);

            if (date != null && weight != null && weight > 0) {
                result.put(date, weight);
                validRows++;
                log.trace("Добавлена запись: дата={}, вес={}", date, weight);
            }
        }

        log.info("Найдено {} записей о весе", result.size());
        log.debug("Успешно обработано {} строк из {}", validRows, rows.size());
        return result;
    }

    public Map<String, Map<String, Map<LocalDate, Double>>> getExercisesProgress(
            Long cadetId, LocalDate from, LocalDate to, List<String> types) {

        log.info("Получение прогресса по упражнениям: cadetId={}, from={}, to={}, types={}",
                cadetId, from, to, types);

        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            log.warn("Некорректный ID курсанта: {}", cadetId);
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            log.warn("Дата начала null");
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            log.warn("Дата окончания null");
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            log.warn("Некорректный диапазон дат: from={} after to={}", from, to);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Training> trainings;
        try {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
            log.debug("Получено {} тренировок из репозитория", trainings != null ? trainings.size() : 0);
        } catch (Exception e) {
            log.error("Ошибка при получении тренировок: {}", e.getMessage(), e);
            return new HashMap<>();
        }

        if (trainings == null || trainings.isEmpty()) {
            log.debug("Нет тренировок за указанный период");
            return new HashMap<>();
        }

        // Фильтрация по типам
        if (types != null && !types.isEmpty()) {
            log.debug("Фильтрация по типам: {}", types);
            List<String> validTypes = List.of("Сила", "Скорость", "Выносливость");
            for (String type : types) {
                if (type != null && !validTypes.contains(type)) {
                    log.warn("Некорректный тип тренировки: {}", type);
                    throw new IllegalArgumentException("Некорректный тип тренировки: " + type);
                }
            }

            int beforeFilter = trainings.size();
            trainings = trainings.stream()
                    .filter(t -> t != null && t.getType() != null && types.contains(t.getType()))
                    .collect(Collectors.toList());
            log.debug("После фильтрации по типам: {} тренировок из {}", trainings.size(), beforeFilter);
        }

        Map<String, Map<String, Map<LocalDate, Double>>> result = new HashMap<>();
        int[] processedTrainings = {0};
        int[] processedExercises = {0};
        int[] processedApproaches = {0};

        for (Training t : trainings) {
            if (t == null || t.getDate() == null) {
                log.trace("Пропуск тренировки с null датой");
                continue;
            }
            processedTrainings[0]++;

            if (t.getExercises() == null || t.getExercises().isEmpty()) {
                log.trace("Тренировка {} не содержит упражнений", t.getId());
                continue;
            }

            for (ExercisesInTraining eit : t.getExercises()) {
                if (eit == null || eit.getExerciseCatalog() == null) {
                    log.trace("Пропуск некорректного упражнения");
                    continue;
                }
                processedExercises[0]++;

                String exerciseName = eit.getExerciseCatalog().getDescription();
                if (exerciseName == null || exerciseName.trim().isEmpty()) {
                    exerciseName = "Упражнение";
                }
                String finalExerciseName = exerciseName.trim(); // ← делаем effectively final

                if (eit.getApproaches() == null || eit.getApproaches().isEmpty()) {
                    log.trace("Упражнение {} не содержит подходов", eit.getId());
                    continue;
                }

                for (Approach a : eit.getApproaches()) {
                    if (a == null || a.getExerciseParameter() == null || a.getValue() == null) {
                        log.trace("Пропуск некорректного подхода");
                        continue;
                    }
                    processedApproaches[0]++;

                    String paramCode = a.getExerciseParameter().getCode();
                    String paramName = getParameterDisplayName(paramCode);
                    String finalParamName = paramName; // ← делаем effectively final

                    double value;
                    try {
                        value = a.getValue().doubleValue();
                    } catch (Exception e) {
                        log.trace("Ошибка конвертации значения подхода: {}", e.getMessage());
                        continue;
                    }

                    result.computeIfAbsent(finalExerciseName, k -> {
                        log.trace("Создание записи для упражнения '{}'", finalExerciseName);
                        return new HashMap<>();
                    }).computeIfAbsent(finalParamName, k -> {
                        log.trace("Создание записи для параметра '{}'", finalParamName);
                        return new HashMap<>();
                    }).put(t.getDate(), value);

                    log.trace("Добавлена точка: упражнение='{}', параметр='{}', дата={}, значение={}",
                            finalExerciseName, finalParamName, t.getDate(), value);
                }
            }
        }

        log.info("Обработано тренировок: {}, упражнений: {}, подходов: {}",
                processedTrainings[0], processedExercises[0], processedApproaches[0]);
        log.debug("Найдено {} упражнений в результате", result.size());

        return result;
    }

    public Map<String, Map<LocalDate, Double>> getTonnageProgress(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        log.info("Получение прогресса тоннажа: cadetId={}, from={}, to={}, types={}", cadetId, from, to, types);

        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            log.warn("Некорректный ID курсанта: {}", cadetId);
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            log.warn("Дата начала null");
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            log.warn("Дата окончания null");
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            log.warn("Некорректный диапазон дат: from={} after to={}", from, to);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Training> trainings;
        try {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
            log.debug("Получено {} тренировок из репозитория", trainings != null ? trainings.size() : 0);
        } catch (Exception e) {
            log.error("Ошибка при получении тренировок: {}", e.getMessage(), e);
            return new HashMap<>();
        }

        if (trainings == null || trainings.isEmpty()) {
            log.debug("Нет тренировок за указанный период");
            return new HashMap<>();
        }

        if (types != null && !types.isEmpty()) {
            int beforeFilter = trainings.size();
            trainings = trainings.stream()
                    .filter(t -> t != null && t.getType() != null && types.contains(t.getType()))
                    .collect(Collectors.toList());
            log.debug("После фильтрации по типам: {} тренировок из {}", trainings.size(), beforeFilter);
        }

        Map<String, Map<LocalDate, Double>> result = new HashMap<>();
        int processedTrainings = 0;
        int processedExercises = 0;

        for (Training t : trainings) {
            if (t == null || t.getDate() == null) {
                continue;
            }
            processedTrainings++;

            if (t.getExercises() == null) {
                continue;
            }

            for (ExercisesInTraining eit : t.getExercises()) {
                if (eit == null || eit.getExerciseCatalog() == null) {
                    continue;
                }
                processedExercises++;

                String exerciseName = eit.getExerciseCatalog().getDescription();
                if (exerciseName == null) {
                    exerciseName = "Упражнение";
                }
                exerciseName = exerciseName.trim();

                Double weight = null;
                Integer reps = null;

                if (eit.getApproaches() == null) {
                    continue;
                }

                for (Approach a : eit.getApproaches()) {
                    if (a == null || a.getExerciseParameter() == null || a.getValue() == null) {
                        continue;
                    }

                    String paramCode = a.getExerciseParameter().getCode();
                    if (paramCode == null) continue;

                    if ("вес".equalsIgnoreCase(paramCode)) {
                        try {
                            weight = a.getValue().doubleValue();
                            log.trace("Найден вес {} для упражнения {}", weight, exerciseName);
                        } catch (Exception e) {
                            log.trace("Ошибка конвертации веса: {}", e.getMessage());
                        }
                    } else if ("повторения".equalsIgnoreCase(paramCode)) {
                        try {
                            reps = a.getValue().intValue();
                            log.trace("Найдены повторения {} для упражнения {}", reps, exerciseName);
                        } catch (Exception e) {
                            log.trace("Ошибка конвертации повторений: {}", e.getMessage());
                        }
                    }
                }

                if (weight != null && reps != null && weight > 0 && reps > 0) {
                    double tonnage = weight * reps;
                    result.computeIfAbsent(exerciseName, k -> new HashMap<>())
                            .put(t.getDate(), tonnage);
                    log.trace("Добавлен тоннаж {} кг для упражнения {} на дату {}",
                            tonnage, exerciseName, t.getDate());
                }
            }
        }

        log.info("Обработано тренировок: {}, упражнений: {}", processedTrainings, processedExercises);
        log.debug("Найдено {} упражнений с данными о тоннаже", result.size());

        return result;
    }

    public Map<LocalDate, Integer> getVolumeProgress(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        log.info("Получение прогресса объема: cadetId={}, from={}, to={}, types={}", cadetId, from, to, types);

        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            log.warn("Некорректный ID курсанта: {}", cadetId);
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            log.warn("Дата начала null");
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            log.warn("Дата окончания null");
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            log.warn("Некорректный диапазон дат: from={} after to={}", from, to);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Training> trainings;
        try {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
            log.debug("Получено {} тренировок из репозитория", trainings != null ? trainings.size() : 0);
        } catch (Exception e) {
            log.error("Ошибка при получении тренировок: {}", e.getMessage(), e);
            return new LinkedHashMap<>();
        }

        if (trainings == null || trainings.isEmpty()) {
            log.debug("Нет тренировок за указанный период");
            return new LinkedHashMap<>();
        }

        if (types != null && !types.isEmpty()) {
            int beforeFilter = trainings.size();
            trainings = trainings.stream()
                    .filter(t -> t != null && t.getType() != null && types.contains(t.getType()))
                    .collect(Collectors.toList());
            log.debug("После фильтрации по типам: {} тренировок из {}", trainings.size(), beforeFilter);
        }

        Map<LocalDate, Integer> result = new LinkedHashMap<>();
        int processedTrainings = 0;

        for (Training t : trainings) {
            if (t == null || t.getDate() == null) {
                continue;
            }
            processedTrainings++;

            if (t.getExercises() == null) {
                result.put(t.getDate(), 0);
                log.trace("Тренировка {}: объем = 0 (нет упражнений)", t.getId());
                continue;
            }

            int volume = t.getExercises().stream()
                    .filter(eit -> eit != null && eit.getApproaches() != null)
                    .mapToInt(eit -> eit.getApproaches().size())
                    .sum();
            result.put(t.getDate(), volume);
            log.trace("Тренировка {}: объем = {}", t.getId(), volume);
        }

        log.info("Обработано тренировок: {}, получено {} записей об объеме", processedTrainings, result.size());
        return result;
    }

    public Map<String, Object> getSummary(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        log.info("Получение сводной статистики: cadetId={}, from={}, to={}, types={}", cadetId, from, to, types);

        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            log.warn("Некорректный ID курсанта: {}", cadetId);
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            log.warn("Дата начала null");
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            log.warn("Дата окончания null");
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            log.warn("Некорректный диапазон дат: from={} after to={}", from, to);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Training> trainings;
        try {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
            log.debug("Получено {} тренировок из репозитория", trainings != null ? trainings.size() : 0);
        } catch (Exception e) {
            log.error("Ошибка при получении тренировок: {}", e.getMessage(), e);
            trainings = new ArrayList<>();
        }

        if (trainings == null) {
            trainings = new ArrayList<>();
        }

        int beforeFilter = trainings.size();

        if (types != null && !types.isEmpty()) {
            trainings = trainings.stream()
                    .filter(t -> t != null && t.getType() != null && types.contains(t.getType()))
                    .collect(Collectors.toList());
            log.debug("После фильтрации по типам: {} тренировок из {}", trainings.size(), beforeFilter);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTrainings", trainings.size());

        long daysBetween = ChronoUnit.DAYS.between(from, to) + 1;
        summary.put("totalDays", daysBetween);

        Map<String, Long> byType = new HashMap<>();
        double totalTonnage = 0;
        int totalApproaches = 0;
        int processedTrainings = 0;

        for (Training t : trainings) {
            if (t == null) continue;
            processedTrainings++;

            String type = t.getType();
            if (type != null) {
                byType.put(type, byType.getOrDefault(type, 0L) + 1);
                log.trace("Тренировка {} типа {}", t.getId(), type);
            }

            if (t.getExercises() != null) {
                for (ExercisesInTraining eit : t.getExercises()) {
                    if (eit == null) continue;

                    if (eit.getApproaches() != null) {
                        totalApproaches += eit.getApproaches().size();

                        for (Approach a : eit.getApproaches()) {
                            if (a == null || a.getExerciseParameter() == null || a.getValue() == null) {
                                continue;
                            }

                            String paramCode = a.getExerciseParameter().getCode();
                            if (paramCode != null && "вес".equalsIgnoreCase(paramCode)) {
                                try {
                                    totalTonnage += a.getValue().doubleValue();
                                } catch (Exception e) {
                                    log.trace("Ошибка при подсчете тоннажа: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }

        summary.put("byType", byType);
        summary.put("totalTonnage", BigDecimal.valueOf(totalTonnage)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue());
        summary.put("totalApproaches", totalApproaches);

        if (!trainings.isEmpty() && daysBetween > 0) {
            double avgTrainingsPerWeek = (trainings.size() * 7.0) / daysBetween;
            summary.put("avgTrainingsPerWeek", BigDecimal.valueOf(avgTrainingsPerWeek)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue());
            log.debug("Среднее количество тренировок в неделю: {}", avgTrainingsPerWeek);
        }

        log.info("Сводная статистика: тренировок={}, дней={}, тоннаж={}, подходов={}",
                trainings.size(), daysBetween,
                summary.get("totalTonnage"), totalApproaches);

        return summary;
    }

    private String getParameterDisplayName(String code) {
        if (code == null) {
            log.trace("Параметр: code=null -> 'Значение'");
            return "Значение";
        }

        String displayName;
        switch (code.toLowerCase()) {
            case "повторения":
                displayName = "Повторения";
                break;
            case "вес":
                displayName = "Вес (кг)";
                break;
            case "время":
                displayName = "Время (сек)";
                break;
            case "длительность":
                displayName = "Длительность (мин)";
                break;
            case "дистанция":
                displayName = "Дистанция (м)";
                break;
            default:
                displayName = code;
        }

        log.trace("Параметр: code='{}' -> displayName='{}'", code, displayName);
        return displayName;
    }

    private LocalDate extractDate(Object obj) {
        if (obj == null) {
            log.trace("extractDate: объект null");
            return null;
        }

        try {
            LocalDate result = null;
            if (obj instanceof Date) {
                result = ((Date) obj).toLocalDate();
            } else if (obj instanceof java.util.Date) {
                result = ((java.util.Date) obj).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } else if (obj instanceof LocalDate) {
                result = (LocalDate) obj;
            }

            log.trace("extractDate: {} -> {}", obj.getClass().getSimpleName(), result);
            return result;
        } catch (Exception e) {
            log.trace("extractDate: ошибка конвертации {}: {}", obj.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    private Double extractDouble(Object obj) {
        if (obj == null) {
            log.trace("extractDouble: объект null");
            return null;
        }

        try {
            Double result = null;
            if (obj instanceof BigDecimal) {
                result = ((BigDecimal) obj).setScale(1, RoundingMode.HALF_UP).doubleValue();
            } else if (obj instanceof Double) {
                result = BigDecimal.valueOf((Double) obj)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
            } else if (obj instanceof Integer) {
                result = ((Integer) obj).doubleValue();
            } else if (obj instanceof Long) {
                result = ((Long) obj).doubleValue();
            } else if (obj instanceof Float) {
                result = ((Float) obj).doubleValue();
            }

            log.trace("extractDouble: {} -> {}", obj.getClass().getSimpleName(), result);
            return result;
        } catch (Exception e) {
            log.trace("extractDouble: ошибка конвертации {}: {}", obj.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
}