package com.fakel.service;

import com.fakel.model.*;
import com.fakel.repository.TrainingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrainingAnalyticsServiceTest {

    @Mock
    private TrainingRepository trainingRepository;

    @InjectMocks
    private TrainingAnalyticsService analyticsService;

    private LocalDate now;
    private LocalDate from;
    private LocalDate to;
    private Long cadetId;
    private List<String> types;

    private Training training1;
    private Training training2;
    private Training training3;

    private ExercisesInTraining exercise1;
    private ExercisesInTraining exercise2;

    private Approach approach1;
    private Approach approach2;
    private Approach approach3;
    private Approach approach4;

    private ExerciseCatalog exerciseCatalog1;
    private ExerciseCatalog exerciseCatalog2;

    private ExerciseParameter paramWeight;
    private ExerciseParameter paramReps;
    private ExerciseParameter paramTime;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        from = now.minusMonths(1);
        to = now;
        cadetId = 1L;
        types = List.of("Сила", "Скорость");

        // Создаем каталоги упражнений
        exerciseCatalog1 = new ExerciseCatalog();
        exerciseCatalog1.setId(1L);
        exerciseCatalog1.setCode("BENCH_PRESS");
        exerciseCatalog1.setDescription("Жим лежа");
        exerciseCatalog1.setType("Сила");

        exerciseCatalog2 = new ExerciseCatalog();
        exerciseCatalog2.setId(2L);
        exerciseCatalog2.setCode("SQUAT");
        exerciseCatalog2.setDescription("Приседания");
        exerciseCatalog2.setType("Сила");

        // Создаем параметры упражнений
        paramWeight = new ExerciseParameter();
        paramWeight.setId(1L);
        paramWeight.setCode("вес");

        paramReps = new ExerciseParameter();
        paramReps.setId(2L);
        paramReps.setCode("повторения");

        paramTime = new ExerciseParameter();
        paramTime.setId(3L);
        paramTime.setCode("время");

        // Создаем подходы
        approach1 = new Approach();
        approach1.setId(1L);
        approach1.setExerciseParameter(paramWeight);
        approach1.setValue(new BigDecimal("100.0"));

        approach2 = new Approach();
        approach2.setId(2L);
        approach2.setExerciseParameter(paramReps);
        approach2.setValue(new BigDecimal("10.0"));

        approach3 = new Approach();
        approach3.setId(3L);
        approach3.setExerciseParameter(paramWeight);
        approach3.setValue(new BigDecimal("120.0"));

        approach4 = new Approach();
        approach4.setId(4L);
        approach4.setExerciseParameter(paramReps);
        approach4.setValue(new BigDecimal("8.0"));

        // Создаем упражнения в тренировке
        exercise1 = new ExercisesInTraining();
        exercise1.setId(1L);
        exercise1.setExerciseCatalog(exerciseCatalog1);
        exercise1.setApproaches(List.of(approach1, approach2));

        exercise2 = new ExercisesInTraining();
        exercise2.setId(2L);
        exercise2.setExerciseCatalog(exerciseCatalog2);
        exercise2.setApproaches(List.of(approach3, approach4));

        // Создаем тренировки
        training1 = new Training();
        training1.setId(1L);
        training1.setCadetId(cadetId);
        training1.setDate(now.minusDays(20));
        training1.setType("Сила");
        training1.setExercises(List.of(exercise1));

        training2 = new Training();
        training2.setId(2L);
        training2.setCadetId(cadetId);
        training2.setDate(now.minusDays(10));
        training2.setType("Сила");
        training2.setExercises(List.of(exercise2));

        training3 = new Training();
        training3.setId(3L);
        training3.setCadetId(cadetId);
        training3.setDate(now.minusDays(5));
        training3.setType("Скорость");
        training3.setExercises(new ArrayList<>());
    }

    // ============= ТЕСТЫ ДЛЯ getWeightProgress =============

    @Test
    void getWeightProgress_WithValidData_ShouldReturnWeightMap() {
        // Given
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75.5});
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(10)), 76.0});
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(5)), 75.8});

        when(trainingRepository.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(mockRows);

        // When
        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsKey(now.minusDays(20)));
        assertTrue(result.containsKey(now.minusDays(10)));
        assertTrue(result.containsKey(now.minusDays(5)));
    }

    @Test
    void getWeightProgress_WithInvalidCadetId_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getWeightProgress(null, from, to)
        );

        assertEquals("ID курсанта должен быть положительным числом", exception.getMessage());
    }

    @Test
    void getWeightProgress_WithNullFrom_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getWeightProgress(cadetId, null, to)
        );

        assertEquals("Дата начала не может быть null", exception.getMessage());
    }

    @Test
    void getWeightProgress_WithNullTo_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getWeightProgress(cadetId, from, null)
        );

        assertEquals("Дата окончания не может быть null", exception.getMessage());
    }

    @Test
    void getWeightProgress_WithInvalidDateRange_ShouldThrowException() {
        // Given
        LocalDate invalidFrom = to.plusDays(1);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getWeightProgress(cadetId, invalidFrom, to)
        );

        assertEquals("Дата начала не может быть позже даты окончания", exception.getMessage());
    }

    @Test
    void getWeightProgress_WhenRepositoryThrowsException_ShouldReturnEmptyMap() {
        // Given
        when(trainingRepository.getWeightProgress(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        // When
        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getWeightProgress_WithNullRows_ShouldReturnEmptyMap() {
        // Given
        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(null);

        // When
        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getWeightProgress_WithInvalidRowData_ShouldSkipInvalidRows() {
        // Given
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75.5}); // валидный
        mockRows.add(new Object[]{null, 76.0}); // null дата
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(5)), null}); // null вес
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(1)), -10.0}); // отрицательный вес

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        // When
        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // только первый
    }

    // ============= ТЕСТЫ ДЛЯ getExercisesProgress =============

    @Test
    void getExercisesProgress_WithValidData_ShouldReturnExercisesMap() {
        // Given
        List<Training> trainings = List.of(training1, training2, training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        // When
        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size()); // два упражнения с данными

        assertTrue(result.containsKey("Жим лежа"));
        assertTrue(result.containsKey("Приседания"));

        Map<String, Map<LocalDate, Double>> benchPress = result.get("Жим лежа");
        assertNotNull(benchPress);
        assertTrue(benchPress.containsKey("Вес (кг)"));
        assertTrue(benchPress.containsKey("Повторения"));
    }

    @Test
    void getExercisesProgress_WithNullTypes_ShouldIncludeAllTypes() {
        // Given
        List<Training> trainings = List.of(training1, training2, training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        // When
        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, null);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }


    @Test
    void getExercisesProgress_WhenRepositoryThrowsException_ShouldReturnEmptyMap() {
        // Given
        when(trainingRepository.findByCadetIdAndDateBetween(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        // When
        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getExercisesProgress_WithNoTrainings_ShouldReturnEmptyMap() {
        // Given
        when(trainingRepository.findByCadetIdAndDateBetween(anyLong(), any(), any()))
                .thenReturn(new ArrayList<>());

        // When
        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============= ТЕСТЫ ДЛЯ getTonnageProgress =============

    @Test
    void getTonnageProgress_WithValidData_ShouldReturnTonnageMap() {
        // Given
        List<Training> trainings = List.of(training1, training2);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        // When
        Map<String, Map<LocalDate, Double>> result =
                analyticsService.getTonnageProgress(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Жим лежа: вес 100 * повторения 10 = 1000
        Map<LocalDate, Double> benchTonnage = result.get("Жим лежа");
        assertNotNull(benchTonnage);
        assertEquals(1000.0, benchTonnage.get(now.minusDays(20)));

        // Приседания: вес 120 * повторения 8 = 960
        Map<LocalDate, Double> squatTonnage = result.get("Приседания");
        assertNotNull(squatTonnage);
        assertEquals(960.0, squatTonnage.get(now.minusDays(10)));
    }

    @Test
    void getTonnageProgress_WithMissingWeightOrReps_ShouldSkip() {
        // Given
        // Убираем вес из первого подхода
        exercise1.setApproaches(List.of(approach2)); // только повторения, без веса

        List<Training> trainings = List.of(training1, training2);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        // When
        Map<String, Map<LocalDate, Double>> result =
                analyticsService.getTonnageProgress(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // только приседания
    }

    // ============= ТЕСТЫ ДЛЯ getVolumeProgress =============

    @Test
    void getVolumeProgress_WithValidData_ShouldReturnVolumeMap() {
        // Given
        List<Training> trainings = List.of(training1, training2, training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        // When
        Map<LocalDate, Integer> result =
                analyticsService.getVolumeProgress(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(2, result.get(now.minusDays(20))); // 2 подхода
        assertEquals(2, result.get(now.minusDays(10))); // 2 подхода
        assertEquals(0, result.get(now.minusDays(5))); // 0 подходов
    }

    @Test
    void getVolumeProgress_WithTrainingWithoutExercises_ShouldReturnZero() {
        // Given
        List<Training> trainings = List.of(training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        // When
        Map<LocalDate, Integer> result =
                analyticsService.getVolumeProgress(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, result.get(now.minusDays(5)));
    }

    // ============= ТЕСТЫ ДЛЯ getSummary =============

    @Test
    void getSummary_WithValidData_ShouldReturnSummary() {
        // Given
        List<Training> trainings = List.of(training1, training2, training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        // When
        Map<String, Object> result = analyticsService.getSummary(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertEquals(3, result.get("totalTrainings"));
        assertEquals(ChronoUnit.DAYS.between(from, to) + 1, result.get("totalDays"));
        assertEquals(4, result.get("totalApproaches")); // 2+2

        Map<String, Long> byType = (Map<String, Long>) result.get("byType");
        assertEquals(2, byType.size());
        assertEquals(2, byType.get("Сила"));
        assertEquals(1, byType.get("Скорость"));
    }

    @Test
    void getSummary_WithNoTrainings_ShouldReturnEmptySummary() {
        // Given
        when(trainingRepository.findByCadetIdAndDateBetween(anyLong(), any(), any()))
                .thenReturn(new ArrayList<>());

        // When
        Map<String, Object> result = analyticsService.getSummary(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertEquals(0, result.get("totalTrainings"));
        assertEquals(ChronoUnit.DAYS.between(from, to) + 1, result.get("totalDays"));
        assertEquals(0.0, result.get("totalTonnage"));
        assertEquals(0, result.get("totalApproaches"));
    }

    @Test
    void getSummary_WhenRepositoryThrowsException_ShouldReturnEmptySummary() {
        // Given
        when(trainingRepository.findByCadetIdAndDateBetween(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        // When
        Map<String, Object> result = analyticsService.getSummary(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertEquals(0, result.get("totalTrainings"));
    }

    // ============= ТЕСТЫ ДЛЯ getParameterDisplayName =============

    @Test
    void getParameterDisplayName_WithKnownCodes_ShouldReturnDisplayName() {
        // Тестируем через публичные методы
        List<Training> trainings = List.of(training1);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        assertNotNull(result);
        Map<String, Map<LocalDate, Double>> benchPress = result.get("Жим лежа");
        assertTrue(benchPress.containsKey("Вес (кг)"));
        assertTrue(benchPress.containsKey("Повторения"));
    }

    @Test
    void getParameterDisplayName_WithUnknownCode_ShouldReturnCode() {
        // Создаем параметр с неизвестным кодом
        ExerciseParameter unknownParam = new ExerciseParameter();
        unknownParam.setId(4L);
        unknownParam.setCode("unknown");

        Approach unknownApproach = new Approach();
        unknownApproach.setId(5L);
        unknownApproach.setExerciseParameter(unknownParam);
        unknownApproach.setValue(new BigDecimal("100.0"));

        exercise1.setApproaches(List.of(unknownApproach));

        List<Training> trainings = List.of(training1);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        assertNotNull(result);
        Map<String, Map<LocalDate, Double>> benchPress = result.get("Жим лежа");
        assertTrue(benchPress.containsKey("unknown"));
    }

    // ============= ТЕСТЫ ДЛЯ extractDate =============

    @Test
    void extractDate_WithSqlDate_ShouldReturnLocalDate() {
        // Тестируем через getWeightProgress
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75.5});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertTrue(result.containsKey(now.minusDays(20)));
    }

    @Test
    void extractDate_WithUtilDate_ShouldReturnLocalDate() {
        // Тестируем через getWeightProgress
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{new java.util.Date(), 75.5});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
    }

    // ============= ТЕСТЫ ДЛЯ extractDouble =============

    @Test
    void extractDouble_WithBigDecimal_ShouldReturnDouble() {
        // Тестируем через getWeightProgress
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), new BigDecimal("75.5")});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertEquals(75.5, result.get(now.minusDays(20)));
    }

    @Test
    void extractDouble_WithInteger_ShouldReturnDouble() {
        // Тестируем через getWeightProgress
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertEquals(75.0, result.get(now.minusDays(20)));
    }

    @Test
    void extractDouble_WithLong_ShouldReturnDouble() {
        // Тестируем через getWeightProgress
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75L});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertEquals(75.0, result.get(now.minusDays(20)));
    }

    @Test
    void extractDouble_WithFloat_ShouldReturnDouble() {
        // Тестируем через getWeightProgress
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75.5f});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertEquals(75.5, result.get(now.minusDays(20)), 0.01);
    }

    @Test
    void extractDouble_WithNull_ShouldReturnNull() {
        // Тестируем через getWeightProgress
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), null});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(0, result.size()); // null пропускается
    }
}