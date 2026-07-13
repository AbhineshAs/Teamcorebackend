package com.example.crm.contoller;

import com.example.crm.entity.User;
import com.example.crm.entity.EmployeeLoginHistory;
import com.example.crm.repository.UserRepository;
import com.example.crm.repository.EmployeeLoginHistoryRepository;
import com.example.crm.security.JwtUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceController attendanceController;

    @Autowired
    private EmployeeLoginHistoryRepository loginHistoryRepository;

    @GetMapping("/")
    public String Home() {
        return "login";
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @PostMapping("/login")
    public ResponseEntity<?> processLogin(@RequestParam String email,
                                          @RequestParam String password,
                                          HttpSession session,
                                          HttpServletRequest request) {

        String cleanEmail = email == null ? "" : email.trim();
        String cleanPassword = password == null ? "" : password;
        User user = userRepository.findByEmailIgnoreCase(cleanEmail);

        if (user != null && passwordMatches(user.getPassword(), cleanPassword)) {
            // 1. Store user in session
            session.setAttribute("user", user);

            // 2. Trigger Attendance Recording
            attendanceController.autoRecordLogin(user);

            // 3. Generate JWT Token
            String token = JwtUtil.generateToken(user.getEmail(), user.getRole(), user.getName());

            // 4. Log to employee_login_history
            try {
                EmployeeLoginHistory history = new EmployeeLoginHistory();
                history.setEmployeeName(user.getName());
                history.setLoginTime(LocalDateTime.now());
                history.setBrowser(request.getHeader("User-Agent"));
                history.setIpAddress(request.getRemoteAddr());
                history.setLocation("Local Terminal");
                history.setStatus("SUCCESS");
                EmployeeLoginHistory savedHistory = loginHistoryRepository.save(history);
                
                // Store the login history record ID in session to update logout time later
                session.setAttribute("loginHistoryId", savedHistory.getId());
            } catch (Exception e) {
                System.err.println("Failed to save login history: " + e.getMessage());
            }

            // 5. Construct response including token
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", user.getId());
            responseData.put("name", user.getName());
            responseData.put("email", user.getEmail());
            responseData.put("role", user.getRole());
            responseData.put("token", token);
            
            if (user.getManager() != null) {
                responseData.put("managerId", user.getManager().getId());
            }

            return ResponseEntity.ok(responseData);
        }

        Map<String, String> errorMap = new HashMap<>();
        errorMap.put("error", "Invalid email or password");
        return ResponseEntity.status(401).body(errorMap);
    }

    private boolean passwordMatches(String storedPassword, String submittedPassword) {
        if (storedPassword == null) {
            return false;
        }
        return storedPassword.equals(submittedPassword) || storedPassword.trim().equals(submittedPassword);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user != null) {
            attendanceController.autoRecordLogout(user);
            
            // Update logout time in employee_login_history
            Long historyId = (Long) session.getAttribute("loginHistoryId");
            if (historyId != null) {
                loginHistoryRepository.findById(historyId).ifPresent(history -> {
                    history.setLogoutTime(LocalDateTime.now());
                    java.time.Duration duration = java.time.Duration.between(history.getLoginTime(), history.getLogoutTime());
                    long hours = duration.toHours();
                    long minutes = duration.toMinutesPart();
                    history.setDuration(hours + "h " + minutes + "m");
                    loginHistoryRepository.save(history);
                });
            }
        }

        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
