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

    private static final Logger logger = LoggerFactory.getLogger(TrainingAnalyticsService.class);

    private final TrainingRepository trainingRepository;

    @Autowired
    public TrainingAnalyticsService(TrainingRepository trainingRepository) {
        this.trainingRepository = trainingRepository;
    }

    public Map<LocalDate, Double> getWeightProgress(Long cadetId, LocalDate from, LocalDate to) {
        logger.info("Getting weight progress for cadet {} from {} to {}", cadetId, from, to);

        List<Object[]> rows = trainingRepository.getWeightProgress(cadetId, from, to);
        Map<LocalDate, Double> result = new LinkedHashMap<>();

        for (Object[] row : rows) {
            if (row.length < 2) {
                logger.warn("Skipping row with insufficient data: {}", Arrays.toString(row));
                continue;
            }

            LocalDate date = null;
            if (row[0] instanceof Date) {
                date = ((Date) row[0]).toLocalDate();
            } else if (row[0] instanceof java.util.Date) {
                date = ((java.util.Date) row[0]).toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } else {
                logger.warn("Unexpected date type: {}", row[0].getClass());
                continue;
            }

            Double weight = null;
            if (row[1] instanceof BigDecimal) {
                weight = ((BigDecimal) row[1]).setScale(1, RoundingMode.HALF_UP).doubleValue();
            } else if (row[1] instanceof Double) {
                weight = BigDecimal.valueOf((Double) row[1])
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
            } else if (row[1] instanceof Integer) {
                weight = ((Integer) row[1]).doubleValue();
            } else {
                logger.warn("Unexpected weight type: {}", row[1].getClass());
                continue;
            }

            if (date != null && weight != null) {
                result.put(date, weight);
            }
        }

        logger.info("Found {} weight records", result.size());
        return result;
    }

    public Map<String, Map<String, Map<LocalDate, Double>>> getExercisesProgress(
            Long cadetId, LocalDate from, LocalDate to, List<String> types) {

        logger.info("Getting exercises progress for cadet {} from {} to {}", cadetId, from, to);

        List<Training> trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);
        logger.info("Found {} trainings", trainings.size());

        if (types != null && !types.isEmpty()) {
            trainings = trainings.stream()
                    .filter(t -> types.contains(t.getType()))
                    .collect(Collectors.toList());
            logger.info("After filtering by types {}: {} trainings", types, trainings.size());
        }

        Map<String, Map<String, Map<LocalDate, Double>>> result = new HashMap<>();

        for (Training t : trainings) {
            logger.debug("Processing training on {}", t.getDate());

            for (ExercisesInTraining eit : t.getExercises()) {
                String exerciseName = eit.getExerciseCatalog().getDescription();
                logger.debug("  Exercise: {}", exerciseName);

                for (Approach a : eit.getApproaches()) {
                    String paramCode = a.getExerciseParameter().getCode();
                    String paramName = getParameterDisplayName(paramCode);
                    double value = a.getValue().doubleValue();

                    result.computeIfAbsent(exerciseName, k -> new HashMap<>())
                            .computeIfAbsent(paramName, k -> new HashMap<>())
                            .put(t.getDate(), value);

                    logger.debug("    Parameter: {} = {}", paramName, value);
                }
            }
        }

        logger.info("Found data for {} exercises", result.size());
        return result;
    }

    public Map<String, Map<LocalDate, Double>> getTonnageProgress(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        List<Training> trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);

        if (types != null && !types.isEmpty()) {
            trainings = trainings.stream()
                    .filter(t -> types.contains(t.getType()))
                    .collect(Collectors.toList());
        }

        Map<String, Map<LocalDate, Double>> result = new HashMap<>();

        for (Training t : trainings) {
            for (ExercisesInTraining eit : t.getExercises()) {
                String exerciseName = eit.getExerciseCatalog().getDescription();

                Double weight = null;
                Integer reps = null;

                for (Approach a : eit.getApproaches()) {
                    String paramCode = a.getExerciseParameter().getCode();
                    if ("вес".equalsIgnoreCase(paramCode)) {
                        weight = a.getValue().doubleValue();
                    } else if ("повторения".equalsIgnoreCase(paramCode)) {
                        reps = a.getValue().intValue();
                    }
                }

                if (weight != null && reps != null) {
                    double tonnage = weight * reps;
                    result.computeIfAbsent(exerciseName, k -> new HashMap<>())
                            .put(t.getDate(), tonnage);
                }
            }
        }

        return result;
    }

    public Map<LocalDate, Integer> getVolumeProgress(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        List<Training> trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);

        if (types != null && !types.isEmpty()) {
            trainings = trainings.stream()
                    .filter(t -> types.contains(t.getType()))
                    .collect(Collectors.toList());
        }

        Map<LocalDate, Integer> result = new LinkedHashMap<>();

        for (Training t : trainings) {
            int volume = t.getExercises().stream()
                    .mapToInt(eit -> eit.getApproaches().size())
                    .sum();
            result.put(t.getDate(), volume);
        }

        return result;
    }

    public Map<String, Object> getSummary(Long cadetId, LocalDate from, LocalDate to, List<String> types) {
        logger.info("Generating summary for cadet {} from {} to {}", cadetId, from, to);

        List<Training> trainings = trainingRepository.findByCadetIdAndDateBetween(cadetId, from, to);

        if (types != null && !types.isEmpty()) {
            trainings = trainings.stream()
                    .filter(t -> types.contains(t.getType()))
                    .collect(Collectors.toList());
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTrainings", trainings.size());
        summary.put("totalDays", ChronoUnit.DAYS.between(from, to) + 1);

        Map<String, Long> byType = new HashMap<>();
        double totalTonnage = 0;
        int totalApproaches = 0;

        for (Training t : trainings) {
            String type = t.getType();
            byType.put(type, byType.getOrDefault(type, 0L) + 1);

            for (ExercisesInTraining eit : t.getExercises()) {
                totalApproaches += eit.getApproaches().size();

                for (Approach a : eit.getApproaches()) {
                    if ("вес".equalsIgnoreCase(a.getExerciseParameter().getCode())) {
                        totalTonnage += a.getValue().doubleValue();
                    }
                }
            }
        }

        summary.put("byType", byType);
        summary.put("totalTonnage", BigDecimal.valueOf(totalTonnage)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue());
        summary.put("totalApproaches", totalApproaches);

        if (!trainings.isEmpty()) {
            double avgTrainingsPerWeek = (trainings.size() * 7.0) / ChronoUnit.DAYS.between(from, to);
            summary.put("avgTrainingsPerWeek", BigDecimal.valueOf(avgTrainingsPerWeek)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue());
        }

        logger.info("Summary generated: {}", summary);
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
}