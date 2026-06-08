package com.example.crm.contoller;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.crm.entity.AttendanceLog;
import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import com.example.crm.repository.AttendanceRepository;
import com.example.crm.repository.TaskRepository;
import com.example.crm.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/hr")
public class HrController {

    @Autowired
    private UserRepository userRepository;
    public UserRepository getUserRepository() {
		return userRepository;
	}

	public void setUserRepository(UserRepository userRepository) {
		this.userRepository = userRepository;
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

	public TaskRepository getTaskRepository() {
		return taskRepository;
	}

	public void setTaskRepository(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}
	@Autowired
    private AttendanceController attendanceController;
    
    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    

    @GetMapping("/dashboard")
    public String hrDashboard(HttpSession session, Model model) {
        User loggedInHr = (User) session.getAttribute("user");
        if (loggedInHr == null || !"HR".equals(loggedInHr.getRole())) {
            return "redirect:/login";
        }

        User assignedManager = loggedInHr.getManager();
        
        if (assignedManager != null) {
            // 1. Get all members assigned to the manager
            List<User> allTeamMembers = userRepository.findByManager(assignedManager);
            
            // 2. FILTER: Remove the HR themselves and keep only EXECUTIVES
            List<User> executivesOnly = allTeamMembers.stream()
                .filter(user -> !"HR".equals(user.getRole())) // This removes the HR
                .filter(user -> "EXECUTIVE".equals(user.getRole())) // This ensures only executives remain
                .toList();

            model.addAttribute("executives", executivesOnly);
            model.addAttribute("managerName", assignedManager.getName());
            
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
        }
        
        return "hr_dashboard";
    }
    
    @GetMapping("/attendance/all")
    public String viewFullAttendanceHistory(HttpSession session, Model model) {
        User loggedInHr = (User) session.getAttribute("user");
        if (loggedInHr == null || !"HR".equals(loggedInHr.getRole())) {
            return "redirect:/login";
        }

        User assignedManager = loggedInHr.getManager();
        if (assignedManager != null) {
        	
        	// 1. FETCH ALL EXECUTIVES FOR THE SELECT BOX
            List<User> myExecutives = userRepository.findByManager(assignedManager).stream()
                .filter(u -> "EXECUTIVE".equals(u.getRole()))
                .toList();
            
            // 1. Get IDs of all Executives in this HR's team
            List<Long> teamUserIds = userRepository.findByManager(assignedManager).stream()
                .filter(u -> "EXECUTIVE".equals(u.getRole()))
                .map(User::getId)
                .toList();

            // 2. Fetch ALL historical logs for these team members
            List<AttendanceLog> allLogs = attendanceRepository.findAll().stream()
                .filter(log -> log.getUser() != null && teamUserIds.contains(log.getUser().getId()))
                .sorted((a, b) -> b.getId().compareTo(a.getId())) // Newest records at top
                .toList();
            model.addAttribute("executives", myExecutives);
            model.addAttribute("logs", allLogs);
            model.addAttribute("managerName", assignedManager.getName());
        }
        return "hr_attendance_history";
    }
    
    @GetMapping("/add-executive")
    public String addExecutivePage(Model model, HttpSession session) {
        User hr = (User) session.getAttribute("user");
        if (hr == null || !"HR".equals(hr.getRole())) return "redirect:/login";

        User currentHr = userRepository.findById(hr.getId()).orElse(hr);
        
        // Pass the current HR object to use for comparison in HTML
        model.addAttribute("currentHr", currentHr); 

        User assignedManager = currentHr.getManager();
        List<User> myTeamExecutives = List.of();
        if (assignedManager != null) {
            myTeamExecutives = userRepository.findByManager(assignedManager).stream()
                    .filter(u -> "EXECUTIVE".equals(u.getRole()))
                    .sorted((u1, u2) -> u2.getId().compareTo(u1.getId()))
                    .toList();
            model.addAttribute("managerName", assignedManager.getName());
        }
        
        model.addAttribute("executives", myTeamExecutives);

        // Timer Logic
        attendanceRepository.findByUserAndDate(currentHr, LocalDate.now()).stream()
            .filter(log -> log.getCheckOut() == null)
            .max(Comparator.comparing(AttendanceLog::getCheckIn))
            .ifPresent(log -> model.addAttribute("checkInTime", log.getCheckIn().toString()));

        return "hr_add_executive";
    }

    @PostMapping("/executive/save")
    public String saveExecutive(@RequestParam String name, @RequestParam String email, 
                                @RequestParam String password, HttpSession session) {
        User hr = (User) session.getAttribute("user");
        User currentHr = userRepository.findById(hr.getId()).orElseThrow();

        if (userRepository.existsByEmail(email)) {
            return "redirect:/hr/add-executive?error=email_exists";
        }

        User exec = new User();
        exec.setName(name);
        exec.setEmail(email);
        exec.setPassword(password);
        exec.setRole("EXECUTIVE");
        
        // Auto-assign the Executive to the same Manager as the HR
        if (currentHr.getManager() != null) {
            exec.setManager(currentHr.getManager());
        }
        
        // IMPORTANT: Storing HR's ID in profile_picture temporarily 
        // to identify who added whom without changing DB schema immediately.
        exec.setProfilePicture(currentHr.getId().toString());

        userRepository.save(exec);
        return "redirect:/hr/add-executive?success=executive_added";
    }
    
    @GetMapping("/tasks")
    public String viewManagerTasks(HttpSession session, Model model) {
        User hr = (User) session.getAttribute("user");
        if (hr == null || !"HR".equals(hr.getRole())) return "redirect:/login";

        // 1. Refresh HR data to get the manager link
        User currentHr = userRepository.findById(hr.getId()).orElse(hr);
        
        // 2. Fetch tasks assigned specifically to this HR
        List<Task> myTasks = taskRepository.findByUser(currentHr).stream()
                .sorted(Comparator.comparing(Task::getDueDate))
                .toList();

        model.addAttribute("myTasks", myTasks);
        model.addAttribute("managerName", currentHr.getManager() != null ? currentHr.getManager().getName() : "System");
        
     // --- UPDATED REMINDER LOGIC ---
        LocalDate today = LocalDate.now();
        
        long overdueCount = myTasks.stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today))
                .count();

        long dueTodayCount = myTasks.stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .filter(t -> t.getDueDate() != null && t.getDueDate().isEqual(today))
                .count();

        model.addAttribute("myTasks", myTasks.stream().sorted(Comparator.comparing(Task::getDueDate)).toList());
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("dueTodayCount", dueTodayCount);
        // ------------------------------

        // 3. Re-use your dashboard's timer logic
        attendanceRepository.findByUserAndDate(currentHr, LocalDate.now()).stream()
            .filter(log -> log.getCheckOut() == null)
            .max(Comparator.comparing(AttendanceLog::getCheckIn))
            .ifPresent(log -> model.addAttribute("checkInTime", log.getCheckIn().toString()));

        return "hr_tasks";
    }
    @GetMapping("/tasks/complete/{id}")
    public String completeTask(@PathVariable Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Directive not found"));
        
        // Set status to COMPLETED
        task.setStatus("COMPLETED");
        taskRepository.save(task);
        
        // Redirect back to the tasks list
        return "redirect:/hr/tasks?success=completed";
    }
 }
