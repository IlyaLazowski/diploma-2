package com.fakel.service;

import com.fakel.model.Cadet;
import com.fakel.repository.CadetRepository;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TrainingAnalyticsService analyticsService;
    private final ChartService chartService;
    private final PdfService pdfService;
    private final CadetRepository cadetRepository;

    @Autowired
    public ReportService(TrainingAnalyticsService analyticsService,
                         ChartService chartService,
                         PdfService pdfService,
                         CadetRepository cadetRepository) {
        this.analyticsService = analyticsService;
        this.chartService = chartService;
        this.pdfService = pdfService;
        this.cadetRepository = cadetRepository;
    }

    public byte[] generateTrainingReport(Long cadetId, LocalDate from, LocalDate to, List<String> types) throws Exception {

        Cadet cadet = cadetRepository.findById(cadetId)
                .orElseThrow(() -> new RuntimeException("Курсант не найден"));

        String cadetName = cadet.getUser().getLastName() + " " +
                cadet.getUser().getFirstName() + " " +
                (cadet.getUser().getPatronymic() != null ? cadet.getUser().getPatronymic() : "");

        String universityName = cadet.getUser().getUniversity() != null ?
                cadet.getUser().getUniversity().getCode() : "Военная академия";

        Map<String, Object> summary = analyticsService.getSummary(cadetId, from, to, types);

        Map<LocalDate, Double> weightData = analyticsService.getWeightProgress(cadetId, from, to);
        JFreeChart weightChart = null;
        if (!weightData.isEmpty() && weightData.size() >= 2) {
            weightChart = chartService.createWeightChart(weightData);
        }

        Map<String, Map<String, Map<LocalDate, Double>>> exercisesData =
                analyticsService.getExercisesProgress(cadetId, from, to, types);

        Map<String, JFreeChart> exerciseCharts = new HashMap<>();

        for (Map.Entry<String, Map<String, Map<LocalDate, Double>>> entry : exercisesData.entrySet()) {
            String exerciseName = entry.getKey();
            Map<String, Map<LocalDate, Double>> parametersData = entry.getValue();

            boolean hasData = parametersData.values().stream()
                    .anyMatch(m -> m.size() >= 2);

            if (hasData) {
                JFreeChart chart = chartService.createExerciseChart(
                        exerciseName,
                        parametersData
                );
                exerciseCharts.put(exerciseName, chart);
            }
        }

        return pdfService.buildTrainingPdf(
                cadetId,
                cadetName,
                cadet.getGroupId().toString(),
                universityName,
                from,
                to,
                types,
                summary,
                weightChart,
                exerciseCharts
        );
    }
}