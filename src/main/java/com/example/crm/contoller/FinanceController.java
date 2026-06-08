package com.example.crm.contoller;

import com.example.crm.entity.Transaction;
import com.example.crm.entity.User;
import com.example.crm.service.FinanceService;
import com.example.crm.service.ReportService;
import com.example.crm.repository.TransactionRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/finance")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private ReportService reportService;
    
    

   /** @GetMapping("/terminal")
    public String viewTerminal(Model model, 
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        // Default to current month if no dates provided
        if (start == null) start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        if (end == null) end = LocalDateTime.now();

        model.addAttribute("plReport", financeService.generatePLReport(start, end));
        model.addAttribute("vatReport", financeService.generateVATReport(start, end));
        model.addAttribute("transactions", transactionRepository.findByTransactionDateBetween(start, end));
        
        return "admin_finance_terminal";
    }**/

    @PostMapping("/add")
    public String addTransaction(@ModelAttribute Transaction transaction, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        transaction.setRecordedBy(currentUser);
        transactionRepository.save(transaction);
        return "redirect:/admin/finance/terminal?success=true";
    }
    @GetMapping("/terminal")
    public String viewTerminal(Model model, 
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        // Default to a 1-year range if no dates are selected
        if (start == null) start = LocalDateTime.now().minusYears(1);
        if (end == null) end = LocalDateTime.now();

        // 1. Generate the reports via the service
        Map<String, Double> plReport = financeService.generatePLReport(start, end);
        Map<String, Double> vatReport = financeService.generateVATReport(start, end);
        Map<String, Double> zakatReport = financeService.generateZakatReport(start, end); // CRITICAL: This was likely missing

        // 2. Add all reports to the model
        model.addAttribute("plReport", plReport);
        model.addAttribute("vatReport", vatReport);
        model.addAttribute("zakatReport", zakatReport); // This fixes the "null" error
        model.addAttribute("transactions", transactionRepository.findByTransactionDateBetween(start, end));
        
        return "admin_finance_terminal";
    }
    

    @GetMapping("/download-report")
    public void downloadReport(@RequestParam String type, HttpServletResponse response) throws IOException {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime end = LocalDateTime.now();
        
        Map<String, Double> reportData;
        String fileName;

        if ("VAT".equalsIgnoreCase(type)) {
            reportData = financeService.generateVATReport(start, end);
            fileName = "VAT_Return_Report.pdf";
        } else {
            reportData = financeService.generatePLReport(start, end);
            fileName = "Profit_Loss_Statement.pdf";
        }

        List<Transaction> transactions = transactionRepository.findByTransactionDateBetween(start, end);
        byte[] pdfBytes = reportService.generateFinancialPdf(fileName.replace(".pdf", ""), reportData, transactions);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getOutputStream().write(pdfBytes);
    }
    
    
    
    
}