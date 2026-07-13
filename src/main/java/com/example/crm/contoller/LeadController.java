package com.example.crm.contoller;

import com.example.crm.entity.Lead;
import com.example.crm.entity.Transaction;
import com.example.crm.entity.User;
import com.example.crm.entity.Student;
import com.example.crm.entity.HrNotification;
import com.example.crm.repository.LeadRepository;
import com.example.crm.repository.TransactionRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.repository.StudentRepository;
import com.example.crm.repository.HrNotificationRepository;
import com.example.crm.service.ExcelImportService;
import java.time.LocalDate;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.util.List;

@RestController
public class LeadController {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ExcelImportService excelService;

    @Autowired
    private UserRepository userRepository;
    
    
    public LeadRepository getLeadRepository() {
		return leadRepository;
	}

	public void setLeadRepository(LeadRepository leadRepository) {
		this.leadRepository = leadRepository;
	}

	public ExcelImportService getExcelService() {
		return excelService;
	}

	public void setExcelService(ExcelImportService excelService) {
		this.excelService = excelService;
	}

	public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public TransactionRepository getTransactionRepository() {
		return transactionRepository;
	}

	public void setTransactionRepository(TransactionRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}
	@Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private HrNotificationRepository hrNotificationRepository;
    
    @PostMapping("/leads/capture")
    public String captureLead(@ModelAttribute Lead lead, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (leadRepository.existsByPhoneNumberOrEmail(lead.getPhoneNumber(), lead.getEmail())) {
            String redirectPath = "MANAGER".equals(currentUser.getRole()) ? "/manager/lead-capture" : "/executive/lead-capture";
            return "redirect:" + redirectPath + "?error=duplicate";
        }
        lead.setUser(currentUser);
        leadRepository.save(lead);
        String successPath = "MANAGER".equals(currentUser.getRole()) ? "/manager/lead-capture" : "/executive/lead-capture";
        return "redirect:" + successPath + "?success=true";
    }

    @PostMapping("/leads/update")
    public String updateLead(@ModelAttribute Lead lead, HttpSession session, HttpServletRequest request) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        // Preserve the original owner of the lead
        lead.setUser(currentUser);
        leadRepository.save(lead); 

        // Get the previous URL to see where the user was (Personal Leads vs Team Leads)
        String referer = request.getHeader("Referer");
        String redirectPath;

        if ("MANAGER".equals(currentUser.getRole())) {
            // If the manager was on their 'Personal Registry' (/manager/my-leads), stay there
            if (referer != null && referer.contains("/manager/my-leads")) {
                redirectPath = "/manager/my-leads";
            } else {
                // Default manager redirect for lead capture/team leads
                redirectPath = "/manager/lead-capture";
            }
        } else {
            // Default redirect for Executives
            redirectPath = "/executive/lead-capture";
        }

        return "redirect:" + redirectPath + "?updated=true";
    }

    @GetMapping("/leads/delete/{id}")
    public String deleteLead(@PathVariable Long id, HttpSession session, HttpServletRequest request) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        leadRepository.deleteById(id);

        // Get the previous URL to see where the user was when they clicked delete
        String referer = request.getHeader("Referer");
        String redirectPath;

        if ("MANAGER".equals(currentUser.getRole())) {
            // If the manager was on their 'Personal Registry' (my-leads), stay there
            if (referer != null && referer.contains("/manager/my-leads")) {
                redirectPath = "/manager/my-leads";
            } else {
                // Default manager redirect for lead capture/team leads
                redirectPath = "/manager/lead-capture";
            }
        } else {
            // Default redirect for Executives
            redirectPath = "/executive/lead-capture";
        }

        return "redirect:" + redirectPath + "?deleted=true";
    }

    
    
    
    @PostMapping("/executive/leads/bulk-upload")
    public String bulkUpload(@RequestParam("file") MultipartFile file, HttpSession session) {
        // 1. Retrieve user from session
        User currentUser = (User) session.getAttribute("user");

        // 2. SAFETY CHECK: If session expired or user is null, redirect to login to avoid NPE
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // 3. Process the Excel file
            excelService.importLeads(file, currentUser);
            
            // Retrieve result counts from session
            Integer added = (Integer) session.getAttribute("bulkAdded");
            Integer skipped = (Integer) session.getAttribute("bulkSkipped");

            // 4. DYNAMIC REDIRECTION: Check role to stay on the correct page
            String redirectPath = "MANAGER".equals(currentUser.getRole()) 
                                  ? "/manager/lead-capture" 
                                  : "/executive/lead-capture";

            return "redirect:" + redirectPath + "?success=bulk_done&added=" + added + "&skipped=" + skipped;

        } catch (Exception e) {
            // 5. Handle errors and redirect back to the originating page
            String errorPath = "MANAGER".equals(currentUser.getRole()) 
                                ? "/manager/lead-capture" 
                                : "/executive/lead-capture";
            return "redirect:" + errorPath + "?error=upload_failed";
        }
    }

    @GetMapping("/executive/follow-ups")
    public String showFollowUps(HttpSession session, org.springframework.ui.Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";
        
        List<Lead> myLeads = leadRepository.findByUser(currentUser);
        long overdueCount = myLeads.stream()
            .filter(l -> l.getFollowUpDate() != null && l.getFollowUpDate().isBefore(java.time.LocalDate.now()) && "PENDING".equals(l.getFollowUpStatus()))
            .count();

        model.addAttribute("leads", myLeads);
        model.addAttribute("overdueCount", overdueCount);
        return "executive_followups";
    }
    
    
    /**
    @PostMapping("/executive/sale-close/submit")
    public String processSaleClose(@RequestParam Long leadId, 
                                   @RequestParam Double amount, 
                                   @RequestParam String remarks, 
                                   @RequestParam String closeDate,
                                   @RequestParam String closeTime) {
        Lead lead = leadRepository.findById(leadId).orElseThrow();
        lead.setStatus("Close"); // Matches your new status option
        lead.setFollowUpStatus("COMPLETED");
        // Saving closing details in the notes field for registry
        lead.setNotes("SALE CLOSED: ₹" + amount + " | Date: " + closeDate + " | Time: " + closeTime + " | Remarks: " + remarks);
        leadRepository.save(lead);

        return "redirect:/executive/lead-capture?success=sale_closed"; 
    }**/

    @GetMapping("/executive/closed-sales")
    public String showClosedSales(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        List<Lead> closedLeads = leadRepository.findByUser(user).stream()
            .filter(l -> "Close".equalsIgnoreCase(l.getStatus()))
            .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("leads", closedLeads);
        return "executive_closed_sales";
    }
    
    @GetMapping("/executive/leads/download-sample")
    public void downloadSample(HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Bulk Import Template");

        // ADDED: "Follow-up Date" header at index 4
        String[] headers = {"Customer Name", "Email", "Phone", "Source", "Follow-up Date"};
        Row headerRow = sheet.createRow(0);
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 6000);
        }

        // Updated Sample Data with dates
        Object[][] sampleData = {
            {"Arjun Kumar", "arjun@example.com", "9876543210", "Meta Ads", "2026-03-20"},
            {"Sara Khan", "sara.k@test.com", "8877665544", "WhatsApp", "2026-03-22"}
        };

        int rowNum = 1;
        for (Object[] data : sampleData) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < data.length; i++) {
                row.createCell(i).setCellValue(data[i].toString());
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=GCC_Lead_Template.xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
    //**********************
 // FIXED: Updated redirect to stay on the follow-ups page
    @GetMapping("/leads/terminal/status/{id}/{newStatus}")
    public String updateLeadFollowUpStatus(@PathVariable Long id, @PathVariable String newStatus) {
        Lead lead = leadRepository.findById(id).orElseThrow(() -> new RuntimeException("Lead not found"));
        lead.setFollowUpStatus(newStatus);
        
        // Logic: If follow-up is done, we assume they were contacted
        if ("COMPLETED".equalsIgnoreCase(newStatus)) {
            lead.setStatus("Contacted");
        }
        
        leadRepository.save(lead);
        
        // Redirect back to the Pipeline page with the success trigger
        return "redirect:/executive/follow-ups?success=updated";
    }
    //**************************
    @PostMapping("/executive/sale-close/submit")
    public String processSaleClose(@RequestParam Long leadId, 
                                   @RequestParam Double amount, 
                                   @RequestParam String remarks, 
                                   @RequestParam String closeDate,
                                   @RequestParam String closeTime,
                                   HttpSession session) { // Added session to get recordedBy
        
        User currentUser = (User) session.getAttribute("user");
        Lead lead = leadRepository.findById(leadId).orElseThrow();
        
        // 1. CRM Update
        lead.setStatus("Close");
        lead.setFollowUpStatus("COMPLETED");
        lead.setNotes("SALE CLOSED: ₹" + amount + " | Date: " + closeDate + " | Time: " + closeTime + " | Remarks: " + remarks);
        leadRepository.save(lead);

        // 2. AUTOMATION: Create Finance Entry (INCOME)
        Transaction incomeTx = new Transaction();
        incomeTx.setDescription("Sale Closed: " + lead.getCustomerName());
        incomeTx.setType("INCOME");
        incomeTx.setCategory("Sales");
        incomeTx.setBaseAmount(amount);
        incomeTx.setVatRate(15.0); // GCC Standard VAT (Adjust to 5.0 or 15.0 as needed)
        incomeTx.setRecordedBy(currentUser);
        
        // Save to finance_transactions table
        transactionRepository.save(incomeTx);

        // 3. AUTOMATION: Auto-provision Student
        try {
            Student student = new Student();
            long studentCount = studentRepository.count();
            student.setStudentId("STU-" + (1000 + studentCount + 1));
            student.setName(lead.getCustomerName());
            student.setPhone(lead.getPhoneNumber());
            student.setEmail(lead.getEmail());
            student.setCollege(lead.getCollege());
            student.setQualification(lead.getDepartment() != null ? lead.getDepartment() : "Not Specified");
            student.setCoursePurchased(lead.getSource() != null ? lead.getSource() : "Java Full Stack");
            student.setCourseFees(amount);
            student.setPaidAmount(amount);
            student.setBalance(0.0);
            student.setJoiningDate(LocalDate.now());
            student.setSalesExecutive(currentUser != null ? currentUser.getName() : "System");
            student.setStatus("PENDING_VERIFICATION");
            studentRepository.save(student);

            // 4. AUTOMATION: Create HR Notification
            HrNotification notification = new HrNotification();
            notification.setMessage("Sales Closed: Student " + student.getName() + " Purchased " + student.getCoursePurchased());
            notification.setType("SALE_CLOSE");
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notification.setIsRead(false);
            hrNotificationRepository.save(notification);
        } catch (Exception e) {
            System.err.println("Failed to auto-provision student or create notification: " + e.getMessage());
        }
        
     // --- UPDATED REDIRECT LOGIC START ---
        String role = currentUser.getRole();
        if ("MANAGER".equals(role)) {
            return "redirect:/manager/my-leads?success=sale_closed";
        }
       

        return "redirect:/executive/lead-capture?success=sale_closed"; 
    }
}