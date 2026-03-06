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
import java.util.stream.Collectors;

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

        // Проверка обязательных параметров
        if (cadetId == null || cadetId <= 0) {
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }

        if (from == null) {
            throw new IllegalArgumentException("Дата начала не может быть null");
        }

        if (to == null) {
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        // Проверка типов тренировок
        if (types != null) {
            List<String> validTypes = List.of("Сила", "Скорость", "Выносливость");
            for (String type : types) {
                if (type != null && !validTypes.contains(type)) {
                    throw new IllegalArgumentException("Некорректный тип тренировки: " + type);
                }
            }
        }

        // Получение данных курсанта
        Cadet cadet = cadetRepository.findById(cadetId)
                .orElseThrow(() -> new RuntimeException("Курсант не найден с id: " + cadetId));

        // Проверка наличия пользователя у курсанта
        if (cadet.getUser() == null) {
            throw new RuntimeException("У курсанта отсутствуют данные пользователя");
        }

        // Формирование ФИО
        String cadetName = buildFullName(cadet.getUser());

        // Проверка номера группы
        if (cadet.getGroupId() == null) {
            throw new RuntimeException("У курсанта не указана группа");
        }

        // Получение названия университета
        String universityName = getUniversityName(cadet);

        // Получение сводной статистики
        Map<String, Object> summary;
        try {
            summary = analyticsService.getSummary(cadetId, from, to, types);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении сводной статистики: " + e.getMessage());
        }

        // Получение данных о весе
        Map<LocalDate, Double> weightData;
        try {
            weightData = analyticsService.getWeightProgress(cadetId, from, to);
        } catch (Exception e) {
            weightData = new HashMap<>();
        }

        // Создание графика веса
        JFreeChart weightChart = null;
        if (weightData != null && !weightData.isEmpty() && weightData.size() >= 2) {
            try {
                weightChart = chartService.createWeightChart(weightData);
            } catch (Exception e) {
                // Если не удалось создать график, просто пропускаем
            }
        }

        // Получение данных об упражнениях
        Map<String, Map<String, Map<LocalDate, Double>>> exercisesData;
        try {
            exercisesData = analyticsService.getExercisesProgress(cadetId, from, to, types);
        } catch (Exception e) {
            exercisesData = new HashMap<>();
        }

        // Создание графиков упражнений
        Map<String, JFreeChart> exerciseCharts = new HashMap<>();

        if (exercisesData != null && !exercisesData.isEmpty()) {
            for (Map.Entry<String, Map<String, Map<LocalDate, Double>>> entry : exercisesData.entrySet()) {
                String exerciseName = entry.getKey();
                Map<String, Map<LocalDate, Double>> parametersData = entry.getValue();

                if (exerciseName == null || exerciseName.trim().isEmpty()) {
                    continue;
                }

                if (parametersData == null || parametersData.isEmpty()) {
                    continue;
                }

                boolean hasData = parametersData.values().stream()
                        .filter(m -> m != null)
                        .anyMatch(m -> m.size() >= 2);

                if (hasData) {
                    try {
                        JFreeChart chart = chartService.createExerciseChart(
                                exerciseName.trim(),
                                parametersData
                        );
                        if (chart != null) {
                            exerciseCharts.put(exerciseName.trim(), chart);
                        }
                    } catch (Exception e) {
                        // Если не удалось создать график, пропускаем это упражнение
                    }
                }
            }
        }

        // Генерация PDF
        try {
            return pdfService.buildTrainingPdf(
                    cadetId,
                    cadetName,
                    cadet.getGroupId().toString(),
                    universityName,
                    from,
                    to,
                    types != null ? types : List.of(),
                    summary != null ? summary : new HashMap<>(),
                    weightChart,
                    exerciseCharts
            );
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при генерации PDF отчета: " + e.getMessage());
        }
    }

    private String buildFullName(com.fakel.model.User user) {
        StringBuilder fullName = new StringBuilder();

        if (user.getLastName() != null) {
            fullName.append(user.getLastName());
        }

        if (user.getFirstName() != null) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(user.getFirstName());
        }

        if (user.getPatronymic() != null && !user.getPatronymic().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(user.getPatronymic());
        }

        return fullName.toString().trim();
    }

    private String getUniversityName(Cadet cadet) {
        if (cadet.getUser() == null) {
            return "Военная академия";
        }

        if (cadet.getUser().getUniversity() == null) {
            return "Военная академия";
        }

        String universityCode = cadet.getUser().getUniversity().getCode();
        return universityCode != null ? universityCode : "Военная академия";
    }
}