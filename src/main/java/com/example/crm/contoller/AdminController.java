package com.example.crm.contoller;

import com.example.crm.entity.Lead;
import com.example.crm.entity.LeaveRequest;
import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import com.example.crm.entity.Employee;
import com.example.crm.repository.AttendanceRepository;
import com.example.crm.repository.EmployeeRepository;
import com.example.crm.repository.LeadRepository;
import com.example.crm.repository.LeaveRepository;
import com.example.crm.repository.TaskRepository;
import com.example.crm.repository.TransactionRepository;
import com.example.crm.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

@RestController
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private LeaveRepository leaveRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LeadRepository getLeadRepository() {
        return leadRepository;
    }

    public void setLeadRepository(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public LeaveRepository getLeaveRepository() {
        return leaveRepository;
    }

    public void setLeaveRepository(LeaveRepository leaveRepository) {
        this.leaveRepository = leaveRepository;
    }

    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    public void setTransactionRepository(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public AttendanceRepository getAttendanceRepository() {
        return attendanceRepository;
    }

    public void setAttendanceRepository(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private com.example.crm.service.EmailService emailService;

    @GetMapping("/api/admin/users")
    public List<User> apiGetUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/api/admin/add-user")
    public org.springframework.http.ResponseEntity<?> apiAddUser(@RequestBody Map<String, Object> payload) {
        String email = payload.get("email") == null ? "" : payload.get("email").toString().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            Map<String, String> errors = new HashMap<>();
            errors.put("error", "Email already exists");
            return org.springframework.http.ResponseEntity.badRequest().body(errors);
        }

        User user = new User();
        user.setName((String) payload.get("name"));
        user.setEmail(email);
        user.setPassword((String) payload.get("password"));
        String role = payload.get("role") == null ? "EXECUTIVE" : payload.get("role").toString().trim().toUpperCase();
        user.setRole(role);
        user.setPosition((String) payload.get("position"));

        if (payload.get("salary") != null && !payload.get("salary").toString().isEmpty()) {
            user.setSalary(Double.valueOf(payload.get("salary").toString()));
        }

        if (payload.get("dateJoined") != null && !payload.get("dateJoined").toString().isEmpty()) {
            user.setDateOfJoining(java.time.LocalDate.parse(payload.get("dateJoined").toString()));
        }

        if (payload.get("targetAmount") != null && !payload.get("targetAmount").toString().isEmpty()) {
            user.setTargetAmount(Double.valueOf(payload.get("targetAmount").toString()));
        }

        if (payload.get("managerId") != null && !payload.get("managerId").toString().isEmpty()) {
            Long managerId = Long.valueOf(payload.get("managerId").toString());
            userRepository.findById(managerId).ifPresent(user::setManager);
        }

        User savedUser = userRepository.save(user);
        syncEmployeeForUser(savedUser, payload);
        if (savedUser.getEmail() != null && !savedUser.getEmail().trim().isEmpty()) {
            emailService.sendUserCredentials(
                savedUser.getEmail().trim(),
                savedUser.getName(),
                savedUser.getEmail().trim(),
                savedUser.getPassword()
            );
        }
        return org.springframework.http.ResponseEntity.ok(savedUser);
    }

    private void syncEmployeeForUser(User user, Map<String, Object> payload) {
        if (user == null || user.getEmail() == null || "ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }

        Employee employee = employeeRepository.findByEmailIgnoreCase(user.getEmail());
        if (employee == null) {
            employee = new Employee();
            employee.setEmployeeId(nextEmployeeId());
            employee.setEmail(user.getEmail());
            employee.setStatus("Active");
        }

        employee.setName(user.getName());
        employee.setPassword(user.getPassword());
        employee.setRole(user.getRole());
        employee.setSalary(user.getSalary());
        employee.setJoiningDate(user.getDateOfJoining() != null ? user.getDateOfJoining() : LocalDate.now());
        employee.setPhone(textValue(payload.get("phone"), "+966 50 000 0000"));
        employee.setDepartment(textValue(payload.get("department"), defaultDepartment(user.getRole())));

        employeeRepository.save(employee);
    }

    private String nextEmployeeId() {
        long next = employeeRepository.count() + 101;
        String employeeId = "EMP-" + next;
        while (employeeRepository.existsByEmployeeId(employeeId)) {
            next++;
            employeeId = "EMP-" + next;
        }
        return employeeId;
    }

    private String defaultDepartment(String role) {
        if (role == null) return "Sales Execution";
        String normalized = role.trim().toUpperCase();
        if ("HR".equals(normalized) || "HR_HEAD".equals(normalized)) {
            return "Human Resources";
        }
        if ("MANAGER".equals(normalized) || "BDE_MANAGER".equals(normalized) || "ASSISTANT_MANAGER".equals(normalized)) {
            return "Sales Management";
        }
        if ("TRAINER".equals(normalized) || "TECH_LEAD".equals(normalized)) {
            return "Training & Development";
        }
        return "Sales Execution";
    }

    private String textValue(Object value, String fallback) {
        if (value == null || value.toString().trim().isEmpty()) {
            return fallback;
        }
        return value.toString().trim();
    }

    private boolean isNotLoggedIn(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user == null || !"ADMIN".equals(user.getRole());
    }

    /**
     * CREATE MANAGER with Duplicate Prevention
     */
    @PostMapping("/admin/add-manager")
    public String addManager(@ModelAttribute User user) {
        if (userRepository.existsByEmailIgnoreCase(user.getEmail().trim())) {
            return "redirect:/admin/manage-managers?error=email_exists";
        }
        user.setEmail(user.getEmail().trim());
        user.setRole("MANAGER");
        userRepository.save(user);
        return "redirect:/admin/manage-managers?success=manager_added";
    }

    /**
     * CREATE EXECUTIVE with Duplicate Prevention
     * FIXED: Single method handling both Admin and Manager origins
     */
    @PostMapping("/admin/add-executive")
    public String addExecutive(@ModelAttribute User user,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) String origin) {

        // Check for duplicate email
        if (userRepository.existsByEmailIgnoreCase(user.getEmail().trim())) {
            if ("manager".equals(origin))
                return "redirect:/manager/executives?error=email_exists";
            return "redirect:/admin/manage-executives?error=email_exists";
        }

        user.setEmail(user.getEmail().trim());
        user.setRole("EXECUTIVE");
        if (managerId != null) {
            userRepository.findById(managerId).ifPresent(user::setManager);
        }
        userRepository.save(user);

        // Smart Redirection
        if ("manager".equals(origin)) {
            return "redirect:/manager/executives?success=executive_added";
        }
        return "redirect:/admin/manage-executives?success=executive_added";
    }

    /**
     * UPDATE OPERATIONS
     */
    @PostMapping("/admin/update-manager")
    public String updateManager(@RequestParam Long id, @RequestParam String name, @RequestParam String email) {
        userRepository.findById(id).ifPresent(user -> {
            user.setName(name);
            user.setEmail(email);
            userRepository.save(user);
        });
        return "redirect:/admin/manage-managers?success=updated";
    }

    /**
     * UPDATE GENERAL USER (Used by Team Roster and Dashboards)
     * FIXED: Combined logic with origin redirection
     */
    @PostMapping("/admin/update-user")
    public String updateUser(@RequestParam Long id,
            @RequestParam String name,
            @RequestParam(required = false) String origin) {
        userRepository.findById(id).ifPresent(user -> {
            user.setName(name);
            userRepository.save(user);
        });

        if ("manager".equals(origin)) {
            return "redirect:/manager/executives?success=updated";
        }
        return "redirect:/admin/dashboard?success=updated";
    }

    @PostMapping("/admin/update-executive")
    public String updateExecutive(@RequestParam Long id, @RequestParam String name, @RequestParam String email,
            @RequestParam Long managerId, @RequestParam(required = false) String origin) {
        User exec = userRepository.findById(id).orElseThrow();
        User manager = userRepository.findById(managerId).orElseThrow();
        exec.setName(name);
        exec.setEmail(email);
        exec.setManager(manager);
        userRepository.save(exec);

        // Redirect back to Manage HR if that's where the request started
        if ("manage-hr".equals(origin)) {
            return "redirect:/admin/manage-hr?success=updated";
        }
        return "redirect:/admin/manage-executives?success=updated";
    }

    @PostMapping("/admin/assign-manager")
    public String assignManager(@RequestParam Long executiveId, @RequestParam Long managerId) {
        userRepository.findById(executiveId).ifPresent(executive -> {
            userRepository.findById(managerId).ifPresent(manager -> {
                executive.setManager(manager);
                userRepository.save(executive);
            });
        });
        return "redirect:/admin/manage-executives?success=assigned";
    }

    /**
     * DELETE OPERATIONS
     */
    @GetMapping("/admin/delete-user/{id}")
    public String deleteUser(@PathVariable Long id, @RequestParam(required = false) String origin) {
        User user = userRepository.findById(id).orElse(null);
        String role = (user != null) ? user.getRole() : "";

        userRepository.deleteById(id);
        // Check if the request came from the Manage HR page
        if ("manage-hr".equals(origin)) {
            return "redirect:/admin/manage-hr?success=deleted";
        }

        // Smart redirect based on role
        if ("MANAGER".equals(role)) {
            return "redirect:/admin/manage-managers?success=deleted";
        }
        return "redirect:/admin/dashboard?success=deleted";
    }

    @GetMapping("/admin/delete-exec/{id}")
    public String deleteExecutive(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin/manage-executives?success=deleted";
    }

    @GetMapping("/manager/delete-exec/{id}")
    public String managerDeleteExecutive(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/manager/executives?success=deleted";
    }

    /**
     * PROFILE MANAGEMENT
     */
    @GetMapping("/admin/profile")
    public String adminProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";
        model.addAttribute("admin", user);
        return "admin_profile";
    }

    @PostMapping("/admin/update-profile")
    public String updateAdminProfile(@RequestParam String name,
            @RequestParam String email,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            HttpSession session) throws IOException {
        User sessionUser = (User) session.getAttribute("user");
        User user = userRepository.findById(sessionUser.getId()).orElseThrow();

        user.setName(name);
        user.setEmail(email);

        if (profileImage != null && !profileImage.isEmpty()) {
            user.setProfilePicture(Base64.getEncoder().encodeToString(profileImage.getBytes()));
        }

        userRepository.save(user);
        session.setAttribute("user", user);

        if ("EXECUTIVE".equals(user.getRole()))
            return "redirect:/executive/profile?success=profile_updated";
        if ("MANAGER".equals(user.getRole()))
            return "redirect:/manager/profile?success=profile_updated";

        return "redirect:/admin/profile?success=profile_updated";
    }

    @PostMapping("/admin/reset-password")
    public String resetPassword(@RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        User user = userRepository.findById(sessionUser.getId()).orElseThrow();

        String redirectPath = "/admin/profile";
        if ("EXECUTIVE".equals(user.getRole()))
            redirectPath = "/executive/profile";
        if ("MANAGER".equals(user.getRole()))
            redirectPath = "/manager/profile";

        if (!user.getPassword().equals(currentPassword)) {
            return "redirect:" + redirectPath + "?error=wrong_current_password";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:" + redirectPath + "?error=password_mismatch";
        }

        user.setPassword(newPassword);
        userRepository.save(user);
        return "redirect:" + redirectPath + "?success=password_updated";
    }

    @PostMapping("/admin/remove-photo")
    public String removePhoto(HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        User user = userRepository.findById(sessionUser.getId()).orElseThrow();

        user.setProfilePicture(null);
        userRepository.save(user);
        session.setAttribute("user", user);

        if ("EXECUTIVE".equals(user.getRole()))
            return "redirect:/executive/profile?success=photo_removed";
        if ("MANAGER".equals(user.getRole()))
            return "redirect:/manager/profile?success=photo_removed";

        return "redirect:/admin/profile?success=photo_removed";
    }

    // ***********************************************
    @GetMapping("/admin/sales")
    public String viewAllSales(Model model) {
        // Fetch all leads where status is 'Close'
        List<Lead> closedLeads = leadRepository.findAll().stream()
                .filter(l -> "Close".equalsIgnoreCase(l.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("leads", closedLeads);
        model.addAttribute("managerCount", userRepository.findByRole("MANAGER").size());
        return "admin_sales";
    }

    @GetMapping("/admin/manage-hr")
    public String manageHrPage(Model model, HttpSession session) {
        // 1. Basic security check (Optional but recommended)
        User admin = (User) session.getAttribute("user");
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return "redirect:/login";
        }

        // 2. Fetch all users with role 'HR' for the table
        List<User> hrList = userRepository.findByRole("HR");

        // 3. Fetch all users with role 'MANAGER' for the dropdown
        List<User> managers = userRepository.findByRole("MANAGER");

        // 2. CRITICAL FIX: Fetch pending leaves for the sidebar notification dot
        List<LeaveRequest> pendingAdmin = leaveRepository.findAll().stream()
                .filter(l -> "MANAGER".equals(l.getUser().getRole()) && "PENDING".equals(l.getStatus()))
                .toList();

        // 4. Add data to the UI model
        model.addAttribute("hrList", hrList);
        model.addAttribute("managers", managers);

        return "admin_manage_hr"; // This must match your HTML filename
    }

    @PostMapping("/admin/hr/register")
    public String registerHr(@RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam Long managerId) {

        User hr = new User();
        hr.setName(name);
        hr.setEmail(email);
        hr.setPassword(password);
        hr.setRole("HR");

        // Assign the selected Manager to this HR
        User manager = userRepository.findById(managerId).orElse(null);
        hr.setManager(manager);

        userRepository.save(hr);

        return "redirect:/admin/manage-hr?success";
    }

    @GetMapping("/admin/finance/delete/{id}")
    public String deleteTransaction(@PathVariable Long id) {
        // 1. Delete from database
        transactionRepository.deleteById(id);

        // 2. Redirect back with a success flag for the popup
        return "redirect:/admin/finance/terminal?success=deleted";
    }

    @GetMapping("/admin/manager-leads")
    public String viewManagerLeads(@RequestParam(required = false) Long managerId, Model model, HttpSession session) {
        // 1. Get all managers for the sidebar
        List<User> managers = userRepository.findByRole("MANAGER");
        model.addAttribute("managers", managers);

        if (managerId != null) {
            User selectedManager = userRepository.findById(managerId).orElse(null);
            if (selectedManager != null) {
                // Logic: Show leads owned by the manager OR their subordinate team
                List<Lead> managerLeads = leadRepository.findAll().stream()
                        .filter(l -> l.getUser() != null && (l.getUser().getId().equals(managerId) || // Manager's own
                                                                                                      // leads
                                (l.getUser().getManager() != null && l.getUser().getManager().getId().equals(managerId)) // Subordinates'
                                                                                                                         // leads
                        ))
                        .toList();

                model.addAttribute("selectedManager", selectedManager);
                model.addAttribute("leads", managerLeads);
            }
        }

        // Ensure pending leaves are passed for the sidebar notification dot
        model.addAttribute("pendingLeaves", leaveRepository.findByStatus("PENDING"));

        return "admin_manager_leads";
    }

    @GetMapping("/admin/leads/report")
    public String leadReportPage(Model model, HttpSession session) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        // 1. Fetch all managers for the dropdown
        model.addAttribute("managers", userRepository.findByRole("MANAGER"));

        // 2. Fetch all leads for the preview table
        model.addAttribute("allLeads", leadRepository.findAll());

        // 3. Keep sidebar notification dot working
        model.addAttribute("pendingLeaves", leaveRepository.findByStatus("PENDING"));

        return "admin_leads_report";
    }

    @GetMapping("/admin/leads/download")
    public void downloadLeads(
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam String scope,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) String teamMode,
            @RequestParam(required = false) Long executiveId,
            HttpServletResponse response) throws IOException {

        // 1. Convert String dates to LocalDate
        LocalDate start = LocalDate.parse(fromDate);
        LocalDate end = LocalDate.parse(toDate);

        // 2. Fetch and filter leads
        List<Lead> filteredLeads = leadRepository.findAll().stream()
                .filter(l -> l.getCreatedAt() != null &&
                        !l.getCreatedAt().toLocalDate().isBefore(start) &&
                        !l.getCreatedAt().toLocalDate().isAfter(end))
                .filter(l -> {
                    if ("ALL".equals(scope))
                        return true;
                    if (managerId == null)
                        return false;

                    if ("MANAGER_ONLY".equals(teamMode)) {
                        return l.getUser().getId().equals(managerId);
                    } else if ("FULL_TEAM".equals(teamMode)) {
                        return l.getUser().getId().equals(managerId) ||
                                (l.getUser().getManager() != null
                                        && l.getUser().getManager().getId().equals(managerId));
                    } else if ("EXECUTIVE_ONLY".equals(teamMode) && executiveId != null) {
                        return l.getUser().getId().equals(executiveId);
                    }
                    return false;
                })
                .toList();

        // 3. Set response headers for CSV download
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=leads_report_" + fromDate + "_to_" + toDate + ".csv");

        // 4. Write CSV Data
        PrintWriter writer = response.getWriter();
        writer.println("Date,Client Name,Phone,Source,Status,Added By,Role"); // CSV Header

        for (Lead l : filteredLeads) {
            writer.println(String.format("%s,%s,%s,%s,%s,%s,%s",
                    l.getCreatedAt().toLocalDate(),
                    l.getCustomerName().replace(",", " "), // Prevent comma breaking CSV
                    l.getPhoneNumber(),
                    l.getSource(),
                    l.getStatus(),
                    l.getUser() != null ? l.getUser().getName() : "System",
                    l.getUser() != null ? l.getUser().getRole() : "-"));
        }
        writer.flush();
        writer.close();
    }

    @GetMapping("/admin/tasks/assign")
    public String assignTaskPage(Model model, HttpSession session) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        model.addAttribute("managers", userRepository.findByRole("MANAGER"));

        // Logic: Admin tasks only have a 'manager' assigned and 'executive' is null.
        // We filter the list so Executives tasks don't show up here.
        List<Task> adminToManagerTasks = taskRepository.findAll().stream()
                .filter(t -> t.getManager() != null && t.getExecutive() == null)
                .collect(Collectors.toList());

        model.addAttribute("tasks", adminToManagerTasks);
        model.addAttribute("pendingLeaves", leaveRepository.findByStatus("PENDING"));

        return "admin_assign_task";
    }

    @PostMapping("/admin/tasks/save")
    public String saveTask(@RequestParam Long managerId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam LocalDate dueDate) {
        User manager = userRepository.findById(managerId).orElseThrow();
        Task task = new Task();
        task.setManager(manager);
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.setStatus("PENDING");
        taskRepository.save(task);
        return "redirect:/admin/tasks/assign?success";
    }

    @GetMapping("/admin/delete-task/{id}")
    public String adminDeleteTask(@PathVariable Long id) {
        taskRepository.deleteById(id);
        // Redirects strictly back to the Admin Assign Task page
        return "redirect:/admin/tasks/assign?success=deleted";
    }

    @PostMapping("/admin/tasks/update")
    public String updateTask(@RequestParam Long taskId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam LocalDate dueDate,
            @RequestParam String status) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.setStatus(status);
        taskRepository.save(task);
        return "redirect:/admin/tasks/assign?success=updated";
    }

    @GetMapping("/admin/performance/managers")
    public String adminManagerPerformance(Model model, HttpSession session) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        List<User> managers = userRepository.findByRole("MANAGER");
        List<Lead> allLeads = leadRepository.findAll();

        List<Map<String, Object>> performanceList = managers.stream().map(manager -> {
            // A. Team & Manager Leads
            List<User> team = userRepository.findByManager(manager);
            List<Lead> teamLeads = allLeads.stream()
                    .filter(l -> l.getUser() != null && (l.getUser().getId().equals(manager.getId()) ||
                            (l.getUser().getManager() != null
                                    && l.getUser().getManager().getId().equals(manager.getId()))))
                    .toList();

            // B. Calculate Sales Revenue (Only status 'Close')
            double revenue = teamLeads.stream()
                    .filter(l -> "Close".equalsIgnoreCase(l.getStatus()) && l.getNotes() != null
                            && l.getNotes().contains("₹"))
                    .mapToDouble(l -> {
                        try {
                            String amtStr = l.getNotes().split("₹")[1].split(" \\|")[0].replaceAll("[^0-9.]", "");
                            return Double.parseDouble(amtStr);
                        } catch (Exception e) {
                            return 0.0;
                        }
                    }).sum();

            // C. Calculate Completed Tasks (Assigned by Admin to this Manager)
            long tasksDone = taskRepository.findAll().stream()
                    .filter(t -> t.getManager() != null
                            && t.getManager().getId().equals(manager.getId())
                            && t.getExecutive() == null
                            && "COMPLETED".equals(t.getStatus()))
                    .count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("name", manager.getName());
            stats.put("teamSize", team.size());
            stats.put("leadCount", teamLeads.size());
            stats.put("revenue", revenue);
            stats.put("tasksDone", tasksDone);
            return stats;
        })
                .sorted((m1, m2) -> Double.compare((double) m2.get("revenue"), (double) m1.get("revenue")))
                .collect(Collectors.toList());

        model.addAttribute("managerCount", managers.size());
        model.addAttribute("totalLeads", allLeads.size());
        model.addAttribute("performanceList", performanceList);

        // Existing timer and notification logic remains here...
        return "admin_manager_performance";
    }
}
