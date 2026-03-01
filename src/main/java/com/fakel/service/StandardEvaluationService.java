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
        List<Standard> standards = standardRepository.findByNumberAndCourseOrderByGrade(
                standard.getNumber(), course.shortValue());

        String unitCode = standard.getMeasurementUnit().getCode();

        if (unitCode.equals("интервал")) {
            if (timeValue == null) return 2;
            return evaluateTime(timeValue, standards);
        } else {
            if (intValue == null) return 2;
            return evaluateQuantity(intValue, standards);
        }
    }

    private Short evaluateTime(BigDecimal result, List<Standard> standards) {
        for (Standard s : standards) {
            if (s.getGrade().equals("Отлично") && s.getTimeValue() != null) {
                BigDecimal standardValue = convertDurationToBigDecimal(s.getTimeValue());
                if (result.compareTo(standardValue) <= 0) return 5;
            }
        }
        for (Standard s : standards) {
            if (s.getGrade().equals("Хорошо") && s.getTimeValue() != null) {
                BigDecimal standardValue = convertDurationToBigDecimal(s.getTimeValue());
                if (result.compareTo(standardValue) <= 0) return 4;
            }
        }
        for (Standard s : standards) {
            if (s.getGrade().equals("Удовлетворительно") && s.getTimeValue() != null) {
                BigDecimal standardValue = convertDurationToBigDecimal(s.getTimeValue());
                if (result.compareTo(standardValue) <= 0) return 3;
            }
        }
        return 2;
    }

    private Short evaluateQuantity(Integer result, List<Standard> standards) {
        // Если результата нет - возвращаем 2 (не сдал)
        if (result == null) {
            return 2;
        }

        for (Standard s : standards) {
            if (s.getGrade().equals("Отлично") && s.getIntValue() != null) {
                if (result >= s.getIntValue().intValue()) return 5;
            }
        }
        for (Standard s : standards) {
            if (s.getGrade().equals("Хорошо") && s.getIntValue() != null) {
                if (result >= s.getIntValue().intValue()) return 4;
            }
        }
        for (Standard s : standards) {
            if (s.getGrade().equals("Удовлетворительно") && s.getIntValue() != null) {
                if (result >= s.getIntValue().intValue()) return 3;
            }
        }
        return 2;
    }

    private BigDecimal convertDurationToBigDecimal(Duration duration) {
        if (duration == null) return BigDecimal.ZERO;

        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        BigDecimal minutesDecimal = BigDecimal.valueOf(minutes);
        BigDecimal secondsDecimal = BigDecimal.valueOf(seconds)
                .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);

        return minutesDecimal.add(secondsDecimal);
    }
}