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

        paramWeight = new ExerciseParameter();
        paramWeight.setId(1L);
        paramWeight.setCode("вес");

        paramReps = new ExerciseParameter();
        paramReps.setId(2L);
        paramReps.setCode("повторения");

        paramTime = new ExerciseParameter();
        paramTime.setId(3L);
        paramTime.setCode("время");

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

        exercise1 = new ExercisesInTraining();
        exercise1.setId(1L);
        exercise1.setExerciseCatalog(exerciseCatalog1);
        exercise1.setApproaches(List.of(approach1, approach2));

        exercise2 = new ExercisesInTraining();
        exercise2.setId(2L);
        exercise2.setExerciseCatalog(exerciseCatalog2);
        exercise2.setApproaches(List.of(approach3, approach4));

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

    @Test
    void getWeightProgress_WithValidData_ShouldReturnWeightMap() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75.5});
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(10)), 76.0});
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(5)), 75.8});

        when(trainingRepository.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.containsKey(now.minusDays(20)));
        assertTrue(result.containsKey(now.minusDays(10)));
        assertTrue(result.containsKey(now.minusDays(5)));
    }

    @Test
    void getWeightProgress_WithInvalidCadetId_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getWeightProgress(null, from, to)
        );

        assertEquals("ID курсанта должен быть положительным числом", exception.getMessage());
    }

    @Test
    void getWeightProgress_WithNullFrom_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getWeightProgress(cadetId, null, to)
        );

        assertEquals("Дата начала не может быть null", exception.getMessage());
    }

    @Test
    void getWeightProgress_WithNullTo_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getWeightProgress(cadetId, from, null)
        );

        assertEquals("Дата окончания не может быть null", exception.getMessage());
    }

    @Test
    void getWeightProgress_WithInvalidDateRange_ShouldThrowException() {
        LocalDate invalidFrom = to.plusDays(1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getWeightProgress(cadetId, invalidFrom, to)
        );

        assertEquals("Дата начала не может быть позже даты окончания", exception.getMessage());
    }

    @Test
    void getWeightProgress_WhenRepositoryThrowsException_ShouldReturnEmptyMap() {
        when(trainingRepository.getWeightProgress(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getWeightProgress_WithNullRows_ShouldReturnEmptyMap() {
        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(null);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getWeightProgress_WithInvalidRowData_ShouldSkipInvalidRows() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75.5});
        mockRows.add(new Object[]{null, 76.0});
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(5)), null});
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(1)), -10.0});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getExercisesProgress_WithValidData_ShouldReturnExercisesMap() {
        List<Training> trainings = List.of(training1, training2, training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertTrue(result.containsKey("Жим лежа"));
        assertTrue(result.containsKey("Приседания"));

        Map<String, Map<LocalDate, Double>> benchPress = result.get("Жим лежа");
        assertNotNull(benchPress);
        assertTrue(benchPress.containsKey("Вес (кг)"));
        assertTrue(benchPress.containsKey("Повторения"));
    }

    @Test
    void getExercisesProgress_WithNullTypes_ShouldIncludeAllTypes() {
        List<Training> trainings = List.of(training1, training2, training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, null);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getExercisesProgress_WhenRepositoryThrowsException_ShouldReturnEmptyMap() {
        when(trainingRepository.findByCadetIdAndDateBetween(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getExercisesProgress_WithNoTrainings_ShouldReturnEmptyMap() {
        when(trainingRepository.findByCadetIdAndDateBetween(anyLong(), any(), any()))
                .thenReturn(new ArrayList<>());

        Map<String, Map<String, Map<LocalDate, Double>>> result =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getTonnageProgress_WithValidData_ShouldReturnTonnageMap() {
        List<Training> trainings = List.of(training1, training2);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<String, Map<LocalDate, Double>> result =
                analyticsService.getTonnageProgress(cadetId, from, to, types);

        assertNotNull(result);
        assertEquals(2, result.size());

        Map<LocalDate, Double> benchTonnage = result.get("Жим лежа");
        assertNotNull(benchTonnage);
        assertEquals(1000.0, benchTonnage.get(now.minusDays(20)));

        Map<LocalDate, Double> squatTonnage = result.get("Приседания");
        assertNotNull(squatTonnage);
        assertEquals(960.0, squatTonnage.get(now.minusDays(10)));
    }

    @Test
    void getTonnageProgress_WithMissingWeightOrReps_ShouldSkip() {
        exercise1.setApproaches(List.of(approach2));

        List<Training> trainings = List.of(training1, training2);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<String, Map<LocalDate, Double>> result =
                analyticsService.getTonnageProgress(cadetId, from, to, types);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getVolumeProgress_WithValidData_ShouldReturnVolumeMap() {
        List<Training> trainings = List.of(training1, training2, training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<LocalDate, Integer> result =
                analyticsService.getVolumeProgress(cadetId, from, to, types);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(2, result.get(now.minusDays(20)));
        assertEquals(2, result.get(now.minusDays(10)));
        assertEquals(0, result.get(now.minusDays(5)));
    }

    @Test
    void getVolumeProgress_WithTrainingWithoutExercises_ShouldReturnZero() {
        List<Training> trainings = List.of(training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<LocalDate, Integer> result =
                analyticsService.getVolumeProgress(cadetId, from, to, types);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(0, result.get(now.minusDays(5)));
    }

    @Test
    void getSummary_WithValidData_ShouldReturnSummary() {
        List<Training> trainings = List.of(training1, training2, training3);
        when(trainingRepository.findByCadetIdAndDateBetween(eq(cadetId), eq(from), eq(to)))
                .thenReturn(trainings);

        Map<String, Object> result = analyticsService.getSummary(cadetId, from, to, types);

        assertNotNull(result);
        assertEquals(3, result.get("totalTrainings"));
        assertEquals(ChronoUnit.DAYS.between(from, to) + 1, result.get("totalDays"));
        assertEquals(4, result.get("totalApproaches"));

        Map<String, Long> byType = (Map<String, Long>) result.get("byType");
        assertEquals(2, byType.size());
        assertEquals(2, byType.get("Сила"));
        assertEquals(1, byType.get("Скорость"));
    }

    @Test
    void getSummary_WithNoTrainings_ShouldReturnEmptySummary() {
        when(trainingRepository.findByCadetIdAndDateBetween(anyLong(), any(), any()))
                .thenReturn(new ArrayList<>());

        Map<String, Object> result = analyticsService.getSummary(cadetId, from, to, types);

        assertNotNull(result);
        assertEquals(0, result.get("totalTrainings"));
        assertEquals(ChronoUnit.DAYS.between(from, to) + 1, result.get("totalDays"));
        assertEquals(0.0, result.get("totalTonnage"));
        assertEquals(0, result.get("totalApproaches"));
    }

    @Test
    void getSummary_WhenRepositoryThrowsException_ShouldReturnEmptySummary() {
        when(trainingRepository.findByCadetIdAndDateBetween(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        Map<String, Object> result = analyticsService.getSummary(cadetId, from, to, types);

        assertNotNull(result);
        assertEquals(0, result.get("totalTrainings"));
    }

    @Test
    void getParameterDisplayName_WithKnownCodes_ShouldReturnDisplayName() {
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

    @Test
    void extractDate_WithSqlDate_ShouldReturnLocalDate() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75.5});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertTrue(result.containsKey(now.minusDays(20)));
    }

    @Test
    void extractDate_WithUtilDate_ShouldReturnLocalDate() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{new java.util.Date(), 75.5});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
    }

    @Test
    void extractDouble_WithBigDecimal_ShouldReturnDouble() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), new BigDecimal("75.5")});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertEquals(75.5, result.get(now.minusDays(20)));
    }

    @Test
    void extractDouble_WithInteger_ShouldReturnDouble() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertEquals(75.0, result.get(now.minusDays(20)));
    }

    @Test
    void extractDouble_WithLong_ShouldReturnDouble() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75L});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertEquals(75.0, result.get(now.minusDays(20)));
    }

    @Test
    void extractDouble_WithFloat_ShouldReturnDouble() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), 75.5f});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(1, result.size());
        assertEquals(75.5, result.get(now.minusDays(20)), 0.01);
    }

    @Test
    void extractDouble_WithNull_ShouldReturnNull() {
        List<Object[]> mockRows = new ArrayList<>();
        mockRows.add(new Object[]{Date.valueOf(now.minusDays(20)), null});

        when(trainingRepository.getWeightProgress(anyLong(), any(), any())).thenReturn(mockRows);

        Map<LocalDate, Double> result = analyticsService.getWeightProgress(cadetId, from, to);

        assertEquals(0, result.size());
    }
}