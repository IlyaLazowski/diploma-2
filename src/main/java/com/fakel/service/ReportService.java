package com.fakel.service;

import com.fakel.model.Cadet;
import com.fakel.repository.CadetRepository;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

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

        log.info("Начало генерации отчета по тренировкам: cadetId={}, from={}, to={}, types={}",
                cadetId, from, to, types);

        // Проверка обязательных параметров
        if (cadetId == null || cadetId <= 0) {
            log.warn("Некорректный ID курсанта: {}", cadetId);
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }

        if (from == null) {
            log.warn("Дата начала null");
            throw new IllegalArgumentException("Дата начала не может быть null");
        }

        if (to == null) {
            log.warn("Дата окончания null");
            throw new IllegalArgumentException("Дата окончания не может быть null");
        }

        if (from.isAfter(to)) {
            log.warn("Некорректный диапазон дат: from={} after to={}", from, to);
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        // Проверка типов тренировок
        if (types != null && !types.isEmpty()) {
            log.debug("Проверка типов тренировок: {}", types);
            List<String> validTypes = List.of("Сила", "Скорость", "Выносливость");
            for (String type : types) {
                if (type != null && !validTypes.contains(type)) {
                    log.warn("Некорректный тип тренировки: {}", type);
                    throw new IllegalArgumentException("Некорректный тип тренировки: " + type);
                }
            }
        }

        // Получение данных курсанта
        log.debug("Поиск курсанта по ID: {}", cadetId);
        Cadet cadet = cadetRepository.findById(cadetId)
                .orElseThrow(() -> {
                    log.warn("Курсант не найден с id: {}", cadetId);
                    return new RuntimeException("Курсант не найден с id: " + cadetId);
                });

        // Проверка наличия пользователя у курсанта
        if (cadet.getUser() == null) {
            log.warn("У курсанта {} отсутствуют данные пользователя", cadetId);
            throw new RuntimeException("У курсанта отсутствуют данные пользователя");
        }

        // Формирование ФИО
        String cadetName = buildFullName(cadet.getUser());
        log.debug("ФИО курсанта: {}", cadetName);

        // Проверка номера группы
        if (cadet.getGroupId() == null) {
            log.warn("У курсанта {} не указана группа", cadetId);
            throw new RuntimeException("У курсанта не указана группа");
        }
        log.debug("Группа курсанта: {}", cadet.getGroupId());

        // Получение названия университета
        String universityName = getUniversityName(cadet);
        log.debug("Университет: {}", universityName);

        // Получение сводной статистики
        log.debug("Получение сводной статистики от analyticsService");
        Map<String, Object> summary;
        try {
            summary = analyticsService.getSummary(cadetId, from, to, types);
            log.debug("Сводная статистика получена: {}", summary);
        } catch (Exception e) {
            log.error("Ошибка при получении сводной статистики: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении сводной статистики: " + e.getMessage());
        }

        // Получение данных о весе
        log.debug("Получение данных о весе");
        Map<LocalDate, Double> weightData;
        try {
            weightData = analyticsService.getWeightProgress(cadetId, from, to);
            log.debug("Данные о весе получены, количество записей: {}", weightData != null ? weightData.size() : 0);
        } catch (Exception e) {
            log.warn("Ошибка при получении данных о весе: {}", e.getMessage());
            weightData = new HashMap<>();
        }

        // Создание графика веса
        JFreeChart weightChart = null;
        if (weightData != null && !weightData.isEmpty() && weightData.size() >= 2) {
            log.debug("Создание графика веса ({} точек данных)", weightData.size());
            try {
                weightChart = chartService.createWeightChart(weightData);
                log.debug("График веса успешно создан");
            } catch (Exception e) {
                log.warn("Ошибка при создании графика веса: {}", e.getMessage());
            }
        } else {
            log.debug("Недостаточно данных для графика веса (нужно минимум 2 точки)");
        }

        // Получение данных об упражнениях
        log.debug("Получение данных об упражнениях");
        Map<String, Map<String, Map<LocalDate, Double>>> exercisesData;
        try {
            exercisesData = analyticsService.getExercisesProgress(cadetId, from, to, types);
            log.debug("Данные об упражнениях получены, количество упражнений: {}",
                    exercisesData != null ? exercisesData.size() : 0);
        } catch (Exception e) {
            log.warn("Ошибка при получении данных об упражнениях: {}", e.getMessage());
            exercisesData = new HashMap<>();
        }

        // Создание графиков упражнений
        log.debug("Создание графиков упражнений");
        Map<String, JFreeChart> exerciseCharts = new HashMap<>();
        int chartsCreated = 0;
        int chartsSkipped = 0;

        if (exercisesData != null && !exercisesData.isEmpty()) {
            for (Map.Entry<String, Map<String, Map<LocalDate, Double>>> entry : exercisesData.entrySet()) {
                String exerciseName = entry.getKey();
                Map<String, Map<LocalDate, Double>> parametersData = entry.getValue();

                if (exerciseName == null || exerciseName.trim().isEmpty()) {
                    log.trace("Пропуск упражнения с пустым именем");
                    chartsSkipped++;
                    continue;
                }

                if (parametersData == null || parametersData.isEmpty()) {
                    log.trace("Упражнение {} не содержит данных", exerciseName);
                    chartsSkipped++;
                    continue;
                }

                boolean hasData = parametersData.values().stream()
                        .filter(m -> m != null)
                        .anyMatch(m -> m.size() >= 2);

                if (hasData) {
                    log.trace("Создание графика для упражнения '{}'", exerciseName);
                    try {
                        JFreeChart chart = chartService.createExerciseChart(
                                exerciseName.trim(),
                                parametersData
                        );
                        if (chart != null) {
                            exerciseCharts.put(exerciseName.trim(), chart);
                            chartsCreated++;
                            log.trace("График для упражнения '{}' создан", exerciseName);
                        }
                    } catch (Exception e) {
                        log.warn("Ошибка при создании графика для упражнения '{}': {}", exerciseName, e.getMessage());
                        chartsSkipped++;
                    }
                } else {
                    log.trace("Упражнение {} не содержит достаточно данных для графика (минимум 2 точки)", exerciseName);
                    chartsSkipped++;
                }
            }
        }

        log.info("Создано {} графиков упражнений, пропущено {}", chartsCreated, chartsSkipped);

        // Генерация PDF
        log.info("Генерация PDF отчета через PdfService");
        try {
            byte[] pdfBytes = pdfService.buildTrainingPdf(
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

            log.info("PDF отчет успешно сгенерирован, размер: {} байт", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("Ошибка при генерации PDF отчета: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при генерации PDF отчета: " + e.getMessage());
        }
    }

    private String buildFullName(com.fakel.model.User user) {
        log.trace("Формирование ФИО из данных пользователя");

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

        String result = fullName.toString().trim();
        log.trace("Сформированное ФИО: '{}'", result);
        return result;
    }

    private String getUniversityName(Cadet cadet) {
        log.trace("Получение названия университета для курсанта");

        if (cadet.getUser() == null) {
            log.trace("У курсанта нет пользователя, возвращаем значение по умолчанию");
            return "Военная академия";
        }

        if (cadet.getUser().getUniversity() == null) {
            log.trace("У пользователя нет университета, возвращаем значение по умолчанию");
            return "Военная академия";
        }

        String universityCode = cadet.getUser().getUniversity().getCode();
        String result = universityCode != null ? universityCode : "Военная академия";

        log.trace("Название университета: '{}'", result);
        return result;
    }
}