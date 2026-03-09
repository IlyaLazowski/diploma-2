package com.fakel.service;

import com.fakel.model.Cadet;
import com.fakel.model.University;
import com.fakel.model.User;
import com.fakel.repository.CadetRepository;
import org.jfree.chart.JFreeChart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock
    private TrainingAnalyticsService analyticsService;  // ← ИСПРАВЛЕНО!

    @Mock
    private ChartService chartService;

    @Mock
    private PdfService pdfService;

    @Mock
    private CadetRepository cadetRepository;

    @InjectMocks
    private ReportService reportService;

    private Cadet testCadet;
    private User testUser;
    private University testUniversity;
    private LocalDate now;
    private LocalDate from;
    private LocalDate to;
    private List<String> types;
    private Map<String, Object> summary;
    private Map<LocalDate, Double> weightData;
    private Map<String, Map<String, Map<LocalDate, Double>>> exercisesData;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        from = now.minusMonths(1);
        to = now;
        types = List.of("Сила", "Скорость");

        // Создаем университет
        testUniversity = new University();
        testUniversity.setId(1L);
        testUniversity.setCode("Военная академия");

        // Создаем пользователя
        testUser = new User();
        testUser.setId(1L);
        testUser.setLastName("Иванов");
        testUser.setFirstName("Иван");
        testUser.setPatronymic("Иванович");
        testUser.setUniversity(testUniversity);

        // Создаем курсанта
        testCadet = new Cadet();
        testCadet.setUserId(1L);
        testCadet.setUser(testUser);
        testCadet.setGroupId(1L);

        // Создаем сводную статистику
        summary = new HashMap<>();
        summary.put("totalTrainings", 10);
        summary.put("totalDays", 30);
        summary.put("totalTonnage", 1500.5);

        Map<String, Long> byType = new HashMap<>();
        byType.put("Сила", 6L);
        byType.put("Скорость", 4L);
        summary.put("byType", byType);

        // Создаем данные о весе
        weightData = new LinkedHashMap<>();
        weightData.put(now.minusDays(30), 75.5);
        weightData.put(now.minusDays(20), 76.0);
        weightData.put(now.minusDays(10), 75.8);
        weightData.put(now, 76.2);

        // Создаем данные об упражнениях
        exercisesData = new HashMap<>();

        Map<String, Map<LocalDate, Double>> exercise1Data = new HashMap<>();
        Map<LocalDate, Double> param1Data = new LinkedHashMap<>();
        param1Data.put(now.minusDays(30), 100.0);
        param1Data.put(now.minusDays(20), 105.0);
        param1Data.put(now.minusDays(10), 102.0);
        param1Data.put(now, 110.0);
        exercise1Data.put("Вес (кг)", param1Data);

        Map<LocalDate, Double> param2Data = new LinkedHashMap<>();
        param2Data.put(now.minusDays(30), 10.0);
        param2Data.put(now.minusDays(20), 12.0);
        param2Data.put(now.minusDays(10), 11.0);
        param2Data.put(now, 15.0);
        exercise1Data.put("Повторения", param2Data);

        exercisesData.put("Жим лежа", exercise1Data);
    }

    // ============= ТЕСТЫ ДЛЯ generateTrainingReport =============

    @Test
    void generateTrainingReport_WithValidData_ShouldGenerateReport() throws Exception {
        // Given
        Long cadetId = 1L;
        byte[] expectedPdf = "PDF content".getBytes();

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                eq("Военная академия"),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When
        byte[] result = reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);

        verify(analyticsService, times(1)).getSummary(cadetId, from, to, types);
        verify(analyticsService, times(1)).getWeightProgress(cadetId, from, to);
        verify(analyticsService, times(1)).getExercisesProgress(cadetId, from, to, types);
        verify(chartService, times(1)).createWeightChart(anyMap());
        verify(chartService, times(1)).createExerciseChart(anyString(), anyMap());
        verify(pdfService, times(1)).buildTrainingPdf(
                eq(cadetId), anyString(), eq("1"), eq("Военная академия"),
                eq(from), eq(to), eq(types), anyMap(), eq(mockWeightChart), anyMap()
        );
    }

    @Test
    void generateTrainingReport_WithNullCadetId_ShouldThrowException() {
        // Given
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.generateTrainingReport(null, from, to, types)
        );

        assertEquals("ID курсанта должен быть положительным числом", exception.getMessage());
        verify(cadetRepository, never()).findById(any());
    }

    @Test
    void generateTrainingReport_WithInvalidCadetId_ShouldThrowException() {
        // Given
        Long cadetId = 0L;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.generateTrainingReport(cadetId, from, to, types)
        );

        assertEquals("ID курсанта должен быть положительным числом", exception.getMessage());
    }

    @Test
    void generateTrainingReport_WithNullFrom_ShouldThrowException() {
        // Given
        Long cadetId = 1L;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.generateTrainingReport(cadetId, null, to, types)
        );

        assertEquals("Дата начала не может быть null", exception.getMessage());
    }

    @Test
    void generateTrainingReport_WithNullTo_ShouldThrowException() {
        // Given
        Long cadetId = 1L;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.generateTrainingReport(cadetId, from, null, types)
        );

        assertEquals("Дата окончания не может быть null", exception.getMessage());
    }

    @Test
    void generateTrainingReport_WithInvalidDateRange_ShouldThrowException() {
        // Given
        Long cadetId = 1L;
        LocalDate invalidFrom = to.plusDays(1);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.generateTrainingReport(cadetId, invalidFrom, to, types)
        );

        assertEquals("Дата начала не может быть позже даты окончания", exception.getMessage());
    }

    @Test
    void generateTrainingReport_WithInvalidType_ShouldThrowException() {
        // Given
        Long cadetId = 1L;
        List<String> invalidTypes = List.of("Сила", "Неверный тип");

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.generateTrainingReport(cadetId, from, to, invalidTypes)
        );

        assertTrue(exception.getMessage().contains("Некорректный тип тренировки"));
    }

    @Test
    void generateTrainingReport_WhenCadetNotFound_ShouldThrowException() {
        // Given
        Long cadetId = 999L;
        when(cadetRepository.findById(cadetId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reportService.generateTrainingReport(cadetId, from, to, types)
        );

        assertEquals("Курсант не найден с id: 999", exception.getMessage());
    }

    @Test
    void generateTrainingReport_WhenCadetHasNoUser_ShouldThrowException() {
        // Given
        Long cadetId = 1L;
        testCadet.setUser(null);
        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reportService.generateTrainingReport(cadetId, from, to, types)
        );

        assertEquals("У курсанта отсутствуют данные пользователя", exception.getMessage());
    }

    @Test
    void generateTrainingReport_WhenCadetHasNoGroup_ShouldThrowException() {
        // Given
        Long cadetId = 1L;
        testCadet.setGroupId(null);
        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reportService.generateTrainingReport(cadetId, from, to, types)
        );

        assertEquals("У курсанта не указана группа", exception.getMessage());
    }

    @Test
    void generateTrainingReport_WhenCadetHasNoUniversity_ShouldUseDefault() throws Exception {
        // Given
        Long cadetId = 1L;
        testUser.setUniversity(null);
        byte[] expectedPdf = "PDF content".getBytes();

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        ArgumentCaptor<String> uniCaptor = ArgumentCaptor.forClass(String.class);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                uniCaptor.capture(),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When
        byte[] result = reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);
        assertEquals("Военная академия", uniCaptor.getValue());
    }

    @Test
    void generateTrainingReport_WhenAnalyticsServiceThrowsException_ShouldThrowRuntimeException() {
        // Given
        Long cadetId = 1L;
        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types)))
                .thenThrow(new RuntimeException("Analytics error"));

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reportService.generateTrainingReport(cadetId, from, to, types)
        );

        assertTrue(exception.getMessage().contains("Ошибка при получении сводной статистики"));
    }

    @Test
    void generateTrainingReport_WhenWeightProgressThrowsException_ShouldUseEmptyMap() throws Exception {
        // Given
        Long cadetId = 1L;
        byte[] expectedPdf = "PDF content".getBytes();

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to)))
                .thenThrow(new RuntimeException("Weight error"));
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                eq("Военная академия"),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                isNull(),
                anyMap()
        );

        // When
        byte[] result = reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);

        verify(chartService, never()).createWeightChart(anyMap());
    }

    @Test
    void generateTrainingReport_WhenExercisesProgressThrowsException_ShouldUseEmptyMap() throws Exception {
        // Given
        Long cadetId = 1L;
        byte[] expectedPdf = "PDF content".getBytes();

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types)))
                .thenThrow(new RuntimeException("Exercises error"));

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                eq("Военная академия"),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When
        byte[] result = reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);

        verify(chartService, never()).createExerciseChart(anyString(), anyMap());
    }

    @Test
    void generateTrainingReport_WithInsufficientWeightData_ShouldNotCreateWeightChart() throws Exception {
        // Given
        Long cadetId = 1L;
        byte[] expectedPdf = "PDF content".getBytes();

        Map<LocalDate, Double> singlePointData = new HashMap<>();
        singlePointData.put(now, 75.5);

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(singlePointData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                eq("Военная академия"),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                isNull(),
                anyMap()
        );

        // When
        byte[] result = reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);

        verify(chartService, never()).createWeightChart(anyMap());
    }

    @Test
    void generateTrainingReport_WithExerciseWithoutEnoughData_ShouldSkipChart() throws Exception {
        // Given
        Long cadetId = 1L;
        byte[] expectedPdf = "PDF content".getBytes();

        Map<String, Map<String, Map<LocalDate, Double>>> incompleteExercisesData = new HashMap<>();

        Map<String, Map<LocalDate, Double>> exerciseData = new HashMap<>();
        Map<LocalDate, Double> paramData = new LinkedHashMap<>();
        paramData.put(now, 100.0); // только одна точка
        exerciseData.put("Вес (кг)", paramData);
        incompleteExercisesData.put("Приседания", exerciseData);

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types)))
                .thenReturn(incompleteExercisesData);

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                eq("Военная академия"),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When
        byte[] result = reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertNotNull(result);
        assertArrayEquals(expectedPdf, result);

        verify(chartService, never()).createExerciseChart(anyString(), anyMap());
    }

    @Test
    void generateTrainingReport_WhenPdfServiceThrowsException_ShouldThrowRuntimeException() throws Exception {
        // Given
        Long cadetId = 1L;

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        doThrow(new RuntimeException("PDF error")).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                eq("Военная академия"),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reportService.generateTrainingReport(cadetId, from, to, types)
        );

        assertTrue(exception.getMessage().contains("Ошибка при генерации PDF отчета"));
    }

    // ============= ТЕСТЫ ДЛЯ buildFullName =============

    @Test
    void buildFullName_WithAllFields_ShouldReturnFullName() throws Exception {
        // Given
        Long cadetId = 1L;
        byte[] expectedPdf = "PDF content".getBytes();

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                nameCaptor.capture(),
                eq("1"),
                eq("Военная академия"),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When
        reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertEquals("Иванов Иван Иванович", nameCaptor.getValue());
    }

    @Test
    void buildFullName_WithoutPatronymic_ShouldReturnNameWithoutPatronymic() throws Exception {
        // Given
        Long cadetId = 1L;
        testUser.setPatronymic(null);
        byte[] expectedPdf = "PDF content".getBytes();

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                nameCaptor.capture(),
                eq("1"),
                eq("Военная академия"),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When
        reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertEquals("Иванов Иван", nameCaptor.getValue());
    }

    // ============= ТЕСТЫ ДЛЯ getUniversityName =============

    @Test
    void getUniversityName_WhenUserHasUniversity_ShouldReturnUniversityCode() throws Exception {
        // Given
        Long cadetId = 1L;
        byte[] expectedPdf = "PDF content".getBytes();

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        ArgumentCaptor<String> uniCaptor = ArgumentCaptor.forClass(String.class);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                uniCaptor.capture(),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When
        reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertEquals("Военная академия", uniCaptor.getValue());
    }

    @Test
    void getUniversityName_WhenUserHasNoUniversity_ShouldReturnDefault() throws Exception {
        // Given
        Long cadetId = 1L;
        testUser.setUniversity(null);
        byte[] expectedPdf = "PDF content".getBytes();

        when(cadetRepository.findById(cadetId)).thenReturn(Optional.of(testCadet));
        when(analyticsService.getSummary(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(summary);
        when(analyticsService.getWeightProgress(eq(cadetId), eq(from), eq(to))).thenReturn(weightData);
        when(analyticsService.getExercisesProgress(eq(cadetId), eq(from), eq(to), eq(types))).thenReturn(exercisesData);

        JFreeChart mockWeightChart = mock(JFreeChart.class);
        when(chartService.createWeightChart(anyMap())).thenReturn(mockWeightChart);

        JFreeChart mockExerciseChart = mock(JFreeChart.class);
        when(chartService.createExerciseChart(anyString(), anyMap())).thenReturn(mockExerciseChart);

        ArgumentCaptor<String> uniCaptor = ArgumentCaptor.forClass(String.class);

        doReturn(expectedPdf).when(pdfService).buildTrainingPdf(
                eq(cadetId),
                anyString(),
                eq("1"),
                uniCaptor.capture(),
                eq(from),
                eq(to),
                eq(types),
                anyMap(),
                eq(mockWeightChart),
                anyMap()
        );

        // When
        reportService.generateTrainingReport(cadetId, from, to, types);

        // Then
        assertEquals("Военная академия", uniCaptor.getValue());
    }


}