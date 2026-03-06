package com.fakel.service;

import com.fakel.model.Approach;
import com.fakel.model.ExercisesInTraining;
import com.fakel.model.Training;
import com.fakel.repository.TrainingRepository;
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

    private final TrainingRepository trainingRepository;

    @Autowired
    public TrainingAnalyticsService(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    public Map<LocalDate, Double> getWeightProgress(Long cadetId, LocalDate from, LocalDate to) {
        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Object[]> rows;
        try {
            rows = trainingRepository.getWeightProgress(cadetId, from, to);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }

        if (rows == null) {
            return new LinkedHashMap<>();
        }

        Map<LocalDate, Double> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }

            LocalDate date = extractDate(row[0]);
            Double weight = extractDouble(row[1]);

            if (date != null && weight != null && weight > 0) {
                result.put(date, weight);
            }
        }

        return result;
    }

    public Map<String, Map<String, Map<LocalDate, Double>>> getExercisesProgress(
            Long cadetId, LocalDate from, LocalDate to, List<String> types) {

        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Training> trainings;
        try {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
        } catch (Exception e) {
            return new HashMap<>();
        }

        if (trainings == null || trainings.isEmpty()) {
            return new HashMap<>();
        }

        // Фильтрация по типам
        if (types != null && !types.isEmpty()) {
            List<String> validTypes = List.of("Сила", "Скорость", "Выносливость");
            for (String type : types) {
                if (type != null && !validTypes.contains(type)) {
                    throw new IllegalArgumentException("Некорректный тип тренировки: " + type);
                }
            }

            trainings = trainings.stream()
                    .filter(t -> t != null && t.getType() != null && types.contains(t.getType()))
                    .collect(Collectors.toList());
        }

        Map<String, Map<String, Map<LocalDate, Double>>> result = new HashMap<>();

        for (Training t : trainings) {
            if (t == null || t.getDate() == null) {
                continue;
            }

            if (t.getExercises() == null) {
                continue;
            }

            for (ExercisesInTraining eit : t.getExercises()) {
                if (eit == null || eit.getExerciseCatalog() == null) {
                    continue;
                }

                String exerciseName = eit.getExerciseCatalog().getDescription();
                if (exerciseName == null) {
                    exerciseName = "Упражнение";
                }

                if (eit.getApproaches() == null) {
                    continue;
                }

                for (Approach a : eit.getApproaches()) {
                    if (a == null || a.getExerciseParameter() == null || a.getValue() == null) {
                        continue;
                    }

                    String paramCode = a.getExerciseParameter().getCode();
                    String paramName = getParameterDisplayName(paramCode);

                    double value;
                    try {
                        value = a.getValue().doubleValue();
                    } catch (Exception e) {
                        continue;
                    }

                    result.computeIfAbsent(exerciseName, k -> new HashMap<>())
                            .computeIfAbsent(paramName, k -> new HashMap<>())
                            .put(t.getDate(), value);
                }
            }
        }

        return result;
    }

    public Map<String, Map<LocalDate, Double>> getTonnageProgress(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Training> trainings;
        try {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
        } catch (Exception e) {
            return new HashMap<>();
        }

        if (trainings == null || trainings.isEmpty()) {
            return new HashMap<>();
        }

        if (types != null && !types.isEmpty()) {
            trainings = trainings.stream()
                    .filter(t -> t != null && t.getType() != null && types.contains(t.getType()))
                    .collect(Collectors.toList());
        }

        Map<String, Map<LocalDate, Double>> result = new HashMap<>();

        for (Training t : trainings) {
            if (t == null || t.getDate() == null) {
                continue;
            }

            if (t.getExercises() == null) {
                continue;
            }

            for (ExercisesInTraining eit : t.getExercises()) {
                if (eit == null || eit.getExerciseCatalog() == null) {
                    continue;
                }

                String exerciseName = eit.getExerciseCatalog().getDescription();
                if (exerciseName == null) {
                    exerciseName = "Упражнение";
                }

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
                        } catch (Exception e) {
                            // пропускаем
                        }
                    } else if ("повторения".equalsIgnoreCase(paramCode)) {
                        try {
                            reps = a.getValue().intValue();
                        } catch (Exception e) {
                            // пропускаем
                        }
                    }
                }

                if (weight != null && reps != null && weight > 0 && reps > 0) {
                    double tonnage = weight * reps;
                    result.computeIfAbsent(exerciseName, k -> new HashMap<>())
                            .put(t.getDate(), tonnage);
                }
            }
        }

        return result;
    }

    public Map<LocalDate, Integer> getVolumeProgress(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Training> trainings;
        try {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }

        if (trainings == null || trainings.isEmpty()) {
            return new LinkedHashMap<>();
        }

        if (types != null && !types.isEmpty()) {
            trainings = trainings.stream()
                    .filter(t -> t != null && t.getType() != null && types.contains(t.getType()))
                    .collect(Collectors.toList());
        }

        Map<LocalDate, Integer> result = new LinkedHashMap<>();

        for (Training t : trainings) {
            if (t == null || t.getDate() == null) {
                continue;
            }

            if (t.getExercises() == null) {
                result.put(t.getDate(), 0);
                continue;
            }

            int volume = t.getExercises().stream()
                    .filter(eit -> eit != null && eit.getApproaches() != null)
                    .mapToInt(eit -> eit.getApproaches().size())
                    .sum();
            result.put(t.getDate(), volume);
        }

        return result;
    }

    public Map<String, Object> getSummary(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        // Проверка входных данных
        if (cadetId == null || cadetId <= 0) {
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }
        if (from == null) {
            throw new IllegalArgumentException("Дата начала не может быть null");
        }
        if (to == null) {
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        List<Training> trainings;
        try {
            trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
        } catch (Exception e) {
            trainings = new ArrayList<>();
        }

        if (trainings == null) {
            trainings = new ArrayList<>();
        }

        if (types != null && !types.isEmpty()) {
            trainings = trainings.stream()
                    .filter(t -> t != null && t.getType() != null && types.contains(t.getType()))
                    .collect(Collectors.toList());
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTrainings", trainings.size());

        long daysBetween = ChronoUnit.DAYS.between(from, to) + 1;
        summary.put("totalDays", daysBetween);

        Map<String, Long> byType = new HashMap<>();
        double totalTonnage = 0;
        int totalApproaches = 0;

        for (Training t : trainings) {
            if (t == null) continue;

            String type = t.getType();
            if (type != null) {
                byType.put(type, byType.getOrDefault(type, 0L) + 1);
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
                                    // пропускаем
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
        }

        return summary;
    }

    private String getParameterDisplayName(String code) {
        if (code == null) return "Значение";

        switch (code.toLowerCase()) {
            case "повторения":
                return "Повторения";
            case "вес":
                return "Вес (кг)";
            case "время":
                return "Время (сек)";
            case "длительность":
                return "Длительность (мин)";
            case "дистанция":
                return "Дистанция (м)";
            default:
                return code;
        }
    }

    private LocalDate extractDate(Object obj) {
        if (obj == null) return null;

        try {
            if (obj instanceof Date) {
                return ((Date) obj).toLocalDate();
            } else if (obj instanceof java.util.Date) {
                return ((java.util.Date) obj).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } else if (obj instanceof LocalDate) {
                return (LocalDate) obj;
            }
        } catch (Exception e) {
            // игнорируем
        }
        return null;
    }

    private Double extractDouble(Object obj) {
        if (obj == null) return null;

        try {
            if (obj instanceof BigDecimal) {
                return ((BigDecimal) obj).setScale(1, RoundingMode.HALF_UP).doubleValue();
            } else if (obj instanceof Double) {
                return BigDecimal.valueOf((Double) obj)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
            } else if (obj instanceof Integer) {
                return ((Integer) obj).doubleValue();
            } else if (obj instanceof Long) {
                return ((Long) obj).doubleValue();
            } else if (obj instanceof Float) {
                return ((Float) obj).doubleValue();
            }
        } catch (Exception e) {
            // игнорируем
        }
        return null;
    }
}