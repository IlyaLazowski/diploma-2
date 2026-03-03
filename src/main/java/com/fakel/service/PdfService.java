package com.fakel.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {

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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(document, out);
        document.open();

        // ==================== ШАПКА ОТЧЕТА ====================
        Paragraph title = new Paragraph("ОТЧЕТ ПО ТРЕНИРОВКАМ", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);

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

        addInfoRow(infoTable, "Университет:", universityName);
        addInfoRow(infoTable, "Группа:", groupNumber);
        addInfoRow(infoTable, "ФИО:", cadetName);
        addInfoRow(infoTable, "Временной промежуток:",
                from.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + " — " +
                        to.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        String typesStr = (reportTypes == null || reportTypes.isEmpty()) ? "все" : String.join(", ", reportTypes);
        addInfoRow(infoTable, "Типы тренировок в отчете:", typesStr);

        // Добавляем сводную информацию
        if (summary != null && !summary.isEmpty()) {
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

            addSummaryRow(summaryTable, "Всего тренировок:", String.valueOf(summary.get("totalTrainings")));
            addSummaryRow(summaryTable, "Всего дней в периоде:", String.valueOf(summary.get("totalDays")));
            addSummaryRow(summaryTable, "Общий тоннаж:", String.valueOf(summary.get("totalTonnage")) + " кг");

            @SuppressWarnings("unchecked")
            Map<String, Long> byType = (Map<String, Long>) summary.get("byType");
            if (byType != null && !byType.isEmpty()) {
                for (Map.Entry<String, Long> entry : byType.entrySet()) {
                    addSummaryRow(summaryTable, "Тренировок (" + entry.getKey() + "):", String.valueOf(entry.getValue()));
                }
            }

            document.add(summaryTable);
        }

        frameCell.addElement(infoTable);
        infoFrame.addCell(frameCell);
        document.add(infoFrame);

        // ==================== ГРАФИК ВЕСА ====================
        if (weightChart != null) {
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
            } catch (Exception e) {
                Paragraph errorMsg = new Paragraph("Ошибка загрузки графика веса", SMALL_FONT);
                errorMsg.setAlignment(Element.ALIGN_CENTER);
                errorMsg.setFont(FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, BaseColor.RED));
                document.add(errorMsg);
            }
        }

        // ==================== ГРАФИКИ УПРАЖНЕНИЙ ====================
        if (exerciseCharts != null && !exerciseCharts.isEmpty()) {
            document.newPage();

            Paragraph exercisesMainTitle = new Paragraph("ДИНАМИКА ПО УПРАЖНЕНИЯМ", HEADER_FONT);
            exercisesMainTitle.setAlignment(Element.ALIGN_CENTER);
            exercisesMainTitle.setSpacingBefore(20);
            exercisesMainTitle.setSpacingAfter(25);
            document.add(exercisesMainTitle);

            int chartCount = 0;
            for (Map.Entry<String, JFreeChart> entry : exerciseCharts.entrySet()) {
                String exerciseName = entry.getKey();
                JFreeChart chart = entry.getValue();

                // Добавляем название упражнения
                Paragraph exName = new Paragraph(exerciseName, SUBHEADER_FONT);
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
                } catch (Exception e) {
                    Paragraph errorMsg = new Paragraph("Ошибка загрузки графика для упражнения: " + exerciseName, SMALL_FONT);
                    errorMsg.setAlignment(Element.ALIGN_CENTER);
                    errorMsg.setFont(FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, BaseColor.RED));
                    document.add(errorMsg);
                }

                chartCount++;

                // Добавляем разрыв страницы если это не последний график и их много
                if (chartCount < exerciseCharts.size() && chartCount % 2 == 0) {
                    document.newPage();
                } else if (chartCount < exerciseCharts.size()) {
                    document.add(new Paragraph(" "));
                    document.add(new Paragraph(" "));
                }
            }
        }

        // ==================== ПОДВАЛ ====================
        document.newPage();

        Paragraph footer = new Paragraph("Отчет сгенерирован автоматически " +
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(700);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
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
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
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
    }

    private Image convertChartToImage(JFreeChart chart, int width, int height) throws Exception {
        ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(chartOut, chart, width, height);
        return Image.getInstance(chartOut.toByteArray());
    }
}