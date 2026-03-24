package com.fakel.service;

import com.fakel.model.Standard;
import com.fakel.repository.StandardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        if (standard == null) {
            log.warn("Попытка оценки с null нормативом");
            throw new IllegalArgumentException("Норматив не может быть null");
        }

        if (course == null || course < 1 || course > 5) {
            log.warn("Некорректный курс: {}", course);
            throw new IllegalArgumentException("Курс должен быть от 1 до 5");
        }

        if (standard.getMeasurementUnit() == null || standard.getMeasurementUnit().getCode() == null) {
            log.warn("У норматива {} не указана единица измерения", standard.getId());
            throw new IllegalArgumentException("У норматива не указана единица измерения");
        }

        String unitCode = standard.getMeasurementUnit().getCode();
        log.debug("Тип норматива: {}", unitCode);

        List<Standard> standards;
        try {
            standards = standardRepository.findByNumberAndCourseOrderByGrade(
                    standard.getNumber(), course.shortValue());
            log.debug("Найдено {} нормативов для сравнения", standards != null ? standards.size() : 0);
        } catch (Exception e) {
            log.error("Ошибка при получении нормативов из БД: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении нормативов из БД: " + e.getMessage());
        }

        if (standards == null || standards.isEmpty()) {
            log.warn("Не найдены нормативы для сравнения: number={}, course={}", standard.getNumber(), course);
            return 2;
        }

        Short result;
        if (unitCode.equals("интервал")) {
            result = evaluateTime(timeValue, standards);
        } else {
            result = evaluateQuantity(intValue, standards);
        }

        log.info("Результат оценки: {}", result);
        return result;
    }

    public Short evaluateMarkWithStandards(Short standardNumber, Integer course,
                                           BigDecimal timeValue, Integer intValue,
                                           List<Standard> standards) {
        log.info("Оценка норматива: number={}, course={}, timeValue={}, intValue={}",
                standardNumber, course, timeValue, intValue);

        if (standardNumber == null) {
            log.warn("Номер норматива null");
            throw new IllegalArgumentException("Номер норматива не может быть null");
        }

        if (course == null || course < 1 || course > 5) {
            log.warn("Некорректный курс: {}", course);
            throw new IllegalArgumentException("Курс должен быть от 1 до 5");
        }

        if (standards == null || standards.isEmpty()) {
            log.warn("Список нормативов пуст для номера {} и курса {}", standardNumber, course);
            return 2;
        }

        String unitCode = null;
        for (Standard s : standards) {
            if (s != null && s.getMeasurementUnit() != null) {
                unitCode = s.getMeasurementUnit().getCode();
                break;
            }
        }

        if (unitCode == null) {
            log.warn("Не удалось определить тип норматива");
            return 2;
        }

        Short result;
        if (unitCode.equals("интервал")) {
            result = evaluateTime(timeValue, standards);
        } else {
            result = evaluateQuantity(intValue, standards);
        }

        log.info("Результат оценки: {}", result);
        return result;
    }

    private Short evaluateTime(BigDecimal result, List<Standard> standards) {
        log.debug("Оценка временного норматива с результатом: {} сек", result);

        if (result == null) {
            log.debug("Результат null, оценка 2");
            return 2;
        }

        if (result.compareTo(BigDecimal.ZERO) < 0) {
            log.debug("Результат отрицательный, оценка 2");
            return 2;
        }

        if (standards == null || standards.isEmpty()) {
            log.debug("Список нормативов пуст, оценка 2");
            return 2;
        }

        // Сортируем по возрастанию времени (чем меньше время, тем лучше)
        List<Standard> sortedStandards = standards.stream()
                .filter(s -> s != null && s.getTimeValue() != null)
                .sorted((s1, s2) -> {
                    BigDecimal t1 = convertDurationToBigDecimal(s1.getTimeValue());
                    BigDecimal t2 = convertDurationToBigDecimal(s2.getTimeValue());
                    return t1.compareTo(t2);  // от меньшего к большему
                })
                .toList();

        log.debug("Отсортированные нормативы по времени:");
        for (Standard s : sortedStandards) {
            BigDecimal time = convertDurationToBigDecimal(s.getTimeValue());
            log.debug("  {}: {} сек", s.getGrade(), time);
        }

        for (Standard standard : sortedStandards) {
            BigDecimal standardValue = convertDurationToBigDecimal(standard.getTimeValue());
            log.trace("Сравнение с {}: стандарт={} сек, результат={} сек",
                    standard.getGrade(), standardValue, result);

            // Для временных нормативов: результат должен быть МЕНЬШЕ или равен стандарту
            if (result.compareTo(standardValue) <= 0) {
                Short mark = convertGradeToMark(standard.getGrade());
                log.debug("Результат соответствует оценке {} ({})", mark, standard.getGrade());
                return mark;
            }
        }

        log.debug("Результат не соответствует ни одному нормативу, оценка 2");
        return 2;
    }

    private Short evaluateQuantity(Integer result, List<Standard> standards) {
        log.debug("Оценка количественного норматива с результатом: {}", result);

        if (result == null) {
            log.debug("Результат null, оценка 2");
            return 2;
        }

        if (result < 0) {
            log.debug("Результат отрицательный, оценка 2");
            return 2;
        }

        if (standards == null || standards.isEmpty()) {
            log.debug("Список нормативов пуст, оценка 2");
            return 2;
        }

        // Сортируем по убыванию количества (чем больше, тем лучше)
        List<Standard> sortedStandards = standards.stream()
                .filter(s -> s != null && s.getIntValue() != null)
                .sorted((s1, s2) -> {
                    return s2.getIntValue().compareTo(s1.getIntValue());  // от большего к меньшему
                })
                .toList();

        log.debug("Отсортированные нормативы по количеству:");
        for (Standard s : sortedStandards) {
            log.debug("  {}: {} раз", s.getGrade(), s.getIntValue());
        }

        for (Standard standard : sortedStandards) {
            int standardValue = standard.getIntValue().intValue();
            log.trace("Сравнение с {}: стандарт={}, результат={}",
                    standard.getGrade(), standardValue, result);

            if (result >= standardValue) {
                Short mark = convertGradeToMark(standard.getGrade());
                log.debug("Результат соответствует оценке {} ({})", mark, standard.getGrade());
                return mark;
            }
        }

        log.debug("Результат не соответствует ни одному нормативу, оценка 2");
        return 2;
    }

    /**
     * Конвертирует Duration в BigDecimal (секунды)
     * Пример: "14.1 seconds" -> 14.1
     *         "2 minutes 15 seconds" -> 135.0
     */
    private BigDecimal convertDurationToBigDecimal(Duration duration) {
        if (duration == null) {
            log.trace("Конвертация null Duration -> 0");
            return BigDecimal.ZERO;
        }

        try {
            long totalSeconds = duration.getSeconds();
            int nano = duration.getNano();

            // Если есть наносекунды, добавляем их как десятичную часть
            if (nano > 0) {
                BigDecimal seconds = BigDecimal.valueOf(totalSeconds);
                BigDecimal nanoPart = BigDecimal.valueOf(nano)
                        .divide(BigDecimal.valueOf(1_000_000_000), 3, RoundingMode.HALF_UP);
                return seconds.add(nanoPart);
            }

            log.trace("Конвертация Duration {} -> {} секунд", duration, totalSeconds);
            return BigDecimal.valueOf(totalSeconds);
        } catch (Exception e) {
            log.warn("Ошибка при конвертации Duration: {}, возвращаем 0", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Short convertGradeToMark(String grade) {
        if (grade == null) return 2;
        switch (grade) {
            case "Отлично":
                return 5;
            case "Хорошо":
                return 4;
            case "Удовлетворительно":
                return 3;
            default:
                return 2;
        }
    }
}