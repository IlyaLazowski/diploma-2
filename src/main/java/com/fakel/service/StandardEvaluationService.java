package com.fakel.service;

import com.fakel.model.Standard;
import com.fakel.repository.StandardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
public class StandardEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(StandardEvaluationService.class);

    @Autowired
    private StandardRepository standardRepository;

    public Short evaluateMark(Standard standard, BigDecimal timeValue, Integer intValue, Integer course) {

        log.info("Оценка норматива: standardId={}, course={}, timeValue={}, intValue={}",
                standard != null ? standard.getId() : null, course, timeValue, intValue);
        log.debug("Детали норматива: number={}, name={}, unit={}",
                standard != null ? standard.getNumber() : null,
                standard != null ? standard.getName() : null,
                standard != null && standard.getMeasurementUnit() != null ? standard.getMeasurementUnit().getCode() : null);

        // Проверка входных данных
        if (standard == null) {
            log.warn("Попытка оценки с null нормативом");
            throw new IllegalArgumentException("Норматив не может быть null");
        }

        if (course == null) {
            log.warn("Курс null при оценке норматива {}", standard.getId());
            throw new IllegalArgumentException("Курс не может быть null");
        }

        if (course < 1 || course > 5) {
            log.warn("Некорректный курс {} при оценке норматива {}", course, standard.getId());
            throw new IllegalArgumentException("Курс должен быть от 1 до 5, получено: " + course);
        }

        if (standard.getMeasurementUnit() == null) {
            log.warn("У норматива {} не указана единица измерения", standard.getId());
            throw new IllegalArgumentException("У норматива не указана единица измерения");
        }

        String unitCode = standard.getMeasurementUnit().getCode();
        if (unitCode == null) {
            log.warn("Код единицы измерения null для норматива {}", standard.getId());
            throw new IllegalArgumentException("Код единицы измерения не может быть null");
        }

        log.debug("Тип норматива: {}", unitCode);

        // Получаем нормативы для сравнения
        List<Standard> standards;
        try {
            log.debug("Поиск нормативов для сравнения: number={}, course={}",
                    standard.getNumber(), course);
            standards = standardRepository.findByNumberAndCourseOrderByGrade(
                    standard.getNumber(), course.shortValue());
            log.debug("Найдено {} нормативов для сравнения", standards != null ? standards.size() : 0);
        } catch (Exception e) {
            log.error("Ошибка при получении нормативов из БД: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении нормативов из БД: " + e.getMessage());
        }

        if (standards == null || standards.isEmpty()) {
            log.warn("Не найдены нормативы для сравнения: number={}, course={}",
                    standard.getNumber(), course);
            throw new RuntimeException("Не найдены нормативы для сравнения");
        }

        // Оценка в зависимости от типа
        Short result;
        if (unitCode.equals("интервал")) {
            log.debug("Оценка временного норматива");
            result = evaluateTime(timeValue, standards);
        } else {
            log.debug("Оценка количественного норматива");
            result = evaluateQuantity(intValue, standards);
        }

        log.info("Результат оценки: {}", result);
        return result;
    }

    private Short evaluateTime(BigDecimal result, List<Standard> standards) {
        log.debug("Оценка временного норматива с результатом: {}", result);

        // Проверка результата
        if (result == null) {
            log.debug("Результат null, оценка 2");
            return 2;
        }

        if (result.compareTo(BigDecimal.ZERO) < 0) {
            log.debug("Результат отрицательный, оценка 2");
            return 2;
        }

        // Проверка списка нормативов
        if (standards == null || standards.isEmpty()) {
            log.debug("Список нормативов пуст, оценка 2");
            return 2;
        }

        // Проверяем на "Отлично"
        for (Standard s : standards) {
            if (s == null) continue;

            if ("Отлично".equals(s.getGrade())) {
                Duration timeValue = s.getTimeValue();
                if (timeValue != null) {
                    BigDecimal standardValue = convertDurationToBigDecimal(timeValue);
                    log.trace("Сравнение с нормативом Отлично: стандарт={}, результат={}",
                            standardValue, result);
                    if (result.compareTo(standardValue) <= 0) {
                        log.debug("Результат соответствует оценке 5 (Отлично)");
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
                    log.trace("Сравнение с нормативом Хорошо: стандарт={}, результат={}",
                            standardValue, result);
                    if (result.compareTo(standardValue) <= 0) {
                        log.debug("Результат соответствует оценке 4 (Хорошо)");
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
                    log.trace("Сравнение с нормативом Удовлетворительно: стандарт={}, результат={}",
                            standardValue, result);
                    if (result.compareTo(standardValue) <= 0) {
                        log.debug("Результат соответствует оценке 3 (Удовлетворительно)");
                        return 3;
                    }
                }
            }
        }

        log.debug("Результат не соответствует ни одному нормативу, оценка 2");
        return 2;
    }

    private Short evaluateQuantity(Integer result, List<Standard> standards) {
        log.debug("Оценка количественного норматива с результатом: {}", result);

        // Проверка результата
        if (result == null) {
            log.debug("Результат null, оценка 2");
            return 2;
        }

        if (result < 0) {
            log.debug("Результат отрицательный, оценка 2");
            return 2;
        }

        // Проверка списка нормативов
        if (standards == null || standards.isEmpty()) {
            log.debug("Список нормативов пуст, оценка 2");
            return 2;
        }

        // Проверяем на "Отлично"
        for (Standard s : standards) {
            if (s == null) continue;

            if ("Отлично".equals(s.getGrade())) {
                BigDecimal intValue = s.getIntValue();
                if (intValue != null) {
                    log.trace("Сравнение с нормативом Отлично: стандарт={}, результат={}",
                            intValue.intValue(), result);
                    if (result >= intValue.intValue()) {
                        log.debug("Результат соответствует оценке 5 (Отлично)");
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
                    log.trace("Сравнение с нормативом Хорошо: стандарт={}, результат={}",
                            intValue.intValue(), result);
                    if (result >= intValue.intValue()) {
                        log.debug("Результат соответствует оценке 4 (Хорошо)");
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
                    log.trace("Сравнение с нормативом Удовлетворительно: стандарт={}, результат={}",
                            intValue.intValue(), result);
                    if (result >= intValue.intValue()) {
                        log.debug("Результат соответствует оценке 3 (Удовлетворительно)");
                        return 3;
                    }
                }
            }
        }

        log.debug("Результат не соответствует ни одному нормативу, оценка 2");
        return 2;
    }

    private BigDecimal convertDurationToBigDecimal(Duration duration) {
        if (duration == null) {
            log.trace("Конвертация null Duration в BigDecimal.ZERO");
            return BigDecimal.ZERO;
        }

        try {
            long totalSeconds = duration.getSeconds();
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;

            BigDecimal minutesDecimal = BigDecimal.valueOf(minutes);
            BigDecimal secondsDecimal = BigDecimal.valueOf(seconds)
                    .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

            BigDecimal result = minutesDecimal.add(secondsDecimal);
            log.trace("Конвертация Duration {} в BigDecimal: {} мин {} сек = {}",
                    duration, minutes, seconds, result);
            return result;
        } catch (Exception e) {
            log.warn("Ошибка при конвертации Duration: {}, возвращаем 0", e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}