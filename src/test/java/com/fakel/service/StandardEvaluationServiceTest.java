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
        // Создаем единицы измерения
        timeUnit = new MeasurementUnit();
        timeUnit.setId(1L);
        timeUnit.setCode("интервал");

        quantityUnit = new MeasurementUnit();
        quantityUnit.setId(2L);
        quantityUnit.setCode("количество");

        // Создаем тестовый норматив (временной)
        testStandardTime = new Standard();
        testStandardTime.setId(1L);
        testStandardTime.setNumber((short) 1);
        testStandardTime.setName("Бег 100м");
        testStandardTime.setMeasurementUnit(timeUnit);
        testStandardTime.setCourse((short) 2);
        testStandardTime.setGrade("Отлично");
        testStandardTime.setTimeValue(Duration.ofSeconds(90));

        // Создаем тестовый норматив (количественный)
        testStandardQuantity = new Standard();
        testStandardQuantity.setId(2L);
        testStandardQuantity.setNumber((short) 2);
        testStandardQuantity.setName("Подтягивания");
        testStandardQuantity.setMeasurementUnit(quantityUnit);
        testStandardQuantity.setCourse((short) 2);
        testStandardQuantity.setGrade("Отлично");
        testStandardQuantity.setIntValue(new BigDecimal("15"));

        // Создаем нормативы для сравнения (временные)
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

    // ============= ТЕСТЫ ДЛЯ evaluateMark =============

    @Test
    void evaluateMark_WithValidTimeStandard_ShouldReturnCorrectMark() {
        // Given
        BigDecimal timeValue = new BigDecimal("1.30"); // 1 минута 30 секунд = 90 секунд = 1.5 минуты
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(eq((short)1), eq((short)2)))
                .thenReturn(standards);

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        // Then
        assertEquals((short)5, result);
        verify(standardRepository, times(1)).findByNumberAndCourseOrderByGrade((short)1, (short)2);
    }

    @Test
    void evaluateMark_WithValidQuantityStandard_ShouldReturnCorrectMark() {
        // Given
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

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, intValue, 2);

        // Then
        assertEquals((short)5, result);
    }

    @Test
    void evaluateMark_WithNullStandard_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(null, new BigDecimal("1.30"), null, 2)
        );

        assertEquals("Норматив не может быть null", exception.getMessage());
    }

    @Test
    void evaluateMark_WithNullCourse_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, null)
        );

        assertEquals("Курс не может быть null", exception.getMessage());
    }

    @Test
    void evaluateMark_WithInvalidCourse_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 0)
        );

        assertEquals("Курс должен быть от 1 до 5, получено: 0", exception.getMessage());
    }

    @Test
    void evaluateMark_WithNullMeasurementUnit_ShouldThrowException() {
        // Given
        testStandardTime.setMeasurementUnit(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 2)
        );

        assertEquals("У норматива не указана единица измерения", exception.getMessage());
    }

    @Test
    void evaluateMark_WithNullUnitCode_ShouldThrowException() {
        // Given
        timeUnit.setCode(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 2)
        );

        assertEquals("Код единицы измерения не может быть null", exception.getMessage());
    }

    @Test
    void evaluateMark_WhenNoStandardsFound_ShouldThrowException() {
        // Given
        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(new ArrayList<>());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 2)
        );

        assertEquals("Не найдены нормативы для сравнения", exception.getMessage());
    }

    @Test
    void evaluateMark_WhenRepositoryThrowsException_ShouldThrowRuntimeException() {
        // Given
        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenThrow(new RuntimeException("DB error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, new BigDecimal("1.30"), null, 2)
        );

        assertTrue(exception.getMessage().contains("Ошибка при получении нормативов из БД"));
    }

    // ============= ТЕСТЫ ДЛЯ evaluateTime =============


    @Test
    void evaluateTime_WithNegativeResult_ShouldReturn2() {
        // Given
        BigDecimal negativeTime = new BigDecimal("-1.0");
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardTime, negativeTime, null, 2);

        // Then
        assertEquals((short)2, result);
    }

    @Test
    void evaluateTime_WithEmptyStandards_ShouldReturn2() {
        // Given
        BigDecimal timeValue = new BigDecimal("1.30");
        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(RuntimeException.class,
                () -> standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2));
    }

    @Test
    void evaluateTime_WithResultBetterThanExcellent_ShouldReturn5() {
        // Given
        BigDecimal timeValue = new BigDecimal("1.20"); // 1 минута 20 секунд (лучше чем 1.30)
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        // Then
        assertEquals((short)5, result);
    }

    @Test
    void evaluateTime_WithResultBetweenExcellentAndGood_ShouldReturn5() {
        // Given
        BigDecimal timeValue = new BigDecimal("1.30"); // точно как отлично
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        // Then
        assertEquals((short)5, result);
    }




    // ============= ТЕСТЫ ДЛЯ evaluateQuantity =============



    @Test
    void evaluateQuantity_WithNegativeResult_ShouldReturn2() {
        // Given
        Integer negativeValue = -5;

        Standard excellent = new Standard();
        excellent.setGrade("Отлично");
        excellent.setIntValue(new BigDecimal("15"));

        List<Standard> standards = List.of(excellent);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, negativeValue, 2);

        // Then
        assertEquals((short)2, result);
    }

    @Test
    void evaluateQuantity_WithResultAboveExcellent_ShouldReturn5() {
        // Given
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

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        // Then
        assertEquals((short)5, result);
    }

    @Test
    void evaluateQuantity_WithResultEqualToExcellent_ShouldReturn5() {
        // Given
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

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        // Then
        assertEquals((short)5, result);
    }

    @Test
    void evaluateQuantity_WithResultBetweenExcellentAndGood_ShouldReturn4() {
        // Given
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

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        // Then
        assertEquals((short)4, result);
    }

    @Test
    void evaluateQuantity_WithResultEqualToGood_ShouldReturn4() {
        // Given
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

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        // Then
        assertEquals((short)4, result);
    }

    @Test
    void evaluateQuantity_WithResultBetweenGoodAndSatisfactory_ShouldReturn3() {
        // Given
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

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        // Then
        assertEquals((short)3, result);
    }

    @Test
    void evaluateQuantity_WithResultEqualToSatisfactory_ShouldReturn3() {
        // Given
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

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        // Then
        assertEquals((short)3, result);
    }

    @Test
    void evaluateQuantity_WithResultBelowSatisfactory_ShouldReturn2() {
        // Given
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

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardQuantity, null, resultValue, 2);

        // Then
        assertEquals((short)2, result);
    }

    // ============= ТЕСТЫ ДЛЯ convertDurationToBigDecimal =============

    @Test
    void convertDurationToBigDecimal_WithNullDuration_ShouldReturnZero() {
        // This method is private, tested through evaluateMark
        // Just verifying that the service handles null properly
        BigDecimal timeValue = null;

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(List.of(standardExcellent));

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        // Then
        assertEquals((short)2, result);
    }

    @Test
    void convertDurationToBigDecimal_WithValidDuration_ShouldConvertCorrectly() {
        // Given
        BigDecimal timeValue = new BigDecimal("1.30"); // 1 минута 30 секунд
        List<Standard> standards = List.of(standardExcellent, standardGood, standardSatisfactory);

        when(standardRepository.findByNumberAndCourseOrderByGrade(anyShort(), anyShort()))
                .thenReturn(standards);

        // When
        Short result = standardEvaluationService.evaluateMark(testStandardTime, timeValue, null, 2);

        // Then
        assertEquals((short)5, result);
    }
}