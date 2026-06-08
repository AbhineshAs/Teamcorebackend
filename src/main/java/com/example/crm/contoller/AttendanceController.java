package com.example.crm.contoller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.crm.entity.AttendanceLog;
import com.example.crm.entity.User;
import com.example.crm.repository.AttendanceRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.service.PdfService;

import jakarta.servlet.http.HttpSession; // Required for fix

@RestController
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private UserRepository userRepository;

    // --- AUTOMATED LOGIC ---
    
    public void autoRecordLogin(User user) {
        LocalDate today = LocalDate.now();
        // Fixed: Use specific user filtering to check for active sessions
        List<AttendanceLog> userLogsToday = attendanceRepository.findByUserAndDate(user, today);

        boolean hasActiveSession = userLogsToday.stream()
                .anyMatch(log -> log.getCheckOut() == null);

        if (!hasActiveSession) {
            AttendanceLog log = new AttendanceLog();
            log.setUser(user);
            log.setCheckIn(LocalDateTime.now());
            log.setDate(today);
            log.setStatus("PRESENT");
            attendanceRepository.saveAndFlush(log);
        }
    }

    public void autoRecordLogout(User user) {
        LocalDate today = LocalDate.now();
        List<AttendanceLog> logs = attendanceRepository.findByUserAndDate(user, today);
        
        for (AttendanceLog log : logs) {
            if (log.getCheckOut() == null) {
                log.setCheckOut(LocalDateTime.now());
                attendanceRepository.saveAndFlush(log);
                break; 
            }
        }
    }

    // --- NEW FIX: DATA ISOLATION FOR DASHBOARDS ---
    // Call this in your Dashboard Controller methods
    public void prepareDashboardSessionData(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            LocalDate today = LocalDate.now();
            // Fetch ONLY the latest active log for this specific user
            attendanceRepository.findByUserAndDate(sessionUser, today).stream()
                .filter(log -> log.getCheckOut() == null)
                .findFirst()
                .ifPresent(log -> {
                    model.addAttribute("checkInTime", log.getCheckIn().toString());
                });
            
            // Explicitly set the user in the model from the session to prevent ID swap
            model.addAttribute("user", sessionUser);
        }
    }

    // --- ADMIN VIEWS ---
    
    @GetMapping("/admin/attendance/list") 
    public String viewAttendanceList(Model model) {
        List<AttendanceLog> sortedLogs = attendanceRepository.findAll().stream()
                .filter(log -> log.getUser() != null)
                .sorted((a, b) -> b.getId().compareTo(a.getId())) 
                .toList();
        
        model.addAttribute("employees", userRepository.findAll()); 
        model.addAttribute("logs", sortedLogs);
        model.addAttribute("presentToday", calculatePresentToday(sortedLogs));
        return "admin_attendance_list"; 
    }

    @GetMapping("/admin/attendance/delete/{id}")
    public String deleteAttendance(@PathVariable Long id) {
        attendanceRepository.deleteById(id);
        return "redirect:/admin/attendance/list?deleted=true";
    }

    @GetMapping("/admin/attendance/punch-out/{id}")
    public String adminManualPunchOut(@PathVariable Long id) {
        attendanceRepository.findById(id).ifPresent(log -> {
            if (log.getCheckOut() == null) {
                log.setCheckOut(LocalDateTime.now());
                attendanceRepository.saveAndFlush(log);
            }
        });
        return "redirect:/admin/attendance/list?manual_out=success";
    }

    @GetMapping("/admin/attendance/report/generate")
    public ResponseEntity<byte[]> generateReport(
            @RequestParam int month, @RequestParam int year, 
            @RequestParam String reportType, @RequestParam(required = false) Long employeeId) {
        
        List<AttendanceLog> logs;
        if ("INDIVIDUAL".equals(reportType) && employeeId != null) {
            logs = attendanceRepository.findAll().stream()
                    .filter(l -> l.getUser() != null && l.getUser().getId().equals(employeeId) && 
                                 l.getDate() != null && l.getDate().getMonthValue() == month && 
                                 l.getDate().getYear() == year).toList();
        } else {
            logs = attendanceRepository.findAll().stream()
                    .filter(l -> l.getDate() != null && l.getDate().getMonthValue() == month && 
                                 l.getDate().getYear() == year).toList();
        }

        byte[] pdf = pdfService.generateMonthlyReport(logs);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Report.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    private long calculatePresentToday(List<AttendanceLog> logs) {
        return logs.stream()
                .filter(l -> l.getDate() != null && l.getDate().equals(LocalDate.now()))
                .map(l -> l.getUser().getId())
                .distinct()
                .count();
    }
}