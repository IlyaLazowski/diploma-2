package com.fakel.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font SUBHEADER_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);

    public byte[] buildTrainingPdf(
            Long cadetId,
            String cadetName,
            String groupNumber,
            String universityName,
            LocalDate from,
            LocalDate to,
            List<String> reportTypes,
            Map<String, Object> summary,
            JFreeChart weightChart,
            Map<String, JFreeChart> exerciseCharts) throws Exception {

        log.info("Начало генерации PDF отчета для курсанта ID: {}", cadetId);
        log.debug("Параметры отчета: cadetName={}, groupNumber={}, universityName={}, from={}, to={}, reportTypes={}",
                cadetName, groupNumber, universityName, from, to, reportTypes);
        log.debug("Наличие графиков: weightChart={}, exerciseCharts size={}",
                weightChart != null, exerciseCharts != null ? exerciseCharts.size() : 0);

        // Проверка обязательных параметров
        if (cadetId == null || cadetId <= 0) {
            log.warn("Некорректный ID курсанта: {}", cadetId);
            throw new IllegalArgumentException("ID курсанта должен быть положительным числом");
        }

        if (cadetName == null || cadetName.trim().isEmpty()) {
            log.warn("Пустое ФИО курсанта");
            throw new IllegalArgumentException("ФИО курсанта не может быть пустым");
        }

        if (groupNumber == null || groupNumber.trim().isEmpty()) {
            log.warn("Пустой номер группы");
            throw new IllegalArgumentException("Номер группы не может быть пустым");
        }

        if (universityName == null || universityName.trim().isEmpty()) {
            log.warn("Пустое название университета");
            throw new IllegalArgumentException("Название университета не может быть пустым");
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        try {
            log.debug("Начало построения документа PDF");

            // ==================== ШАПКА ОТЧЕТА ====================
            Paragraph title = new Paragraph("ОТЧЕТ ПО ТРЕНИРОВКАМ", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(30);
            document.add(title);
            log.trace("Добавлен заголовок отчета");

            // Рамка с информацией
            PdfPTable infoFrame = new PdfPTable(1);
            infoFrame.setWidthPercentage(80);
            infoFrame.setHorizontalAlignment(Element.ALIGN_CENTER);
            infoFrame.setSpacingAfter(30);

            PdfPCell frameCell = new PdfPCell();
            frameCell.setBorder(Rectangle.BOX);
            frameCell.setBorderWidth(1);
            frameCell.setBorderColor(BaseColor.BLACK);
            frameCell.setPadding(15);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1, 2});

            addInfoRow(infoTable, "Университет:", universityName.trim());
            addInfoRow(infoTable, "Группа:", groupNumber.trim());
            addInfoRow(infoTable, "ФИО:", cadetName.trim());
            addInfoRow(infoTable, "Временной промежуток:",
                    from.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " — " +
                            to.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

            String typesStr = (reportTypes == null || reportTypes.isEmpty()) ? "все" : String.join(", ", reportTypes);
            addInfoRow(infoTable, "Типы тренировок в отчете:", typesStr);
            log.trace("Добавлена информационная таблица");

            // Добавляем сводную информацию
            if (summary != null && !summary.isEmpty()) {
                log.debug("Добавление сводной информации, размер summary: {}", summary.size());
                document.add(new Paragraph(" "));

                Paragraph summaryTitle = new Paragraph("СВОДНАЯ ИНФОРМАЦИЯ", SUBHEADER_FONT);
                summaryTitle.setAlignment(Element.ALIGN_CENTER);
                summaryTitle.setSpacingBefore(10);
                summaryTitle.setSpacingAfter(15);
                document.add(summaryTitle);

                PdfPTable summaryTable = new PdfPTable(2);
                summaryTable.setWidthPercentage(60);
                summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);
                summaryTable.setSpacingAfter(20);

                Object totalTrainings = summary.get("totalTrainings");
                addSummaryRow(summaryTable, "Всего тренировок:",
                        totalTrainings != null ? totalTrainings.toString() : "0");

                Object totalDays = summary.get("totalDays");
                addSummaryRow(summaryTable, "Всего дней в периоде:",
                        totalDays != null ? totalDays.toString() : "0");

                Object totalTonnage = summary.get("totalTonnage");
                addSummaryRow(summaryTable, "Общий тоннаж:",
                        (totalTonnage != null ? totalTonnage.toString() : "0") + " кг");

                Object byTypeObj = summary.get("byType");
                if (byTypeObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Long> byType = (Map<String, Long>) byTypeObj;
                    if (byType != null && !byType.isEmpty()) {
                        log.debug("Добавление статистики по типам тренировок: {}", byType);
                        for (Map.Entry<String, Long> entry : byType.entrySet()) {
                            addSummaryRow(summaryTable, "Тренировок (" + entry.getKey() + "):",
                                    entry.getValue() != null ? String.valueOf(entry.getValue()) : "0");
                        }
                    }
                }
                document.add(summaryTable);
                log.trace("Добавлена сводная таблица");
            }

            frameCell.addElement(infoTable);
            infoFrame.addCell(frameCell);
            document.add(infoFrame);

            // ==================== ГРАФИК ВЕСА ====================
            if (weightChart != null) {
                log.debug("Добавление графика веса");
                document.add(new Paragraph("\n"));

                Paragraph weightTitle = new Paragraph("ДИНАМИКА ВЕСА", HEADER_FONT);
                weightTitle.setAlignment(Element.ALIGN_CENTER);
                weightTitle.setSpacingBefore(20);
                weightTitle.setSpacingAfter(15);
                document.add(weightTitle);

                try {
                    Image weightImage = convertChartToImage(weightChart, 500, 300);
                    weightImage.setAlignment(Element.ALIGN_CENTER);
                    weightImage.setSpacingAfter(30);
                    document.add(weightImage);
                    log.debug("График веса успешно добавлен");
                } catch (Exception e) {
                    log.error("Ошибка при добавлении графика веса: {}", e.getMessage(), e);
                    Paragraph errorMsg = new Paragraph("Ошибка загрузки графика веса", SMALL_FONT);
                    errorMsg.setAlignment(Element.ALIGN_CENTER);
                    errorMsg.setFont(FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, BaseColor.RED));
                    document.add(errorMsg);
                }
            }

            // ==================== ГРАФИКИ УПРАЖНЕНИЙ ====================
            if (exerciseCharts != null && !exerciseCharts.isEmpty()) {
                log.debug("Добавление графиков упражнений, количество: {}", exerciseCharts.size());
                document.newPage();

                Paragraph exercisesMainTitle = new Paragraph("ДИНАМИКА ПО УПРАЖНЕНИЯМ", HEADER_FONT);
                exercisesMainTitle.setAlignment(Element.ALIGN_CENTER);
                exercisesMainTitle.setSpacingBefore(20);
                exercisesMainTitle.setSpacingAfter(25);
                document.add(exercisesMainTitle);

                int chartCount = 0;
                int successCount = 0;
                int errorCount = 0;

                for (Map.Entry<String, JFreeChart> entry : exerciseCharts.entrySet()) {
                    chartCount++;
                    String exerciseName = entry.getKey();
                    JFreeChart chart = entry.getValue();

                    if (exerciseName == null || exerciseName.trim().isEmpty()) {
                        exerciseName = "Упражнение без названия";
                    }

                    if (chart == null) {
                        log.warn("Пропуск графика для упражнения {}: chart=null", exerciseName);
                        continue;
                    }

                    log.trace("Добавление графика для упражнения: {}", exerciseName);

                    // Добавляем название упражнения
                    Paragraph exName = new Paragraph(exerciseName.trim(), SUBHEADER_FONT);
                    exName.setAlignment(Element.ALIGN_CENTER);
                    exName.setSpacingBefore(15);
                    exName.setSpacingAfter(10);
                    document.add(exName);

                    try {
                        // Конвертируем и добавляем график
                        Image chartImage = convertChartToImage(chart, 500, 280);
                        chartImage.setAlignment(Element.ALIGN_CENTER);
                        chartImage.setSpacingAfter(25);
                        document.add(chartImage);
                        successCount++;
                        log.trace("График для упражнения {} успешно добавлен", exerciseName);
                    } catch (Exception e) {
                        errorCount++;
                        log.error("Ошибка при добавлении графика для упражнения {}: {}", exerciseName, e.getMessage(), e);
                        Paragraph errorMsg = new Paragraph("Ошибка загрузки графика для упражнения: " + exerciseName, SMALL_FONT);
                        errorMsg.setAlignment(Element.ALIGN_CENTER);
                        errorMsg.setFont(FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, BaseColor.RED));
                        document.add(errorMsg);
                    }

                    // Добавляем разрыв страницы если это не последний график и их много
                    if (chartCount < exerciseCharts.size() && chartCount % 2 == 0) {
                        log.trace("Добавлен разрыв страницы после графика {}", chartCount);
                        document.newPage();
                    } else if (chartCount < exerciseCharts.size()) {
                        document.add(new Paragraph(" "));
                        document.add(new Paragraph(" "));
                    }
                }
                log.debug("Добавление графиков завершено: успешно {}, ошибок {}", successCount, errorCount);
            }

            // ==================== ПОДВАЛ ====================
            document.newPage();

            Paragraph footer = new Paragraph("Отчет сгенерирован автоматически " +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(700);
            document.add(footer);
            log.trace("Добавлен подвал отчета");

        } finally {
            document.close();
            log.debug("Документ PDF закрыт");
        }

        byte[] result = out.toByteArray();
        log.info("PDF отчет успешно сгенерирован, размер: {} байт", result.length);
        return result;
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        if (label == null) label = "";
        if (value == null) value = "";

        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(valueCell);

        log.trace("Добавлена строка в информационную таблицу: {} = {}", label, value);
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        if (label == null) label = "";
        if (value == null) value = "";

        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBackgroundColor(new BaseColor(240, 240, 240));
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, LABEL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        valueCell.setBackgroundColor(new BaseColor(240, 240, 240));
        table.addCell(valueCell);

        log.trace("Добавлена строка в сводную таблицу: {} = {}", label, value);
    }

    private Image convertChartToImage(JFreeChart chart, int width, int height) throws Exception {
        log.trace("Конвертация графика в изображение: размер {}x{}", width, height);

        if (chart == null) {
            log.warn("Попытка конвертации null графика");
            throw new IllegalArgumentException("Chart не может быть null");
        }
        if (width <= 0 || height <= 0) {
            log.warn("Некорректные размеры изображения: {}x{}", width, height);
            throw new IllegalArgumentException("Ширина и высота должны быть положительными");
        }

        ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(chartOut, chart, width, height);
        byte[] imageData = chartOut.toByteArray();

        log.trace("График сконвертирован, размер изображения: {} байт", imageData.length);
        return Image.getInstance(imageData);
    }
}