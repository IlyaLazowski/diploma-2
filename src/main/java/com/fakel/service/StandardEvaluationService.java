package com.fakel.service;

import com.fakel.model.Standard;
import com.fakel.repository.StandardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
public class StandardEvaluationService {

    @Autowired
    private StandardRepository standardRepository;

    public Short evaluateMark(Standard standard, BigDecimal timeValue, Integer intValue, Integer course) {

        // Проверка входных данных
        if (standard == null) {
            throw new IllegalArgumentException("Норматив не может быть null");
        }

        if (course == null) {
            throw new IllegalArgumentException("Курс не может быть null");
        }

        if (course < 1 || course > 5) {
            throw new IllegalArgumentException("Курс должен быть от 1 до 5, получено: " + course);
        }

        if (standard.getMeasurementUnit() == null) {
            throw new IllegalArgumentException("У норматива не указана единица измерения");
        }

        String unitCode = standard.getMeasurementUnit().getCode();
        if (unitCode == null) {
            throw new IllegalArgumentException("Код единицы измерения не может быть null");
        }

        // Получаем нормативы для сравнения
        List<Standard> standards;
        try {
            standards = standardRepository.findByNumberAndCourseOrderByGrade(
                    standard.getNumber(), course.shortValue());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении нормативов из БД: " + e.getMessage());
        }

        if (standards == null || standards.isEmpty()) {
            throw new RuntimeException("Не найдены нормативы для сравнения");
        }

        // Оценка в зависимости от типа
        if (unitCode.equals("интервал")) {
            return evaluateTime(timeValue, standards);
        } else {
            return evaluateQuantity(intValue, standards);
        }
    }

    private Short evaluateTime(BigDecimal result, List<Standard> standards) {

        // Проверка результата
        if (result == null) {
            return 2;
        }

        if (result.compareTo(BigDecimal.ZERO) < 0) {
            return 2;
        }

        // Проверка списка нормативов
        if (standards == null || standards.isEmpty()) {
            return 2;
        }

        // Проверяем на "Отлично"
        for (Standard s : standards) {
            if (s == null) continue;

            if ("Отлично".equals(s.getGrade())) {
                Duration timeValue = s.getTimeValue();
                if (timeValue != null) {
                    BigDecimal standardValue = convertDurationToBigDecimal(timeValue);
                    if (result.compareTo(standardValue) <= 0) {
                        return 5;
                    }
                }
            }
        }

        // Проверяем на "Хорошо"
        for (Standard s : standards) {
            if (s == null) continue;

            if ("Хорошо".equals(s.getGrade())) {
                Duration timeValue = s.getTimeValue();
                if (timeValue != null) {
                    BigDecimal standardValue = convertDurationToBigDecimal(timeValue);
                    if (result.compareTo(standardValue) <= 0) {
                        return 4;
                    }
                }
            }
        }

        // Проверяем на "Удовлетворительно"
        for (Standard s : standards) {
            if (s == null) continue;

            if ("Удовлетворительно".equals(s.getGrade())) {
                Duration timeValue = s.getTimeValue();
                if (timeValue != null) {
                    BigDecimal standardValue = convertDurationToBigDecimal(timeValue);
                    if (result.compareTo(standardValue) <= 0) {
                        return 3;
                    }
                }
            }
        }

        return 2;
    }

    private Short evaluateQuantity(Integer result, List<Standard> standards) {

        // Проверка результата
        if (result == null) {
            return 2;
        }

        if (result < 0) {
            return 2;
        }

        // Проверка списка нормативов
        if (standards == null || standards.isEmpty()) {
            return 2;
        }

        // Проверяем на "Отлично"
        for (Standard s : standards) {
            if (s == null) continue;

            if ("Отлично".equals(s.getGrade())) {
                BigDecimal intValue = s.getIntValue();
                if (intValue != null) {
                    if (result >= intValue.intValue()) {
                        return 5;
                    }
                }
            }
        }

        // Проверяем на "Хорошо"
        for (Standard s : standards) {
            if (s == null) continue;

            if ("Хорошо".equals(s.getGrade())) {
                BigDecimal intValue = s.getIntValue();
                if (intValue != null) {
                    if (result >= intValue.intValue()) {
                        return 4;
                    }
                }
            }
        }

        // Проверяем на "Удовлетворительно"
        for (Standard s : standards) {
            if (s == null) continue;

            if ("Удовлетворительно".equals(s.getGrade())) {
                BigDecimal intValue = s.getIntValue();
                if (intValue != null) {
                    if (result >= intValue.intValue()) {
                        return 3;
                    }
                }
            }
        }

        return 2;
    }

    private BigDecimal convertDurationToBigDecimal(Duration duration) {
        if (duration == null) {
            return BigDecimal.ZERO;
        }

        try {
            long totalSeconds = duration.getSeconds();
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;

            BigDecimal minutesDecimal = BigDecimal.valueOf(minutes);
            BigDecimal secondsDecimal = BigDecimal.valueOf(seconds)
                    .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

            return minutesDecimal.add(secondsDecimal);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}