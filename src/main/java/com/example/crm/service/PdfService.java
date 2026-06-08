package com.example.crm.service;

import com.example.crm.entity.AttendanceLog;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfService {

    public byte[] generateMonthlyReport(List<AttendanceLog> logs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // FIX: Create a mutable sorted copy to avoid UnsupportedOperationException
        List<AttendanceLog> modifiableLogs = logs.stream()
                .filter(l -> l.getCheckIn() != null)
                .sorted(Comparator.comparing(AttendanceLog::getCheckIn))
                .collect(Collectors.toList());

        // 1. Report Header
        document.add(new Paragraph("Whitetrack Technologies- ATTENDANCE REPORT")
                .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Generated on: "
                + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .setFontSize(10).setTextAlignment(TextAlignment.RIGHT));

        document.add(new Paragraph("Session Detailed Logs").setBold().setUnderline().setMarginTop(10));

        // 2. Session Details Table
        float[] columnWidths = { 140f, 90f, 80f, 80f, 80f, 70f };
        Table table = new Table(UnitValue.createPointArray(columnWidths)).useAllAvailableWidth();

        table.addHeaderCell(
                new Cell().add(new Paragraph("Employee")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Date")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Login")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Logout")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Duration")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        table.addHeaderCell(
                new Cell().add(new Paragraph("Status")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        for (AttendanceLog log : modifiableLogs) {
            table.addCell(log.getUser() != null ? log.getUser().getName() : "Unknown");
            table.addCell(log.getDate() != null ? log.getDate().toString() : "N/A");
            table.addCell(log.getCheckIn().format(timeFormatter));

            if (log.getCheckOut() != null) {
                table.addCell(log.getCheckOut().format(timeFormatter));
            } else {
                table.addCell(new Cell().add(new Paragraph("Active")).setItalic().setFontColor(ColorConstants.ORANGE));
            }

            table.addCell(log.getDurationFormatted());
            table.addCell(log.getStatus() != null ? log.getStatus() : "PRESENT");
        }
        document.add(table);

        // 3. DAILY WORK SUMMARY LOGIC
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Work Hour Summary (By Employee/Date)").setBold().setUnderline());

        float[] summaryWidths = { 200f, 150f, 150f };
        Table summaryTable = new Table(UnitValue.createPointArray(summaryWidths)).useAllAvailableWidth();
        summaryTable.addHeaderCell(
                new Cell().add(new Paragraph("Employee Name")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        summaryTable.addHeaderCell(
                new Cell().add(new Paragraph("Date")).setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY));
        summaryTable.addHeaderCell(new Cell().add(new Paragraph("Total Hours Worked")).setBold()
                .setBackgroundColor(ColorConstants.LIGHT_GRAY));

        // Grouping and then sorting by Date
        modifiableLogs.stream()
                .filter(l -> l.getCheckOut() != null && l.getUser() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getUser().getName() + "|" + l.getDate(),
                        Collectors.reducing(Duration.ZERO,
                                l -> Duration.between(l.getCheckIn(), l.getCheckOut()),
                                Duration::plus)))
                .entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().split("\\|")[1]))
                .forEach(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    Duration totalDuration = entry.getValue();
                    long hours = totalDuration.toHours();
                    long minutes = totalDuration.toMinutesPart();

                    summaryTable.addCell(parts[0]);
                    summaryTable.addCell(parts[1]);
                    summaryTable.addCell(hours + "h " + minutes + "m");
                });

        document.add(summaryTable);
        document.close();
        return out.toByteArray();
    }
}