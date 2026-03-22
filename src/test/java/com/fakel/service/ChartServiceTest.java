package com.fakel.service;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ChartServiceTest {

    private ChartService chartService;
    private LocalDate now;
    private Map<LocalDate, Double> validWeightData;
    private Map<String, Map<LocalDate, Double>> validExerciseData;

    @BeforeEach
    void setUp() {
        chartService = new ChartService();
        now = LocalDate.now();

        validWeightData = new LinkedHashMap<>();
        validWeightData.put(now.minusDays(5), 75.5);
        validWeightData.put(now.minusDays(4), 75.8);
        validWeightData.put(now.minusDays(3), 76.2);
        validWeightData.put(now.minusDays(2), 75.9);
        validWeightData.put(now.minusDays(1), 76.1);

        validExerciseData = new HashMap<>();

        Map<LocalDate, Double> param1Data = new LinkedHashMap<>();
        param1Data.put(now.minusDays(5), 100.0);
        param1Data.put(now.minusDays(4), 105.0);
        param1Data.put(now.minusDays(3), 102.0);
        param1Data.put(now.minusDays(2), 110.0);
        param1Data.put(now.minusDays(1), 108.0);

        Map<LocalDate, Double> param2Data = new LinkedHashMap<>();
        param2Data.put(now.minusDays(5), 10.0);
        param2Data.put(now.minusDays(4), 12.0);
        param2Data.put(now.minusDays(3), 11.0);
        param2Data.put(now.minusDays(2), 15.0);
        param2Data.put(now.minusDays(1), 14.0);

        validExerciseData.put("Вес (кг)", param1Data);
        validExerciseData.put("Повторения", param2Data);
    }

    @Test
    void createExerciseChart_WithValidData_ShouldReturnChart() {
        String exerciseName = "Жим лежа";

        JFreeChart chart = chartService.createExerciseChart(exerciseName, validExerciseData);

        assertNotNull(chart);
        assertEquals(exerciseName, chart.getTitle().getText());

        XYPlot plot = chart.getXYPlot();
        assertNotNull(plot);
        assertEquals(2, plot.getDataset().getSeriesCount());

        ValueAxis rangeAxis = plot.getRangeAxis();
        assertNotNull(rangeAxis);
        assertTrue(rangeAxis.getLabel().contains("Вес") || rangeAxis.getLabel().contains("Повторения") ||
                rangeAxis.getLabel().equals("Значение"));
    }

    @Test
    void createExerciseChart_WithSingleParameter_ShouldUseParameterNameAsYAxisLabel() {
        String exerciseName = "Приседания";
        Map<String, Map<LocalDate, Double>> singleParamData = new HashMap<>();
        singleParamData.put("Вес (кг)", validExerciseData.get("Вес (кг)"));

        JFreeChart chart = chartService.createExerciseChart(exerciseName, singleParamData);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        assertEquals("Вес (кг)", plot.getRangeAxis().getLabel());
    }

    @Test
    void createExerciseChart_WithEmptyExerciseName_ShouldThrowException() {
        String exerciseName = "   ";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createExerciseChart(exerciseName, validExerciseData)
        );

        assertEquals("Название упражнения не может быть пустым", exception.getMessage());
    }

    @Test
    void createExerciseChart_WithNullExerciseName_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createExerciseChart(null, validExerciseData)
        );

        assertEquals("Название упражнения не может быть пустым", exception.getMessage());
    }

    @Test
    void createExerciseChart_WithNullData_ShouldThrowException() {
        String exerciseName = "Жим лежа";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createExerciseChart(exerciseName, null)
        );

        assertEquals("Данные для графика не могут быть пустыми", exception.getMessage());
    }

    @Test
    void createExerciseChart_WithEmptyData_ShouldThrowException() {
        String exerciseName = "Жим лежа";
        Map<String, Map<LocalDate, Double>> emptyData = new HashMap<>();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createExerciseChart(exerciseName, emptyData)
        );

        assertEquals("Данные для графика не могут быть пустыми", exception.getMessage());
    }

    @Test
    void createExerciseChart_WithNullParameterName_ShouldSkipParameter() {
        String exerciseName = "Жим лежа";
        Map<String, Map<LocalDate, Double>> dataWithNullParam = new HashMap<>();
        dataWithNullParam.put(null, validExerciseData.get("Вес (кг)"));
        dataWithNullParam.put("Повторения", validExerciseData.get("Повторения"));

        JFreeChart chart = chartService.createExerciseChart(exerciseName, dataWithNullParam);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        assertEquals(1, plot.getDataset().getSeriesCount());
    }

    @Test
    void createExerciseChart_WithEmptyParameterName_ShouldSkipParameter() {
        String exerciseName = "Жим лежа";
        Map<String, Map<LocalDate, Double>> dataWithEmptyParam = new HashMap<>();
        dataWithEmptyParam.put("   ", validExerciseData.get("Вес (кг)"));
        dataWithEmptyParam.put("Повторения", validExerciseData.get("Повторения"));

        JFreeChart chart = chartService.createExerciseChart(exerciseName, dataWithEmptyParam);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        assertEquals(1, plot.getDataset().getSeriesCount());
    }

    @Test
    void createExerciseChart_WithEmptyParameterData_ShouldSkipParameter() {
        String exerciseName = "Жим лежа";
        Map<String, Map<LocalDate, Double>> dataWithEmptyData = new HashMap<>();
        dataWithEmptyData.put("Вес (кг)", new HashMap<>());
        dataWithEmptyData.put("Повторения", validExerciseData.get("Повторения"));

        JFreeChart chart = chartService.createExerciseChart(exerciseName, dataWithEmptyData);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        assertEquals(1, plot.getDataset().getSeriesCount());
    }

    @Test
    void createExerciseChart_WithNullDate_ShouldSkipPoint() {
        String exerciseName = "Жим лежа";
        Map<String, Map<LocalDate, Double>> dataWithNullDate = new HashMap<>();

        Map<LocalDate, Double> paramData = new LinkedHashMap<>();
        paramData.put(now.minusDays(5), 100.0);
        paramData.put(null, 105.0);
        paramData.put(now.minusDays(3), 102.0);

        dataWithNullDate.put("Вес (кг)", paramData);

        JFreeChart chart = chartService.createExerciseChart(exerciseName, dataWithNullDate);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        assertEquals(1, plot.getDataset().getSeriesCount());
    }

    @Test
    void createExerciseChart_WithNullValue_ShouldSkipPoint() {
        String exerciseName = "Жим лежа";
        Map<String, Map<LocalDate, Double>> dataWithNullValue = new HashMap<>();

        Map<LocalDate, Double> paramData = new LinkedHashMap<>();
        paramData.put(now.minusDays(5), 100.0);
        paramData.put(now.minusDays(4), null);
        paramData.put(now.minusDays(3), 102.0);

        dataWithNullValue.put("Вес (кг)", paramData);

        JFreeChart chart = chartService.createExerciseChart(exerciseName, dataWithNullValue);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        assertEquals(1, plot.getDataset().getSeriesCount());
    }

    @Test
    void createExerciseChart_WithFutureDate_ShouldThrowException() {
        String exerciseName = "Жим лежа";
        Map<String, Map<LocalDate, Double>> dataWithFutureDate = new HashMap<>();

        Map<LocalDate, Double> paramData = new LinkedHashMap<>();
        paramData.put(now.minusDays(5), 100.0);
        paramData.put(now.plusDays(1), 105.0);
        paramData.put(now.minusDays(3), 102.0);

        dataWithFutureDate.put("Вес (кг)", paramData);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createExerciseChart(exerciseName, dataWithFutureDate)
        );

        assertTrue(exception.getMessage().contains("Дата не может быть в будущем"));
    }

    @Test
    void createExerciseChart_WithNoValidSeries_ShouldThrowException() {
        String exerciseName = "Жим лежа";
        Map<String, Map<LocalDate, Double>> dataWithOnlyInvalid = new HashMap<>();

        Map<LocalDate, Double> paramData = new HashMap<>();
        paramData.put(now.minusDays(5), null);
        dataWithOnlyInvalid.put("Вес (кг)", paramData);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createExerciseChart(exerciseName, dataWithOnlyInvalid)
        );

        assertEquals("Нет данных для построения графика", exception.getMessage());
    }

    @Test
    void createWeightChart_WithValidData_ShouldReturnChart() {
        JFreeChart chart = chartService.createWeightChart(validWeightData);

        assertNotNull(chart);
        assertEquals("Динамика веса", chart.getTitle().getText());

        XYPlot plot = chart.getXYPlot();
        assertNotNull(plot);
        assertEquals(1, plot.getDataset().getSeriesCount());

        ValueAxis rangeAxis = plot.getRangeAxis();
        assertNotNull(rangeAxis);
        assertEquals("Вес (кг)", rangeAxis.getLabel());
    }

    @Test
    void createWeightChart_WithSingleDataPoint_ShouldCreateChart() {
        Map<LocalDate, Double> singlePointData = new HashMap<>();
        singlePointData.put(now.minusDays(1), 75.5);

        JFreeChart chart = chartService.createWeightChart(singlePointData);

        assertNotNull(chart);
        assertEquals("Динамика веса", chart.getTitle().getText());

        XYPlot plot = chart.getXYPlot();
        assertNotNull(plot);
        assertEquals(1, plot.getDataset().getSeriesCount());

        ValueAxis rangeAxis = plot.getRangeAxis();
        assertNotNull(rangeAxis);
        assertEquals("Вес (кг)", rangeAxis.getLabel());
    }

    @Test
    void createWeightChart_WithNullData_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createWeightChart(null)
        );

        assertEquals("Данные о весе не могут быть пустыми", exception.getMessage());
    }

    @Test
    void createWeightChart_WithEmptyData_ShouldThrowException() {
        Map<LocalDate, Double> emptyData = new HashMap<>();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createWeightChart(emptyData)
        );

        assertEquals("Данные о весе не могут быть пустыми", exception.getMessage());
    }

    @Test
    void createWeightChart_WithNullDate_ShouldThrowException() {
        Map<LocalDate, Double> dataWithNullDate = new HashMap<>();
        dataWithNullDate.put(now.minusDays(5), 75.5);
        dataWithNullDate.put(null, 76.0);
        dataWithNullDate.put(now.minusDays(3), 76.5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createWeightChart(dataWithNullDate)
        );

        assertEquals("Дата не может быть null", exception.getMessage());
    }

    @Test
    void createWeightChart_WithFutureDate_ShouldThrowException() {
        Map<LocalDate, Double> dataWithFutureDate = new HashMap<>();
        dataWithFutureDate.put(now.minusDays(5), 75.5);
        dataWithFutureDate.put(now.plusDays(1), 76.0);
        dataWithFutureDate.put(now.minusDays(3), 76.5);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createWeightChart(dataWithFutureDate)
        );

        assertTrue(exception.getMessage().contains("Дата не может быть в будущем"));
    }

    @Test
    void createWeightChart_WithNullValues_ShouldSkipNullValues() {
        Map<LocalDate, Double> dataWithNullValues = new LinkedHashMap<>();
        dataWithNullValues.put(now.minusDays(5), 75.5);
        dataWithNullValues.put(now.minusDays(4), null);
        dataWithNullValues.put(now.minusDays(3), 76.5);
        dataWithNullValues.put(now.minusDays(2), 76.0);
        dataWithNullValues.put(now.minusDays(1), 75.8);

        JFreeChart chart = chartService.createWeightChart(dataWithNullValues);

        assertNotNull(chart);
    }

    @Test
    void createWeightChart_WithAllNullValues_ShouldThrowException() {
        Map<LocalDate, Double> dataAllNull = new HashMap<>();
        dataAllNull.put(now.minusDays(5), null);
        dataAllNull.put(now.minusDays(4), null);
        dataAllNull.put(now.minusDays(3), null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chartService.createWeightChart(dataAllNull)
        );

        assertEquals("Нет данных о весе для построения графика", exception.getMessage());
    }

    @Test
    void createWeightChart_WithLargeDataSet_ShouldCreateChart() {
        Map<LocalDate, Double> largeData = new LinkedHashMap<>();
        for (int i = 30; i >= 1; i--) {
            largeData.put(now.minusDays(i), 75.0 + Math.random() * 5);
        }

        JFreeChart chart = chartService.createWeightChart(largeData);

        assertNotNull(chart);
    }

    @Test
    void createExerciseChart_WithConstantValues_ShouldAdjustRange() {
        String exerciseName = "Тест";
        Map<String, Map<LocalDate, Double>> constantData = new HashMap<>();

        Map<LocalDate, Double> paramData = new LinkedHashMap<>();
        paramData.put(now.minusDays(5), 100.0);
        paramData.put(now.minusDays(4), 100.0);
        paramData.put(now.minusDays(3), 100.0);

        constantData.put("Константа", paramData);

        JFreeChart chart = chartService.createExerciseChart(exerciseName, constantData);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();

        assertTrue(rangeAxis.getLowerBound() < 100);
        assertTrue(rangeAxis.getUpperBound() > 100);
    }

    @Test
    void createExerciseChart_WithIncreasingValues_ShouldAdjustRange() {
        String exerciseName = "Тест";
        Map<String, Map<LocalDate, Double>> increasingData = new HashMap<>();

        Map<LocalDate, Double> paramData = new LinkedHashMap<>();
        paramData.put(now.minusDays(5), 50.0);
        paramData.put(now.minusDays(4), 60.0);
        paramData.put(now.minusDays(3), 70.0);
        paramData.put(now.minusDays(2), 80.0);
        paramData.put(now.minusDays(1), 90.0);

        increasingData.put("Прогрессия", paramData);

        JFreeChart chart = chartService.createExerciseChart(exerciseName, increasingData);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();

        assertTrue(rangeAxis.getLowerBound() < 50);
        assertTrue(rangeAxis.getUpperBound() > 90);
    }

    @Test
    void createExerciseChart_WithMultipleDifferentParameters_ShouldUseDefaultLabel() {
        String exerciseName = "Тест";

        JFreeChart chart = chartService.createExerciseChart(exerciseName, validExerciseData);

        XYPlot plot = chart.getXYPlot();
        assertEquals("Значение", plot.getRangeAxis().getLabel());
    }

    @Test
    void createExerciseChart_WithMultipleSameParameters_ShouldUseParameterName() {
        String exerciseName = "Тест";
        Map<String, Map<LocalDate, Double>> sameParamData = new HashMap<>();

        sameParamData.put("Вес", validExerciseData.get("Вес (кг)"));
        sameParamData.put("Вес", validExerciseData.get("Повторения"));

        JFreeChart chart = chartService.createExerciseChart(exerciseName, sameParamData);

        assertNotNull(chart);
        XYPlot plot = chart.getXYPlot();
        assertNotNull(plot.getRangeAxis().getLabel());
    }
}