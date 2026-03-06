package com.fakel.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

@Service
public class ChartService {

    private static final Color[] COLORS = {
            new Color(0, 102, 204),    // синий
            new Color(220, 50, 50),     // красный
            new Color(50, 150, 50),     // зеленый
            new Color(255, 140, 0),     // оранжевый
            new Color(128, 0, 128),     // фиолетовый
            new Color(0, 153, 153),     // бирюзовый
            new Color(255, 193, 7),     // золотой
            new Color(233, 30, 99),      // розовый
            new Color(76, 175, 80),      // светло-зеленый
            new Color(156, 39, 176)      // пурпурный
    };

    public JFreeChart createExerciseChart(
            String exerciseName,
            Map<String, Map<LocalDate, Double>> dataByParameter) {

        // Проверка входных данных
        if (exerciseName == null || exerciseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Название упражнения не может быть пустым");
        }

        if (dataByParameter == null || dataByParameter.isEmpty()) {
            throw new IllegalArgumentException("Данные для графика не могут быть пустыми");
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        for (Map.Entry<String, Map<LocalDate, Double>> entry : dataByParameter.entrySet()) {
            String parameterName = entry.getKey();
            Map<LocalDate, Double> values = entry.getValue();

            if (parameterName == null || parameterName.trim().isEmpty()) {
                continue; // пропускаем параметры без имени
            }

            if (values == null || values.isEmpty()) {
                continue;
            }

            TimeSeries series = new TimeSeries(parameterName.trim());

            values.forEach((date, value) -> {
                if (date != null && value != null) {
                    // Проверка даты (не должна быть в будущем для графиков)
                    if (date.isAfter(LocalDate.now())) {
                        throw new IllegalArgumentException("Дата не может быть в будущем: " + date);
                    }

                    series.add(new Day(
                            date.getDayOfMonth(),
                            date.getMonthValue(),
                            date.getYear()
                    ), value);
                }
            });

            if (series.getItemCount() > 0) {
                dataset.addSeries(series);
            }
        }

        if (dataset.getSeriesCount() == 0) {
            throw new IllegalArgumentException("Нет данных для построения графика");
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                exerciseName.trim(),
                "Дата",
                getYAxisLabel(dataByParameter.keySet()),
                dataset,
                true,
                true,
                false
        );

        // Настройка внешнего вида
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));
        chart.getTitle().setPaint(new Color(50, 50, 50));

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(255, 255, 255));
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);

        // Настройка оси X (даты)
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("dd.MM"));
        axis.setLabelFont(new Font("Arial", Font.PLAIN, 11));
        axis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
        axis.setVerticalTickLabels(false);

        // Настройка оси Y (значения)
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#.#"));
        rangeAxis.setLabelFont(new Font("Arial", Font.PLAIN, 11));
        rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
        rangeAxis.setAutoRangeIncludesZero(false);

        // Автоматическая настройка диапазона Y
        autoAdjustRange(plot, dataset);

        // Настройка рендерера (линии и точки)
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, COLORS[i % COLORS.length]);
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesShape(i, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));

            // Добавляем подписи значений если точек немного
            if (dataset.getSeries(i).getItemCount() <= 15) {
                renderer.setSeriesItemLabelGenerator(i,
                        new StandardXYItemLabelGenerator("{2}", new DecimalFormat("#.#"), new DecimalFormat("#.#")));
                renderer.setSeriesItemLabelsVisible(i, true);
                renderer.setSeriesItemLabelFont(i, new Font("Arial", Font.PLAIN, 9));
            }
        }

        plot.setRenderer(renderer);

        return chart;
    }

    public JFreeChart createWeightChart(Map<LocalDate, Double> weightData) {
        if (weightData == null || weightData.isEmpty()) {
            throw new IllegalArgumentException("Данные о весе не могут быть пустыми");
        }

        TimeSeries series = new TimeSeries("Вес (кг)");

        weightData.forEach((date, weight) -> {
            if (date == null) {
                throw new IllegalArgumentException("Дата не может быть null");
            }
            if (date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Дата не может быть в будущем: " + date);
            }
            if (weight != null) {
                double formattedWeight = Math.round(weight * 10) / 10.0;
                series.add(new Day(
                        date.getDayOfMonth(),
                        date.getMonthValue(),
                        date.getYear()
                ), formattedWeight);
            }
        });

        if (series.getItemCount() == 0) {
            throw new IllegalArgumentException("Нет данных о весе для построения графика");
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Динамика веса",
                "Дата",
                "Вес (кг)",
                dataset,
                true,
                true,
                false
        );

        // Настройка внешнего вида
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        // Настройка оси X
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("dd.MM"));
        axis.setLabelFont(new Font("Arial", Font.PLAIN, 11));
        axis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));

        // Настройка оси Y
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#.#"));
        rangeAxis.setLabelFont(new Font("Arial", Font.PLAIN, 11));
        rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));

        // Автоматическая настройка диапазона с отступами
        if (series.getItemCount() > 0) {
            double maxValue = series.getMaxY();
            double minValue = series.getMinY();
            double range = maxValue - minValue;

            if (range > 0) {
                rangeAxis.setRange(minValue - range * 0.1, maxValue + range * 0.1);
            } else {
                rangeAxis.setRange(minValue - 1, maxValue + 1);
            }
        }

        // Настройка линий и точек
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, new Color(0, 153, 153));
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));

        // Добавляем подписи значений
        if (series.getItemCount() <= 20) {
            renderer.setSeriesItemLabelGenerator(0,
                    new StandardXYItemLabelGenerator("{2}", new DecimalFormat("#.#"), new DecimalFormat("#.#")));
            renderer.setSeriesItemLabelsVisible(0, true);
            renderer.setSeriesItemLabelFont(0, new Font("Arial", Font.PLAIN, 9));
        }

        plot.setRenderer(renderer);

        return chart;
    }

    private void autoAdjustRange(XYPlot plot, TimeSeriesCollection dataset) {
        if (dataset.getSeriesCount() == 0) return;

        double minValue = Double.MAX_VALUE;
        double maxValue = Double.MIN_VALUE;

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            TimeSeries series = dataset.getSeries(i);
            for (int j = 0; j < series.getItemCount(); j++) {
                double val = series.getValue(j).doubleValue();
                maxValue = Math.max(maxValue, val);
                minValue = Math.min(minValue, val);
            }
        }

        if (minValue < Double.MAX_VALUE && maxValue > Double.MIN_VALUE) {
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            double range = maxValue - minValue;
            if (range > 0) {
                rangeAxis.setRange(minValue - range * 0.1, maxValue + range * 0.1);
            } else {
                rangeAxis.setRange(minValue - 1, maxValue + 1);
            }
        }
    }

    private String getYAxisLabel(Iterable<String> parameterNames) {
        if (parameterNames == null) return "Значение";

        // Пытаемся определить общий тип параметров
        Map<String, Integer> paramTypes = new HashMap<>();
        for (String name : parameterNames) {
            if (name != null) {
                paramTypes.put(name, 1);
            }
        }

        if (paramTypes.size() == 1) {
            // Если все параметры одного типа, используем его как подпись
            return parameterNames.iterator().next();
        }

        return "Значение";
    }
}