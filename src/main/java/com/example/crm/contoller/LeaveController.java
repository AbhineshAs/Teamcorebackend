package com.example.crm.contoller;

import com.example.crm.entity.EmployeeProfile;
import com.example.crm.entity.LeaveRequest;
import com.example.crm.entity.User;
import com.example.crm.repository.LeaveRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.repository.AttendanceRepository; // Added to keep timer working
import com.example.crm.repository.EmployeeProfileRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@RestController
public class LeaveController {

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private EmployeeProfileRepository profileRepository;
    
    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    // Helper Method to send emails
    private void sendEmail(String fromEmail, String fromName, String toEmail, String subject, String bodyContent) {
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = 
                new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            
            // --- Professional HTML Template ---
            String htmlMsg = "<div style='font-family: Inter, Arial, sans-serif; max-width: 600px; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden;'>" +
                    "  <div style='background: #6366f1; padding: 25px; text-align: center;'>" +
                    "    <h1 style='color: white; margin: 0; font-size: 24px; letter-spacing: -1px;'>GCC SMART</h1>" +
                    "  </div>" +
                    "  <div style='padding: 30px; color: #1e293b; line-height: 1.6;'>" +
                    "    <h2 style='margin-top: 0; color: #6366f1;'>" + subject + "</h2>" +
                    "    <div style='background: #f8fafc; border-radius: 12px; padding: 20px; margin: 20px 0; border: 1px solid #e2e8f0;'>" +
                         bodyContent + 
                    "    </div>" +
                    "    <p style='font-size: 14px; color: #64748b;'>This is an automated notification from the GCC Smart CRM system.</p>" +
                    "  </div>" +
                    "  <div style='background: #f1f5ff; padding: 15px; text-align: center; font-size: 12px; color: #6366f1; font-weight: bold;'>" +
                    "    &copy; 2026 GCC Smart Dashboard" +
                    "  </div>" +
                    "</div>";

            helper.setText(htmlMsg, true); // 'true' tells Spring this is HTML
            
            // Use your system email here, but show the user's name
            helper.setFrom("your-system-email@gmail.com", fromName);
            helper.setReplyTo(fromEmail); // Manager clicks reply -> goes to Executive

            mailSender.send(message);
            System.out.println("HTML Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Critical Mail Error: " + e.getMessage());
        }
    }

    // --- APPLY LEAVE (Common for Managers & Executives) ---
   /** @GetMapping("/leave/apply")
    public String showLeaveForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        // Fetch fresh user to ensure correct role/manager data
        User currentUser = userRepository.findById(user.getId()).orElse(user);
        List<LeaveRequest> myHistory = leaveRepository.findByUserOrderByCreatedAtDesc(currentUser);
        
        model.addAttribute("leaveRequest", new LeaveRequest());
        model.addAttribute("myHistory", myHistory);
        model.addAttribute("user", currentUser);

        // Keep the dashboard timer alive on this page
        LocalDate today = LocalDate.now();
        attendanceRepository.findByUserAndDate(currentUser, today).stream()
            .filter(log -> log.getCheckOut() == null)
            .max(Comparator.comparing(com.example.crm.entity.AttendanceLog::getCheckIn))
            .ifPresent(log -> model.addAttribute("checkInTime", log.getCheckIn().toString()));

        return "leave_apply";
    }**/

    @PostMapping("/leave/submit")
    public String submitLeave(@ModelAttribute LeaveRequest leaveRequest, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

     // 1. Validate: Ensure End Date is not before Start Date
        if (leaveRequest.getEndDate().isBefore(leaveRequest.getStartDate())) {
            return "redirect:/leave/apply?error=invalid_dates";
        }

        // 2. Validate: Check for Overlapping Dates (Duplicate Date Prevention)
        boolean isOverlapping = leaveRepository.existsOverlappingLeave(
                user, 
                leaveRequest.getStartDate(), 
                leaveRequest.getEndDate()
        );

        if (isOverlapping) {
            return "redirect:/leave/apply?error=overlap";
        }

        // 3. Save to Database (Only if validations pass)
        leaveRequest.setUser(user);
        leaveRequest.setStatus("PENDING");
        leaveRepository.save(leaveRequest);
        
     // Get Manager's Email
        User manager = user.getManager(); 
        if (manager != null && manager.getEmail() != null) {
            String content = "<table style='width: 100%; border-collapse: collapse;'>" +
                    "<tr><td style='padding: 8px 0; color: #64748b;'>Employee:</td><td style='font-weight: bold;'>" + user.getName() + "</td></tr>" +
                    "<tr><td style='padding: 8px 0; color: #64748b;'>Leave Type:</td><td style='font-weight: bold;'>" + leaveRequest.getLeaveType() + "</td></tr>" +
                    "<tr><td style='padding: 8px 0; color: #64748b;'>Duration:</td><td style='font-weight: bold;'>" + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + "</td></tr>" +
                    "<tr><td style='padding: 8px 0; color: #64748b;'>Reason:</td><td>" + leaveRequest.getReason() + "</td></tr>" +
                    "</table>";
            
            sendEmail(user.getEmail(), user.getName(), manager.getEmail(), "New Leave Request Submitted", content);
        }

        return "redirect:/leave/apply?success=applied";
    }

    // --- MANAGE LEAVES (Hierarchical Approval) ---
    @GetMapping("/leave/manage")
    public String manageLeaves(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        List<LeaveRequest> requestsToApprove;

        if ("ADMIN".equals(currentUser.getRole())) {
            // ADMIN sees requests from all MANAGERS
            requestsToApprove = leaveRepository.findAll().stream()
                .filter(l -> "MANAGER".equals(l.getUser().getRole()))
                .filter(l -> "PENDING".equals(l.getStatus()))
                .toList();
            model.addAttribute("viewTitle", "Manager Leave Requests");
        } 
        else if ("MANAGER".equals(currentUser.getRole())) {
            // MANAGER sees requests only from THEIR team (Executives)
            List<User> myTeam = userRepository.findByManager(currentUser);
            requestsToApprove = leaveRepository.findByUserInOrderByCreatedAtDesc(myTeam).stream()
                .filter(l -> "PENDING".equals(l.getStatus()))
                .toList();
            model.addAttribute("viewTitle", "Team Leave Requests");
        } 
        else {
            return "redirect:/executive/dashboard?error=unauthorized";
        }

        model.addAttribute("pendingLeaves", requestsToApprove);
        model.addAttribute("user", currentUser);
        return "admin_leave_manage"; 
    }

   /** @PostMapping("/leave/action")
    public String updateLeaveStatus(@RequestParam Long id, @RequestParam String action, HttpSession session) {
        leaveRepository.findById(id).ifPresent(leave -> {
            leave.setStatus(action.toUpperCase()); // APPROVED or REJECTED
            leaveRepository.save(leave);
        });
        return "redirect:/leave/manage?success=action_completed";
    }**/
    
 // --- ADMIN VIEW: Handle Manager Leaves ---
  /**  @GetMapping("/admin/leave/requests")
    public String adminManageLeaves(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !"ADMIN".equals(admin.getRole())) return "redirect:/login";

        // Only show PENDING requests from USERS who are MANAGERS
        List<LeaveRequest> managerRequests = leaveRepository.findAll().stream()
            .filter(l -> "MANAGER".equals(l.getUser().getRole()) && "PENDING".equals(l.getStatus()))
            .toList();

        model.addAttribute("pendingLeaves", managerRequests);
        model.addAttribute("viewTitle", "Manager Leave Approvals");
        return "admin_leave_requests"; 
    }**/

    // --- MANAGER VIEW: Handle Executive Leaves ---
    @GetMapping("/manager/leave/requests")
    public String managerManageLeaves(HttpSession session, Model model) {
        User manager = (User) session.getAttribute("user");
        if (manager == null || !"MANAGER".equals(manager.getRole())) return "redirect:/login";

        // 1. Get the manager's team
        List<User> myTeam = userRepository.findByManager(manager);
        
        // 2. Load the PENDING requests for the top table
        List<LeaveRequest> pending = leaveRepository.findByUserInOrderByCreatedAtDesc(myTeam).stream()
                .filter(l -> "PENDING".equals(l.getStatus()))
                .toList();

        // 3. Load the PROCESSED requests (Approved/Rejected) for the History table
        List<LeaveRequest> history = leaveRepository.findByUserInOrderByCreatedAtDesc(myTeam).stream()
                .filter(l -> !"PENDING".equals(l.getStatus()))
                .toList();

        // 4. Add them to the model (These match the variable names in your error images)
        model.addAttribute("pendingLeaves", pending);
        model.addAttribute("leaveHistory", history);
        model.addAttribute("viewTitle", "Team Leave Approvals");
        
        // Timer Logic
        LocalDate today = LocalDate.now();
        attendanceRepository.findByUserAndDate(manager, today).stream()
            .filter(log -> log.getCheckOut() == null)
            .max(Comparator.comparing(com.example.crm.entity.AttendanceLog::getCheckIn))
            .ifPresent(log -> model.addAttribute("checkInTime", log.getCheckIn().toString()));

        return "manager_leave_requests";
    }
    
    
    @GetMapping("/leave/apply")
    public String showLeaveForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        User currentUser = userRepository.findById(user.getId()).orElse(user);
        
        // 1. Fetch or create the employee profile (Balances)
        EmployeeProfile profile = profileRepository.findByUser(currentUser)
                                  .orElse(new EmployeeProfile());
        
        model.addAttribute("profile", profile);
        
        // 2. Fetch personal leave history for the table
        model.addAttribute("myHistory", leaveRepository.findByUserOrderByCreatedAtDesc(currentUser));

        // --- FIX: POPULATE SIDEBAR DATA ---
        List<LeaveRequest> pendingLeaves = new java.util.ArrayList<>();
        
        if ("MANAGER".equals(currentUser.getRole())) {
            // Find requests from this manager's specific team
            List<User> team = userRepository.findByManager(currentUser);
            pendingLeaves = leaveRepository.findByUserInOrderByCreatedAtDesc(team).stream()
                              .filter(l -> "PENDING".equals(l.getStatus()))
                              .toList();
        }
        
        // This ensures Thymeleaf finds an empty list instead of NULL, preventing the crash
        model.addAttribute("pendingLeaves", pendingLeaves);
        // ---------------------------------
        
        

        return "MANAGER".equals(currentUser.getRole()) ? "manager_leave_apply" : "executive_leave_apply";
    }
    
    @PostMapping("/leave/action")
    public String updateLeaveStatus(@RequestParam Long id, @RequestParam String action, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) return "redirect:/login";

        // 1. Process the Leave
        leaveRepository.findById(id).ifPresent(leave -> {
            leave.setStatus(action.toUpperCase()); // APPROVE or REJECT
            leaveRepository.save(leave);
            
            // If approved, deduct from EmployeeProfile balance logic goes here
            
         // Notify the requester (Executive)
            User requester = leave.getUser();
            if (requester.getEmail() != null) {
                String color = action.equalsIgnoreCase("APPROVE") ? "#10b981" : "#f43f5e";
                String content = "<p>Hello <b>" + requester.getName() + "</b>,</p>" +
                        "<p>Your leave request has been processed by <b>" + currentUser.getName() + "</b>.</p>" +
                        "<div style='display: inline-block; padding: 10px 20px; border-radius: 8px; color: white; font-weight: bold; background: " + color + ";'>" +
                        action.toUpperCase() + "</div>";
                
                sendEmail(currentUser.getEmail(), currentUser.getName(), requester.getEmail(), "Leave Request: " + action.toUpperCase(), content);
            }
            
        });

        // 2. Redirect back to the role-specific management page
        // This ensures the browser URL updates and the GET mapping handles the sidebar data
        if ("ADMIN".equals(currentUser.getRole())) {
            return "redirect:/admin/leave/requests?success=action_completed";
        } else {
            return "redirect:/manager/leave/requests?success=action_completed";
        }
    }
    
    @GetMapping("/admin/leave/requests")
    public String adminManageLeaves(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !"ADMIN".equals(admin.getRole())) return "redirect:/login";

        // 1. Fetch PENDING requests from MANAGERS
        List<LeaveRequest> managerRequests = leaveRepository.findAll().stream()
            .filter(l -> "MANAGER".equals(l.getUser().getRole()) && "PENDING".equals(l.getStatus()))
            .toList();

        // 2. NEW: Fetch PROCESSED (Approved/Rejected) requests from MANAGERS for history
        List<LeaveRequest> history = leaveRepository.findAll().stream()
            .filter(l -> "MANAGER".equals(l.getUser().getRole()) && !"PENDING".equals(l.getStatus()))
            .toList();

        model.addAttribute("pendingLeaves", managerRequests);
        model.addAttribute("leaveHistory", history); // Add this to the model
        model.addAttribute("viewTitle", "Manager Leave Approvals");
        
        // Logic to keep the timer working
        LocalDate today = LocalDate.now();
        attendanceRepository.findByUserAndDate(admin, today).stream()
            .filter(log -> log.getCheckOut() == null)
            .max(Comparator.comparing(com.example.crm.entity.AttendanceLog::getCheckIn))
            .ifPresent(log -> model.addAttribute("checkInTime", log.getCheckIn().toString()));

        return "admin_leave_requests"; 
    }
    @PostMapping("/leave/cancel")
    public String cancelLeave(@RequestParam Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        leaveRepository.findById(id).ifPresent(leave -> {
            // Security check: Ensure only the owner can cancel their own pending leave
            if (leave.getUser().getId().equals(user.getId()) && "PENDING".equals(leave.getStatus())) {
                leaveRepository.delete(leave);
            }
        });

        return "redirect:/leave/apply?success=cancelled";
    }
    
 // --- HR ACTION: VIEW APPLY PAGE ---
    @GetMapping("/hr/leave/apply")
    public String showHrLeaveForm(HttpSession session, Model model) {
        User hr = (User) session.getAttribute("user");
        if (hr == null) return "redirect:/login";
        
        List<LeaveRequest> myLeaves = leaveRepository.findByUserOrderByCreatedAtDesc(hr);
        model.addAttribute("myLeaves", myLeaves);
        return "hr_apply_leave";
    }

    // --- HR ACTION: SUBMIT REQUEST ---
    @PostMapping("/hr/leave/submit")
    public String submitHrLeave(@ModelAttribute LeaveRequest leave, HttpSession session) {
        User hr = (User) session.getAttribute("user");
        leave.setUser(hr);
        leave.setStatus("PENDING");
        leave.setCreatedAt(LocalDateTime.now());
        leaveRepository.save(leave);
        return "redirect:/hr/leave/apply?success";
    }

    // --- MANAGER ACTION: VIEW PENDING REQUESTS ---
    @GetMapping("/manager/hr-leaves")
    public String viewHrLeaves(HttpSession session, Model model) {
        User manager = (User) session.getAttribute("user");
        // Fetch leaves of users (HRs) where the manager is the current logged-in manager
        List<LeaveRequest> requests = leaveRepository.findAll().stream()
            .filter(l -> l.getUser().getManager() != null && l.getUser().getManager().getId().equals(manager.getId()))
            .filter(l -> "HR".equals(l.getUser().getRole()))
            .toList();
            
        model.addAttribute("requests", requests);
        return "manager_review_leaves";
    }

    // --- MANAGER ACTION: APPROVE/REJECT ---
    @GetMapping("/manager/leave/action/{id}/{status}")
    public String handleLeaveAction(@PathVariable Long id, @PathVariable String status) {
        leaveRepository.findById(id).ifPresent(l -> {
            l.setStatus(status);
            leaveRepository.save(l);
        });
        return "redirect:/manager/hr-leaves?updated";
    }
    
    @GetMapping("/hr/leave/cancel/{id}")
    public String cancelLeave(@PathVariable Long id) {
        // 1. Delete the leave request from the database
        leaveRepository.deleteById(id);
        
        // 2. Redirect back to the apply page with a success flag
        return "redirect:/hr/leave/apply?success=cancelled";
    }
}