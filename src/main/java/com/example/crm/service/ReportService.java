package com.example.crm.service;

import com.example.crm.entity.Transaction;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    public byte[] generateFinancialPdf(String title, Map<String, Double> data, List<Transaction> transactions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(title).setFontSize(20).setBold());
        document.add(new Paragraph("Whitetrack Technologies - Official Report"));

        // Summary Table
        Table summaryTable = new Table(2);
        data.forEach((key, value) -> {
            summaryTable.addCell(key);
            summaryTable.addCell("INR " + String.format("%.2f", value));
        });
        document.add(new Paragraph("\nExecutive Summary:"));
        document.add(summaryTable);

        // Transaction Details
        document.add(new Paragraph("\nDetailed Ledger:"));
        Table table = new Table(4);
        table.addCell("Date");
        table.addCell("Description");
        table.addCell("Base Amount");
        table.addCell("VAT Amount");

        for (Transaction tx : transactions) {
            table.addCell(tx.getTransactionDate().toLocalDate().toString());
            table.addCell(tx.getDescription());
            table.addCell(tx.getBaseAmount().toString());
            table.addCell(tx.getVatAmount().toString());
        }
        document.add(table);

        document.close();
        return out.toByteArray();
    }
    public void addZakatSection(Document document, Map<String, Object> zakatData) {
        document.add(new Paragraph("\nZakat Compliance Report (Detailed)").setBold().setFontSize(14));
        
        Table zakatTable = new Table(2).useAllAvailableWidth();
        zakatTable.addCell("Zakat-Eligible Assets (Current)");
        zakatTable.addCell("INR " + String.format("%,.2f", zakatData.get("CurrentAssets")));
        
        zakatTable.addCell("Deductible Liabilities");
        zakatTable.addCell("INR " + String.format("%,.2f", zakatData.get("DeductibleLiabilities")));
        
        zakatTable.addCell(new Cell().add(new Paragraph("Calculated Zakat Base")).setBold());
        zakatTable.addCell(new Cell().add(new Paragraph("INR " + String.format("%,.2f", zakatData.get("ZakatBase")))).setBold());
        
        zakatTable.addCell(new Cell().add(new Paragraph("Zakat Due (2.5%)")).setBold().setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED));
        zakatTable.addCell(new Cell().add(new Paragraph("INR " + String.format("%,.2f", zakatData.get("ZakatDue")))).setBold());
        
        document.add(zakatTable);
        document.add(new Paragraph("Methodology: " + zakatData.get("ComplianceStatus")).setFontSize(8).setItalic());
    }
}