package com.fakel.service;

import com.fakel.model.MeasurementUnit;
import com.fakel.model.Standard;
import com.fakel.repository.StandardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StandardEvaluationServiceTest {

    @Mock
    private StandardRepository standardRepository;

    @InjectMocks
    private StandardEvaluationService standardEvaluationService;

    private Standard testStandardTime;
    private Standard testStandardQuantity;
    private MeasurementUnit timeUnit;
    private MeasurementUnit quantityUnit;

    private Standard standardExcellent;
    private Standard standardGood;
    private Standard standardSatisfactory;

    @BeforeEach
    void setUp() {
        timeUnit = new MeasurementUnit();
        timeUnit.setId(1L);
        timeUnit.setCode("интервал");

        quantityUnit = new MeasurementUnit();
        quantityUnit.setId(2L);
        quantityUnit.setCode("количество");

        testStandardTime = new Standard();
        testStandardTime.setId(1L);
        testStandardTime.setNumber((short) 1);
        testStandardTime.setName("Бег 100м");
        testStandardTime.setMeasurementUnit(timeUnit);
        testStandardTime.setCourse((short) 2);
        testStandardTime.setGrade("Отлично");
        testStandardTime.setTimeValue(Duration.ofSeconds(90));

        testStandardQuantity = new Standard();
        testStandardQuantity.setId(2L);
        testStandardQuantity.setNumber((short) 2);
        testStandardQuantity.setName("Подтягивания");
        testStandardQuantity.setMeasurementUnit(quantityUnit);
        testStandardQuantity.setCourse((short) 2);
        testStandardQuantity.setGrade("Отлично");
        testStandardQuantity.setIntValue(new BigDecimal("15"));

        standardExcellent = new Standard();
        standardExcellent.setId(3L);
        standardExcellent.setNumber((short) 1);
        standardExcellent.setName("Бег 100м");
        standardExcellent.setMeasurementUnit(timeUnit);
        standardExcellent.setCourse((short) 2);
        standardExcellent.setGrade("Отлично");
        standardExcellent.setTimeValue(Duration.ofSeconds(90));

        standardGood = new Standard();
        standardGood.setId(4L);
        standardGood.setNumber((short) 1);
        standardGood.setName("Бег 100м");
        standardGood.setMeasurementUnit(timeUnit);
        standardGood.setCourse((short) 2);
        standardGood.setGrade("Хорошо");
        standardGood.setTimeValue(Duration.ofSeconds(95));

        standardSatisfactory = new Standard();
        standardSatisfactory.setId(5L);
        standardSatisfactory.setNumber((short) 1);
        standardSatisfactory.setName("Бег 100м");
        standardSatisfactory.setMeasurementUnit(timeUnit);
        standardSatisfactory.setCourse((short) 2);
        standardSatisfactory.setGrade("Удовлетворительно");
        standardSatisfactory.setTimeValue(Duration.ofSeconds(100));
    }

    @Test
    void evaluateMark_WithValidTimeStandard_ShouldReturnCorrectMark() {
        BigDecimal timeValue = new BigDecimal("1.30");
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(eq((short)1), eq((short)2)))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        assertEquals((short)5, result);
        verify(standardRepository, times(1)).findByNumberAndCourseOrderByGrade((short)1, (short)2);
    }

    @Test
    void evaluateMark_WithValidQuantityStandard_ShouldReturnCorrectMark() {
        Integer intValue = 15;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        Standard good = new Standard();
        good.setGrade("Хорошо");
        good.setIntValue(new BigDecimal("12"));

        Standard satisfactory = new Standard();
        satisfactory.setGrade("Удовлетворительно");
        satisfactory.setIntValue(new BigDecimal("10"));

        List<Standard> standards = List.of(excellent, good, satisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(eq((short)2), eq((short)2)))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, intValue, 2);

        assertEquals((short)5, result);
    }

    @Test
    void evaluateMark_WithNullStandard_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(null, new BigDecimal("1.30"), null, 2)
        );

        assertEquals("Норматив не может быть null", exception.getMessage());
    }

    @Test
    void evaluateMark_WithNullCourse_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, null)
        );

        assertEquals("Курс не может быть null", exception.getMessage());
    }

    @Test
    void evaluateMark_WithInvalidCourse_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 0)
        );

        assertEquals("Курс должен быть от 1 до 5, получено: 0", exception.getMessage());
    }

    @Test
    void evaluateMark_WithNullMeasurementUnit_ShouldThrowException() {
        testStandardTime.setMeasurementUnit(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 2)
        );

        assertEquals("У норматива не указана единица измерения", exception.getMessage());
    }

    @Test
    void evaluateMark_WithNullUnitCode_ShouldThrowException() {
        timeUnit.setCode(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 2)
        );

        assertEquals("Код единицы измерения не может быть null", exception.getMessage());
    }

    @Test
    void evaluateMark_WhenNoStandardsFound_ShouldThrowException() {
        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(new ArrayList<>());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 2)
        );

        assertEquals("Не найдены нормативы для сравнения", exception.getMessage());
    }

    @Test
    void evaluateMark_WhenRepositoryThrowsException_ShouldThrowRuntimeException() {
        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenThrow(new RuntimeException("DB error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 2)
        );

        assertTrue(exception.getMessage().contains("Ошибка при получении нормативов из БД"));
    }

    @Test
    void evaluateTime_WithNegativeResult_ShouldReturn2() {
        BigDecimal negativeTime = new BigDecimal("-1.0");
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardTime, negativeTime, null, 2);

        assertEquals((short)2, result);
    }

    @Test
    void evaluateTime_WithEmptyStandards_ShouldReturn2() {
        BigDecimal timeValue = new BigDecimal("1.30");
        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(new ArrayList<>());

        assertThrows(RuntimeException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2));
    }

    @Test
    void evaluateTime_WithResultBetterThanExcellent_ShouldReturn5() {
        BigDecimal timeValue = new BigDecimal("1.20");
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        assertEquals((short)5, result);
    }

    @Test
    void evaluateTime_WithResultBetweenExcellentAndGood_ShouldReturn5() {
        BigDecimal timeValue = new BigDecimal("1.30");
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        assertEquals((short)5, result);
    }

    @Test
    void evaluateQuantity_WithNegativeResult_ShouldReturn2() {
        Integer negativeValue = -5;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        List<Standard> standards = List.of(excellent);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, negativeValue, 2);

        assertEquals((short)2, result);
    }

    @Test
    void evaluateQuantity_WithResultAboveExcellent_ShouldReturn5() {
        Integer resultValue = 20;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        Standard good = new Standard();
        good.setGrade("Хорошо");
        good.setIntValue(new BigDecimal("12"));

        Standard satisfactory = new Standard();
        satisfactory.setGrade("Удовлетворительно");
        satisfactory.setIntValue(new BigDecimal("10"));

        List<Standard> standards = List.of(excellent, good, satisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        assertEquals((short)5, result);
    }

    @Test
    void evaluateQuantity_WithResultEqualToExcellent_ShouldReturn5() {
        Integer resultValue = 15;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        Standard good = new Standard();
        good.setGrade("Хорошо");
        good.setIntValue(new BigDecimal("12"));

        Standard satisfactory = new Standard();
        satisfactory.setGrade("Удовлетворительно");
        satisfactory.setIntValue(new BigDecimal("10"));

        List<Standard> standards = List.of(excellent, good, satisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        assertEquals((short)5, result);
    }

    @Test
    void evaluateQuantity_WithResultBetweenExcellentAndGood_ShouldReturn4() {
        Integer resultValue = 13;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        Standard good = new Standard();
        good.setGrade("Хорошо");
        good.setIntValue(new BigDecimal("12"));

        Standard satisfactory = new Standard();
        satisfactory.setGrade("Удовлетворительно");
        satisfactory.setIntValue(new BigDecimal("10"));

        List<Standard> standards = List.of(excellent, good, satisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        assertEquals((short)4, result);
    }

    @Test
    void evaluateQuantity_WithResultEqualToGood_ShouldReturn4() {
        Integer resultValue = 12;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        Standard good = new Standard();
        good.setGrade("Хорошо");
        good.setIntValue(new BigDecimal("12"));

        Standard satisfactory = new Standard();
        satisfactory.setGrade("Удовлетворительно");
        satisfactory.setIntValue(new BigDecimal("10"));

        List<Standard> standards = List.of(excellent, good, satisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        assertEquals((short)4, result);
    }

    @Test
    void evaluateQuantity_WithResultBetweenGoodAndSatisfactory_ShouldReturn3() {
        Integer resultValue = 11;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        Standard good = new Standard();
        good.setGrade("Хорошо");
        good.setIntValue(new BigDecimal("12"));

        Standard satisfactory = new Standard();
        satisfactory.setGrade("Удовлетворительно");
        satisfactory.setIntValue(new BigDecimal("10"));

        List<Standard> standards = List.of(excellent, good, satisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        assertEquals((short)3, result);
    }

    @Test
    void evaluateQuantity_WithResultEqualToSatisfactory_ShouldReturn3() {
        Integer resultValue = 10;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        Standard good = new Standard();
        good.setGrade("Хорошо");
        good.setIntValue(new BigDecimal("12"));

        Standard satisfactory = new Standard();
        satisfactory.setGrade("Удовлетворительно");
        satisfactory.setIntValue(new BigDecimal("10"));

        List<Standard> standards = List.of(excellent, good, satisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        assertEquals((short)3, result);
    }

    @Test
    void evaluateQuantity_WithResultBelowSatisfactory_ShouldReturn2() {
        Integer resultValue = 8;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        Standard good = new Standard();
        good.setGrade("Хорошо");
        good.setIntValue(new BigDecimal("12"));

        Standard satisfactory = new Standard();
        satisfactory.setGrade("Удовлетворительно");
        satisfactory.setIntValue(new BigDecimal("10"));

        List<Standard> standards = List.of(excellent, good, satisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        assertEquals((short)2, result);
    }

    @Test
    void convertDurationToBigDecimal_WithNullDuration_ShouldReturnZero() {
        BigDecimal timeValue = null;

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(List.of(standardExcellent));

        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        assertEquals((short)2, result);
    }

    @Test
    void convertDurationToBigDecimal_WithValidDuration_ShouldConvertCorrectly() {
        BigDecimal timeValue = new BigDecimal("1.30");
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        assertEquals((short)5, result);
    }
}