package com.example.crm.contoller;

import com.example.crm.entity.AttendanceLog;
import com.example.crm.entity.Lead;
import com.example.crm.entity.LeaveRequest;
import com.example.crm.entity.User;
import com.example.crm.entity.Task;
import com.example.crm.repository.AttendanceRepository;
import com.example.crm.repository.LeadRepository;
import com.example.crm.repository.LeaveRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.repository.TaskRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class DashboardController {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AttendanceController attendanceController;

    public LeadRepository getLeadRepository() {
        return leadRepository;
    }

    public void setLeadRepository(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public TaskRepository getTaskRepository() {
        return taskRepository;
    }

    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public AttendanceController getAttendanceController() {
        return attendanceController;
    }

    public void setAttendanceController(AttendanceController attendanceController) {
        this.attendanceController = attendanceController;
    }

    public AttendanceRepository getAttendanceRepository() {
        return attendanceRepository;
    }

    public void setAttendanceRepository(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public LeaveRepository getLeaveRepository() {
        return leaveRepository;
    }

    public void setLeaveRepository(LeaveRepository leaveRepository) {
        this.leaveRepository = leaveRepository;
    }

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    private boolean isNotLoggedIn(HttpSession session) {
        return session.getAttribute("user") == null;
    }

    /**
     * ADMIN DASHBOARD
     */
    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        List<Lead> allLeads = leadRepository.findAll();
        List<User> managers = userRepository.findByRole("MANAGER");

        model.addAttribute("managerCount", managers.size());
        model.addAttribute("executiveCount", userRepository.findByRole("EXECUTIVE").size());
        model.addAttribute("totalLeads", allLeads.size());

        List<Map<String, Object>> managerPerformance = managers.stream().map(manager -> {
            List<User> team = userRepository.findByManager(manager);
            long leadCount = team.isEmpty() ? 0 : leadRepository.findByUserIn(team).size();

            Map<String, Object> stats = new HashMap<>();
            stats.put("name", manager.getName());
            stats.put("teamSize", team.size());
            stats.put("leadCount", leadCount);
            return stats;
        })
                .sorted((m1, m2) -> Long.compare((long) m2.get("leadCount"), (long) m1.get("leadCount")))
                .collect(Collectors.toList());

        model.addAttribute("performanceList", managerPerformance);

        Map<String, Long> sourceCounts = allLeads.stream()
                .collect(Collectors.groupingBy(l -> l.getSource() == null ? "Unknown" : l.getSource(),
                        Collectors.counting()));
        model.addAttribute("sourceLabels", sourceCounts.keySet());
        model.addAttribute("sourceData", sourceCounts.values());

        Map<String, Long> funnelCounts = allLeads.stream()
                .collect(Collectors.groupingBy(l -> l.getStatus() == null ? "New" : l.getStatus(),
                        Collectors.counting()));
        model.addAttribute("funnelLabels", funnelCounts.keySet());
        model.addAttribute("funnelData", funnelCounts.values());

        // --- ADD THIS TO BOTH ADMIN & MANAGER METHODS ---
        // --- UPDATED ATTENDANCE LOGIC FOR LATEST LOGIN ---
        LocalDate today = LocalDate.now();
        User sessionUser = (User) session.getAttribute("user");

        attendanceRepository.findByUserAndDate(sessionUser, today).stream()
                .filter(log -> log.getCheckOut() == null) // Only active sessions
                .max(Comparator.comparing(AttendanceLog::getCheckIn)) // Get the LATEST one
                .ifPresent(log -> {
                    // Convert to String for reliable JS parsing
                    model.addAttribute("checkInTime", log.getCheckIn().toString());
                });
        // ------------------------------------------------
        // Inside Admin Dashboard Mapping
        List<LeaveRequest> pendingAdmin = leaveRepository.findAll().stream()
                .filter(l -> "MANAGER".equals(l.getUser().getRole()) && "PENDING".equals(l.getStatus()))
                .toList();
        model.addAttribute("pendingLeaves", pendingAdmin);

        return "admin_dashboard";
    }

    @GetMapping("/admin/manage-managers")
    public String manageManagers(Model model, HttpSession session) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        // 1. Fetch all managers
        List<User> managers = userRepository.findByRole("MANAGER");

        // 2. Calculate subordinates for each manager
        // This tells the frontend if the "Action Restricted" modal should show
        managers.forEach(m -> {
            // Count all users (HR, Executives) who report to this specific manager
            long count = userRepository.findAll().stream()
                    .filter(u -> u.getManager() != null && u.getManager().getId().equals(m.getId()))
                    .count();
            m.setSubordinateCount((int) count);
        });

        model.addAttribute("managers", managers);

        // Ensure the timer still works by passing the session check-in time
        model.addAttribute("checkInTime", session.getAttribute("checkInTime"));

        return "manage_managers";
    }

    @GetMapping("/admin/manage-executives")
    public String adminManageExecutives(Model model, HttpSession session) {
        if (isNotLoggedIn(session))
            return "redirect:/login";
        model.addAttribute("executives", userRepository.findByRole("EXECUTIVE"));
        model.addAttribute("managers", userRepository.findByRole("MANAGER"));
        return "admin_manage_executives";
    }

    /**
     * MANAGER DASHBOARD
     */
    @GetMapping("/manager/dashboard")
    public String managerDashboard(HttpSession session, Model model) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        User sessionUser = (User) session.getAttribute("user");
        User currentManager = userRepository.findById(sessionUser.getId()).orElse(sessionUser);

        List<User> myTeam = userRepository.findByManager(currentManager);
        List<Lead> teamLeads = new ArrayList<>();

        if (!myTeam.isEmpty()) {
            teamLeads.addAll(leadRepository.findByUserIn(myTeam));
        }
        teamLeads.addAll(leadRepository.findByUser(currentManager));
        teamLeads.sort(Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        model.addAttribute("manager", currentManager);
        model.addAttribute("leads", teamLeads);

        Map<String, Long> sourceCounts = teamLeads.stream()
                .collect(Collectors.groupingBy(l -> l.getSource() == null ? "Unknown" : l.getSource(),
                        Collectors.counting()));
        model.addAttribute("sourceLabels", sourceCounts.keySet());
        model.addAttribute("sourceData", sourceCounts.values());

        // --- ADD THIS TO BOTH ADMIN & MANAGER METHODS ---
        // Logic to get the LATEST active check-in time for the current user today
        LocalDate today = LocalDate.now();
        User sessionUser1 = (User) session.getAttribute("user");

        attendanceRepository.findByUserAndDate(sessionUser1, today).stream()
                .filter(log -> log.getCheckOut() == null)
                .max(Comparator.comparing(AttendanceLog::getCheckIn)) // Get the very last login
                .ifPresent(log -> {
                    model.addAttribute("checkInTime", log.getCheckIn().toString());
                });
        // ------------------------------------------------

        List<User> myTeam1 = userRepository.findByManager(currentManager);
        List<LeaveRequest> pendingManager = leaveRepository.findByUserInOrderByCreatedAtDesc(myTeam1).stream()
                .filter(l -> "PENDING".equals(l.getStatus()))
                .toList();
        model.addAttribute("pendingLeaves", pendingManager);

        return "manager_dashboard";
    }

    /**
     * EXECUTIVE DASHBOARD
     */
    /**
     * @GetMapping("/executive/dashboard")
     * public String executiveDashboard(HttpSession session, Model model) {
     * if (isNotLoggedIn(session)) return "redirect:/login";
     * 
     * User sessionUser = (User) session.getAttribute("user");
     * User currentUser =
     * userRepository.findById(sessionUser.getId()).orElse(sessionUser);
     * 
     * List<Lead> myLeads = leadRepository.findByUser(currentUser);
     * myLeads.sort(Comparator.comparing(Lead::getCreatedAt,
     * Comparator.nullsLast(Comparator.reverseOrder())));
     * 
     * LocalDate today = LocalDate.now();
     * 
     * long overdueCount = myLeads.stream()
     * .filter(l -> l.getFollowUpDate() != null
     * && l.getFollowUpDate().isBefore(today)
     * && "PENDING".equalsIgnoreCase(l.getFollowUpStatus()))
     * .count();
     * 
     * long pendingCount = myLeads.stream()
     * .filter(l -> l.getFollowUpDate() != null
     * && !l.getFollowUpDate().isBefore(today)
     * && "PENDING".equalsIgnoreCase(l.getFollowUpStatus()))
     * .count();
     * 
     * model.addAttribute("executive", currentUser);
     * model.addAttribute("leads", myLeads);
     * model.addAttribute("activeCount", myLeads.size());
     * model.addAttribute("overdueCount", overdueCount);
     * model.addAttribute("pendingCount", pendingCount);
     * 
     * Map<String, Long> sourceCounts = myLeads.stream()
     * .collect(Collectors.groupingBy(l -> l.getSource() == null ? "Unknown" :
     * l.getSource(), Collectors.counting()));
     * model.addAttribute("sourceLabels", sourceCounts.keySet());
     * model.addAttribute("sourceData", sourceCounts.values());
     * 
     * return "executive_dashboard";
     * }
     */

    @GetMapping("/manager/pipeline")
    public String salesPipeline(HttpSession session, Model model) {
        if (isNotLoggedIn(session))
            return "redirect:/login";
        User currentManager = (User) session.getAttribute("user");
        List<User> myTeam = userRepository.findByManager(currentManager);

        List<Lead> teamLeads = new ArrayList<>();
        if (!myTeam.isEmpty()) {
            teamLeads.addAll(leadRepository.findByUserIn(myTeam));
        }
        teamLeads.addAll(leadRepository.findByUser(currentManager));

        Map<String, Long> pipelineCounts = teamLeads.stream()
                .map(l -> l.getStatus() == null || l.getStatus().isEmpty() ? "New" : l.getStatus())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        model.addAttribute("leads", teamLeads);
        model.addAttribute("pipelineLabels", pipelineCounts.keySet());
        model.addAttribute("pipelineData", pipelineCounts.values());
        return "sales_pipeline";
    }

    @GetMapping("/manager/lead-capture")
    public String managerLeadCapture(HttpSession session, Model model) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        User currentManager = (User) session.getAttribute("user");
        List<User> myTeam = userRepository.findByManager(currentManager);

        List<Lead> combinedLeads = new ArrayList<>();
        if (!myTeam.isEmpty()) {
            combinedLeads.addAll(leadRepository.findByUserIn(myTeam));
        }
        combinedLeads.addAll(leadRepository.findByUser(currentManager));
        combinedLeads.sort(Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        model.addAttribute("leads", combinedLeads);
        return "lead_capture";
    }

    /**
     * EXECUTIVE LEAD CAPTURE - Fixed with model attributes for summary cards and
     * tasks
     */
    @GetMapping("/executive/lead-capture")
    public String executiveLeadCapture(HttpSession session, Model model) {
        if (isNotLoggedIn(session))
            return "redirect:/login";
        User currentUser = (User) session.getAttribute("user");

        List<Lead> myLeads = leadRepository.findByUser(currentUser);
        myLeads.sort(Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        LocalDate today = LocalDate.now();

        // Calculate counts for HTML summary cards
        long newCount = myLeads.stream().filter(l -> "New".equalsIgnoreCase(l.getStatus())).count();
        long interestedCount = myLeads.stream().filter(l -> "Interested".equalsIgnoreCase(l.getStatus())).count();
        long overdueCount = myLeads.stream()
                .filter(l -> "PENDING".equalsIgnoreCase(l.getFollowUpStatus())
                        && l.getFollowUpDate() != null
                        && l.getFollowUpDate().isBefore(today))
                .count();

        // Fetch specific tasks assigned to this executive
        List<Task> myTasks = taskRepository.findByExecutive(currentUser);

        model.addAttribute("leads", myLeads);
        model.addAttribute("tasks", myTasks);
        model.addAttribute("newCount", newCount);
        model.addAttribute("interestedCount", interestedCount);
        model.addAttribute("overdueCount", overdueCount);

        return "executive_lead_capture";
    }

    @GetMapping("/executive/profile")
    public String executiveProfile(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null)
            return "redirect:/login";
        User executive = userRepository.findById(sessionUser.getId()).orElse(sessionUser);
        model.addAttribute("executive", executive);
        return "executive_profile";
    }

    /**
     * @GetMapping("/logout")
     * public String logout(HttpSession session) {
     * User user = (User) session.getAttribute("user");
     * 
     * if (user != null && "EXECUTIVE".equals(user.getRole())) {
     * // --- AUTOMATED LOGOUT TRIGGER ---
     * attendanceController.autoRecordLogout(user);
     * }
     * 
     * session.invalidate(); // Clear the session after recording
     * return "redirect:/login?logout=true";
     * }
     **/

    /**
     * MANAGER MANAGE EXECUTIVES - Fixed to include Task Registry Table
     */
    @GetMapping("/manager/executives")
    public String managerManageExecs(Model model, HttpSession session) {
        if (isNotLoggedIn(session))
            return "redirect:/login";
        User currentManager = (User) session.getAttribute("user");

        List<User> myExecutives = userRepository.findByManager(currentManager);

        Map<Long, Long> leadCounts = new HashMap<>();
        for (User exec : myExecutives) {
            long count = leadRepository.findByUser(exec).size();
            leadCounts.put(exec.getId(), count);
        }

        // IMPORTANT: Fetch all tasks created by this manager to display in the separate
        // table
        List<Task> delegatedTasks = taskRepository.findByManager(currentManager);

        model.addAttribute("executives", myExecutives);
        model.addAttribute("leadCounts", leadCounts);
        model.addAttribute("tasks", delegatedTasks); // Sent to "Delegated Tasks Registry" table

        return "manager_manage_executives";
    }

    @GetMapping("/manager/profile")
    public String managerProfile(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null)
            return "redirect:/login";
        User manager = userRepository.findById(sessionUser.getId()).orElse(sessionUser);
        model.addAttribute("manager", manager);
        return "manager_profile";
    }

    // ****************************************
    /**
     * @GetMapping("/executive/tasks")
     * public String executiveTasks(HttpSession session, Model model) {
     * if (isNotLoggedIn(session)) return "redirect:/login";
     * User currentUser = (User) session.getAttribute("user");
     * 
     * // Fetch tasks specifically for this executive
     * List<Task> myTasks = taskRepository.findByExecutive(currentUser);
     * 
     * model.addAttribute("executive", currentUser);
     * model.addAttribute("tasks", myTasks);
     * return "executive_tasks";
     * }
     **/
    // ******************************
    @GetMapping("/leads/status/{id}/{newStatus}")
    public String updateLeadFollowUpStatusQuick(@PathVariable Long id, @PathVariable String newStatus) {
        // 1. Fetch the lead from the database
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found with id: " + id));

        // 2. Update the follow-up status (PENDING -> COMPLETED)
        lead.setFollowUpStatus(newStatus);

        // 3. Optional: Automatically update the main status to 'Contacted'
        if ("COMPLETED".equalsIgnoreCase(newStatus)) {
            lead.setStatus("Contacted");
        }

        // 4. Save the changes
        leadRepository.save(lead);

        // 5. Redirect back to the lead capture page with a success message
        return "redirect:/executive/lead-capture?success=status_updated";
    }

    // ****************************
    @GetMapping("/manager/performance")
    public String managerPerformance(HttpSession session, Model model) {
        if (session.getAttribute("user") == null)
            return "redirect:/login";

        User currentManager = (User) session.getAttribute("user");
        // Ensure we have the latest manager object from DB
        User manager = userRepository.findById(currentManager.getId()).orElse(currentManager);

        List<User> myExecutives = userRepository.findByManager(manager);

        // Data containers for the frontend
        Map<Long, Long> leadCounts = new HashMap<>();
        Map<Long, Long> allTaskCounts = new HashMap<>();
        Map<Long, Long> completedTaskCounts = new HashMap<>();

        for (User exec : myExecutives) {
            // 1. Calculate Leads (Sales performance)
            leadCounts.put(exec.getId(), (long) leadRepository.findByUser(exec).size());

            // 2. Calculate Tasks (Administrative performance)
            allTaskCounts.put(exec.getId(), taskRepository.countByExecutive(exec));
            completedTaskCounts.put(exec.getId(), taskRepository.countByExecutiveAndStatus(exec, "COMPLETED"));
        }

        model.addAttribute("executives", myExecutives);
        model.addAttribute("leadCounts", leadCounts);
        model.addAttribute("allTaskCounts", allTaskCounts);
        model.addAttribute("completedTaskCounts", completedTaskCounts);

        return "manager_performance";
    }

    // *************************************************
    @GetMapping("/executive/dashboard")
    public String executiveDashboard(HttpSession session, Model model) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        User sessionUser = (User) session.getAttribute("user");
        User currentUser = userRepository.findById(sessionUser.getId()).orElse(sessionUser);

        List<Lead> myLeads = leadRepository.findByUser(currentUser);
        myLeads.sort(Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        LocalDate today = LocalDate.now();

        // 1. Calculate Overdue LEAD Follow-ups
        long overdueCount = myLeads.stream()
                .filter(l -> l.getFollowUpDate() != null
                        && l.getFollowUpDate().isBefore(today)
                        && "PENDING".equalsIgnoreCase(l.getFollowUpStatus()))
                .count();

        // 2. NEW: Calculate actual PENDING TASKS from Task Repository
        long pendingTaskCount = taskRepository.countByExecutiveAndStatus(currentUser, "PENDING");

        // --- NEW: ADD ATTENDANCE TIMER LOGIC ---
        // We only want the LATEST login that hasn't been checked out yet
        attendanceRepository.findByUserAndDate(currentUser, today).stream()
                .filter(log -> log.getCheckOut() == null) // Filter for active sessions
                .max(Comparator.comparing(AttendanceLog::getCheckIn)) // Get the absolute newest one
                .ifPresentOrElse(
                        log -> model.addAttribute("checkInTime", log.getCheckIn().toString()),
                        () -> model.addAttribute("checkInTime", null));
        // ---------------------------------------

        model.addAttribute("executive", currentUser);
        model.addAttribute("leads", myLeads);
        model.addAttribute("activeCount", myLeads.size());
        model.addAttribute("overdueCount", overdueCount);

        // 3. Update the model attribute used by the HTML card
        model.addAttribute("pendingTaskCount", pendingTaskCount);

        // Source Chart Logic
        Map<String, Long> sourceCounts = myLeads.stream()
                .collect(Collectors.groupingBy(l -> l.getSource() == null ? "Unknown" : l.getSource(),
                        Collectors.counting()));
        model.addAttribute("sourceLabels", sourceCounts.keySet());
        model.addAttribute("sourceData", sourceCounts.values());

        return "executive_dashboard";
    }

    // ************************
    @GetMapping("/manager/closed-sales")
    public String teamClosedSales(HttpSession session, Model model) {
        if (session.getAttribute("user") == null)
            return "redirect:/login";

        User currentManager = (User) session.getAttribute("user");
        List<User> myTeam = userRepository.findByManager(currentManager);

        List<Lead> teamLeads = new ArrayList<>();
        if (!myTeam.isEmpty()) {
            teamLeads.addAll(leadRepository.findByUserIn(myTeam));
        }
        // Filter leads with status 'Close'
        List<Lead> closedLeads = teamLeads.stream()
                .filter(l -> "Close".equalsIgnoreCase(l.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("leads", closedLeads);
        return "manager_closed_sales";
    }

    // *************************
    @GetMapping("/executive/tasks")
    public String executiveTasks(HttpSession session, Model model) {
        if (isNotLoggedIn(session))
            return "redirect:/login";
        User currentUser = (User) session.getAttribute("user");

        // Fetch pending tasks specifically for this executive
        List<Task> myTasks = taskRepository.findByExecutiveAndStatus(currentUser, "PENDING");

        LocalDate today = LocalDate.now();

        // 1. Calculate Overdue Task Count (Date is before today)
        long overdueCount = myTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .count();

        // 2. Calculate Due Today Count (Date is exactly today)
        long dueTodayCount = myTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isEqual(today))
                .count();

        model.addAttribute("executive", currentUser);
        model.addAttribute("tasks", myTasks);

        // These attributes trigger the top reminders in your HTML
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("dueTodayCount", dueTodayCount);

        return "executive_tasks";
    }

    @GetMapping("/manager/my-leads")
    public String managerMyLeads(HttpSession session, Model model) {
        User manager = (User) session.getAttribute("user");
        if (manager == null || !"MANAGER".equals(manager.getRole()))
            return "redirect:/login";

        // 1. Fetch all leads owned by this manager
        List<Lead> allMyLeads = leadRepository.findByUser(manager);

        // 2. Separate Active Leads (Anything NOT 'Close')
        List<Lead> activeLeads = allMyLeads.stream()
                .filter(l -> !"Close".equalsIgnoreCase(l.getStatus()))
                .sorted(Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // 3. Separate Closed Sales
        List<Lead> closedLeads = allMyLeads.stream()
                .filter(l -> "Close".equalsIgnoreCase(l.getStatus()))
                .sorted(Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // 4. Add both lists to the model
        model.addAttribute("leads", activeLeads);
        model.addAttribute("closedLeads", closedLeads);

        // 5. Chart Data: Based on Active Leads for better planning
        Map<String, Long> mySourceCounts = activeLeads.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getSource() == null || l.getSource().isEmpty() ? "Unknown" : l.getSource(),
                        Collectors.counting()));

        model.addAttribute("sourceLabels", new ArrayList<>(mySourceCounts.keySet()));
        model.addAttribute("sourceData", new ArrayList<>(mySourceCounts.values()));

        // 6. Sidebar & Timer Logic (Preserved exactly as requested)
        model.addAttribute("pendingLeaves",
                leaveRepository.findByUserInOrderByCreatedAtDesc(userRepository.findByManager(manager))
                        .stream().filter(l -> "PENDING".equals(l.getStatus())).toList());

        attendanceRepository.findByUserAndDate(manager, LocalDate.now()).stream()
                .filter(log -> log.getCheckOut() == null)
                .max(Comparator.comparing(com.example.crm.entity.AttendanceLog::getCheckIn))
                .ifPresent(log -> model.addAttribute("checkInTime", log.getCheckIn().toString()));

        return "manager_my_leads";
    }

    @GetMapping("/manager/tasks")
    public String managerTaskPage(Model model, HttpSession session) {
        if (isNotLoggedIn(session))
            return "redirect:/login";

        User currentManager = (User) session.getAttribute("user");

        LocalDate today = LocalDate.now();

        // Logic: Admin tasks for this manager (where executive is null)
        List<Task> adminTasks = taskRepository.findAll().stream()
                .filter(t -> t.getManager() != null
                        && t.getManager().getId().equals(currentManager.getId())
                        && t.getExecutive() == null
                        && !"COMPLETED".equals(t.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("tasks", adminTasks);

        // Passing data for timer and notification dots
        model.addAttribute("pendingLeaves",
                leaveRepository.findByUserInOrderByCreatedAtDesc(userRepository.findByManager(currentManager))
                        .stream().filter(l -> "PENDING".equals(l.getStatus())).toList());

        // Calculate Counts for Reminders
        long overdueCount = adminTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .count();
        long dueTodayCount = adminTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isEqual(today))
                .count();

        model.addAttribute("tasks", adminTasks);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("dueTodayCount", dueTodayCount);

        // 3. Filter Completed (This goes to the 'Task History' table)
        // FIX: Changed allAdminTasks to adminTasks
        List<Task> completedTasks = adminTasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus()))
                .collect(Collectors.toList());

        // Timer Logic
        attendanceRepository.findByUserAndDate(currentManager, LocalDate.now()).stream()
                .filter(log -> log.getCheckOut() == null)
                .max(Comparator.comparing(com.example.crm.entity.AttendanceLog::getCheckIn))
                .ifPresent(log -> model.addAttribute("checkInTime", log.getCheckIn().toString()));

        return "manager_tasks";
    }

    @GetMapping("/manager/assign-hr-task")
    public String showAssignHrTaskPage(Model model, HttpSession session) {
        User manager = (User) session.getAttribute("user");
        if (manager == null || !"MANAGER".equals(manager.getRole()))
            return "redirect:/login";

        // 1. Fetch HRs reporting to this manager
        List<User> hrList = userRepository.findByManager(manager).stream()
                .filter(u -> "HR".equals(u.getRole()))
                .toList();

        // 2. Fetch Tasks assigned BY this manager TO HRs
        // (Assuming you fixed the 'setUser' issue in your entity)
        List<Task> assignedTasks = taskRepository.findByManager(manager).stream()
                .filter(t -> t.getUser() != null && "HR".equals(t.getUser().getRole()))
                .sorted(Comparator.comparing(Task::getId).reversed()) // Newest first
                .toList();

        model.addAttribute("hrList", hrList);
        model.addAttribute("assignedTasks", assignedTasks);

        // 2. Timer logic (matches your dashboard)
        attendanceRepository.findByUserAndDate(manager, LocalDate.now()).stream()
                .filter(log -> log.getCheckOut() == null)
                .max(Comparator.comparing(AttendanceLog::getCheckIn))
                .ifPresent(log -> model.addAttribute("checkInTime", log.getCheckIn().toString()));

        return "manager_assign_hr";
    }

    // 1. Save New Task
    @PostMapping("/manager/save-hr-task")
    public String saveHrTask(@RequestParam Long hrId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String dueDate,
            HttpSession session) {
        User manager = (User) session.getAttribute("user");
        if (manager == null)
            return "redirect:/login";

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(LocalDate.parse(dueDate));
        task.setStatus("PENDING");
        task.setManager(manager);
        task.setUser(userRepository.findById(hrId).orElseThrow());

        taskRepository.save(task);
        return "redirect:/manager/assign-hr-task?success=task_assigned";
    }

    // 2. Update Existing Task (FIXES THE 404 ERROR)
    @PostMapping("/manager/update-hr-task")
    public String updateHrTask(@RequestParam Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String dueDate) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(LocalDate.parse(dueDate));

        taskRepository.save(task);
        return "redirect:/manager/assign-hr-task?success=updated";
    }

    // 3. Delete Task
    @GetMapping("/manager/delete-hr-task/{id}")
    public String deleteHrTask(@PathVariable Long id) {
        taskRepository.deleteById(id);
        return "redirect:/manager/assign-hr-task?success=deleted";
    }

}