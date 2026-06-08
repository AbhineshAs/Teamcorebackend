package com.example.crm.contoller;

import com.example.crm.entity.User;
import com.example.crm.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

@RestController
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceController attendanceController;
    @GetMapping("/")
    public String Home() {
        return "login";
    }
    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

   /** @PostMapping("/login")
    public String processLogin(@RequestParam String email, 
                               @RequestParam String password, 
                               HttpSession session, Model model) {
        
        User user = userRepository.findByEmail(email);
        
        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("user", user); 
            
            // Record Login for non-admins
            if (!"ADMIN".equals(user.getRole())) {
                attendanceController.autoRecordLogin(user);
            }
            
            if ("ADMIN".equals(user.getRole())) return "redirect:/admin/dashboard";
            if ("MANAGER".equals(user.getRole())) return "redirect:/manager/dashboard";
            return "redirect:/executive/dashboard";
        }
        
        model.addAttribute("error", "Invalid email or password");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && !"ADMIN".equals(user.getRole())) {
            attendanceController.autoRecordLogout(user);
        }
        session.invalidate();
        return "redirect:/login?logout=true";
    }**/
    
    @PostMapping("/login")
    public org.springframework.http.ResponseEntity<?> processLogin(@RequestParam String email, 
                               @RequestParam String password, 
                               HttpSession session) {
        
        User user = userRepository.findByEmail(email);
        
        if (user != null && user.getPassword().equals(password)) {
            // 1. Store user in session
            session.setAttribute("user", user);

            // 2. IMPORTANT: Trigger Attendance Recording for EVERYONE
            // This is what makes the timer visible!
            attendanceController.autoRecordLogin(user);

            return org.springframework.http.ResponseEntity.ok(user);
        }
        
        java.util.Map<String, String> errorMap = new java.util.HashMap<>();
        errorMap.put("error", "Invalid email or password");
        return org.springframework.http.ResponseEntity.status(401).body(errorMap);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        // REMOVED THE ADMIN CHECK: Now records logout for everyone
        if (user != null) {
            attendanceController.autoRecordLogout(user);
        }
        
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}