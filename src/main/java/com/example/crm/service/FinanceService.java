package com.example.crm.service;

import com.example.crm.entity.Transaction;
import com.example.crm.repository.TransactionRepository;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Map<String, Double> generatePLReport(LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(start, end);

        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getBaseAmount)
                .sum();

        double totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getBaseAmount)
                .sum();

        return Map.of(
            "TotalIncome", totalIncome,
            "TotalExpense", totalExpense,
            "NetProfit", totalIncome - totalExpense
        );
    }

    public Map<String, Double> generateVATReport(LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(start, end);

        // VAT on Sales (Output VAT)
        double outputVat = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getVatAmount)
                .sum();

        // VAT on Purchases (Input VAT)
        double inputVat = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getVatAmount)
                .sum();

        return Map.of(
            "OutputVAT", outputVat,
            "InputVAT", inputVat,
            "VatPayable", outputVat - inputVat
        );
    }
    
    public Map<String, Double> generateZakatReport(LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(start, end);

        // Calculate Zakat Assets (Total Base Income)
        double zakatAssets = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getBaseAmount)
                .sum();

        // Calculate Deductible Liabilities (Total Base Expenses)
        double zakatLiabilities = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getBaseAmount)
                .sum();

        double zakatBase = Math.max(0, zakatAssets - zakatLiabilities);
        double zakatDue = zakatBase * 0.025; // Standard 2.5% rate

        return Map.of(
            "ZakatAssets", zakatAssets,
            "ZakatLiabilities", zakatLiabilities,
            "ZakatBase", zakatBase,
            "ZakatDue", zakatDue
        );
    }
    public Map<String, Object> generateDetailedPLReport(LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(start, end);

        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getBaseAmount)
                .sum();

        // Group expenses by category
        Map<String, Double> expenseBreakdown = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .collect(Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.summingDouble(Transaction::getBaseAmount)
                ));

        double totalExpense = expenseBreakdown.values().stream().mapToDouble(Double::doubleValue).sum();

        return Map.of(
            "TotalIncome", totalIncome,
            "TotalExpense", totalExpense,
            "NetProfit", totalIncome - totalExpense,
            "ExpenseBreakdown", expenseBreakdown
        );
    }
    public byte[] generateFinancialPdf(String title, Map<String, Double> data, List<Transaction> transactions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph(title).setFontSize(18).setBold());
        document.add(new Paragraph("Whitetrack Technologies - Financial Record"));

        // Summary Section
        document.add(new Paragraph("\nExecutive Summary:").setBold());
        Table summaryTable = new Table(2).useAllAvailableWidth();
        data.forEach((key, value) -> {
            summaryTable.addCell(new Cell().add(new Paragraph(key)));
            summaryTable.addCell(new Cell().add(new Paragraph(String.format("INR %,.2f", value))));
        });
        document.add(summaryTable);

        // UPDATED: Transaction Table with Category Column
        document.add(new Paragraph("\nDetailed Ledger (Categorized):").setBold());
        // Increased columns from 4 to 5 to include Category
        Table table = new Table(UnitValue.createPercentArray(new float[]{15, 25, 25, 17, 18})).useAllAvailableWidth();
        
        table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Description").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Category").setBold())); // New Column
        table.addHeaderCell(new Cell().add(new Paragraph("Base (INR)").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("VAT (INR)").setBold()));

        for (Transaction tx : transactions) {
            table.addCell(tx.getTransactionDate().toLocalDate().toString());
            table.addCell(tx.getDescription());
            table.addCell(tx.getCategory() != null ? tx.getCategory() : "General"); // Show Category
            table.addCell(String.format("%,.2f", tx.getBaseAmount()));
            table.addCell(String.format("%,.2f", tx.getVatAmount()));
        }
        document.add(table);

        document.close();
        return out.toByteArray();
    }
    
    public Map<String, Object> generateDetailedZakatReport(LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(start, end);

        // 1. Zakat Assets (Cash in Hand/Bank + Receivables)
        double currentAssets = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getBaseAmount)
                .sum();

        // 2. Deductible Liabilities (Short-term business debts/Operating Expenses)
        double deductibleLiabilities = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getBaseAmount)
                .sum();

        // 3. Adjusted Zakat Base
        double zakatBase = Math.max(0, currentAssets - deductibleLiabilities);
        
        // 4. Zakat Due (2.5% for Gregorian year)
        double zakatDue = zakatBase * 0.025;

        return Map.of(
            "CurrentAssets", currentAssets,
            "DeductibleLiabilities", deductibleLiabilities,
            "ZakatBase", zakatBase,
            "ZakatDue", zakatDue,
            "ComplianceStatus", "GCC Standard - Net Assets Method"
        );
    }
    
    public Map<String, Double> generateMultiCurrencyReport(String baseCurrency, LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(start, end);

        double totalIncomeBase = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(t -> t.getBaseAmount() * (t.getExchangeRate() != null ? t.getExchangeRate() : 1.0))
                .sum();

        // Similar logic for Expenses...
        return Map.of("ConsolidatedRevenue", totalIncomeBase);
    }
}