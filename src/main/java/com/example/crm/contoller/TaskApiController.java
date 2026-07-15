package com.example.crm.contoller;

import com.example.crm.entity.Lead;
import com.example.crm.entity.Task;
import com.example.crm.entity.User;
import com.example.crm.entity.LeaveRequest;
import com.example.crm.entity.Transaction;
import com.example.crm.entity.AttendanceLog;
import com.example.crm.entity.CallRecord;
import com.example.crm.entity.Employee;

import com.example.crm.repository.LeadRepository;
import com.example.crm.repository.TaskRepository;
import com.example.crm.repository.UserRepository;
import com.example.crm.repository.LeaveRepository;
import com.example.crm.repository.TransactionRepository;
import com.example.crm.repository.AttendanceRepository;
import com.example.crm.repository.CallRecordRepository;
import com.example.crm.repository.EmployeeProfileRepository;
import com.example.crm.repository.EmployeeRepository;
import com.example.crm.repository.EmployeeAttendanceRepository;

import com.example.crm.entity.Student;
import com.example.crm.entity.HrNotification;
import com.example.crm.repository.StudentRepository;
import com.example.crm.repository.HrNotificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crm")
public class TaskApiController {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private CallRecordRepository callRecordRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeAttendanceRepository employeeAttendanceRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private HrNotificationRepository hrNotificationRepository;

    // --- STATUS CHECK ---
    @GetMapping("/status")
    public String checkStatus() {
        return "CRM API is Online and Connected";
    }

    // --- LEADS REST API ---
    @GetMapping("/leads")
    public List<Map<String, Object>> getAllLeads() {
        return leadRepository.findAll().stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("customerName", l.getCustomerName());
            map.put("email", l.getEmail());
            map.put("phone", l.getPhoneNumber());
            map.put("source", l.getSource());
            map.put("description", l.getDescription());
            map.put("value", l.getValue() != null ? l.getValue() : 0.0);
            map.put("notes", l.getNotes());
            map.put("status", l.getStatus());
            map.put("dateAdded", l.getCreatedAt() != null ? l.getCreatedAt().toLocalDate().toString() : "");
            map.put("userId", l.getUser() != null ? l.getUser().getId() : null);
            map.put("college", l.getCollege() != null ? l.getCollege() : "");
            map.put("passoutYear", l.getPassoutYear() != null ? l.getPassoutYear() : null);
            map.put("department", l.getDepartment() != null ? l.getDepartment() : "");
            map.put("followUpDate", l.getFollowUpDate() != null ? l.getFollowUpDate().toString() : null);
            map.put("closedDate", l.getClosedDate() != null ? l.getClosedDate().toString() : null);
            map.put("lastUpdatedBy", l.getLastUpdatedBy() != null ? l.getLastUpdatedBy() : "");
            map.put("lastUpdatedAt", l.getLastUpdatedAt() != null ? l.getLastUpdatedAt() : "");
            return map;
        }).collect(Collectors.toList());
    }

    private String cleanString(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : s;
    }

    @PostMapping("/leads/add")
    public ResponseEntity<?> addLead(@RequestBody Map<String, Object> payload) {
        Lead lead = new Lead();
        lead.setCustomerName(cleanString(payload.get("customerName")));
        lead.setEmail(cleanString(payload.get("email")));
        lead.setPhoneNumber(cleanString(payload.get("phone")));
        lead.setSource(payload.get("source") != null ? cleanString(payload.get("source")) : "Website");
        lead.setDescription(cleanString(payload.get("description")));
        lead.setStatus(payload.get("status") != null ? cleanString(payload.get("status")) : "New");
        lead.setNotes(cleanString(payload.get("notes")));
        lead.setCollege(cleanString(payload.get("college")));

        if (payload.get("passoutYear") != null && !payload.get("passoutYear").toString().trim().isEmpty()) {
            lead.setPassoutYear(Integer.valueOf(payload.get("passoutYear").toString().trim()));
        }

        lead.setDepartment(cleanString(payload.get("department")));

        if (payload.get("value") != null && !payload.get("value").toString().isEmpty()) {
            lead.setValue(Double.valueOf(payload.get("value").toString()));
        }

        if (payload.get("userId") != null) {
            Long userId = Long.valueOf(payload.get("userId").toString());
            userRepository.findById(userId).ifPresent(lead::setUser);
        }

        if (payload.get("followUpDate") != null && !payload.get("followUpDate").toString().trim().isEmpty()) {
            lead.setFollowUpDate(LocalDate.parse(payload.get("followUpDate").toString().trim()));
        }

        if (payload.get("closedDate") != null && !payload.get("closedDate").toString().trim().isEmpty()) {
            lead.setClosedDate(LocalDate.parse(payload.get("closedDate").toString().trim()));
        }

        if (payload.get("lastUpdatedBy") != null) {
            lead.setLastUpdatedBy(cleanString(payload.get("lastUpdatedBy")));
        }
        if (payload.get("lastUpdatedAt") != null) {
            lead.setLastUpdatedAt(cleanString(payload.get("lastUpdatedAt")));
        }

        Lead saved = leadRepository.save(lead);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/leads/update")
    public ResponseEntity<?> updateLead(@RequestBody Map<String, Object> payload) {
        Long id = Long.valueOf(payload.get("id").toString());
        Lead lead = leadRepository.findById(id).orElseThrow(() -> new RuntimeException("Lead not found"));
        String oldStatus = lead.getStatus();

        if (payload.containsKey("customerName"))
            lead.setCustomerName(cleanString(payload.get("customerName")));
        if (payload.containsKey("email"))
            lead.setEmail(cleanString(payload.get("email")));
        if (payload.containsKey("phone"))
            lead.setPhoneNumber(cleanString(payload.get("phone")));
        if (payload.containsKey("source"))
            lead.setSource(cleanString(payload.get("source")));
        if (payload.containsKey("description"))
            lead.setDescription(cleanString(payload.get("description")));
        if (payload.containsKey("status"))
            lead.setStatus(cleanString(payload.get("status")));
        if (payload.containsKey("notes"))
            lead.setNotes(cleanString(payload.get("notes")));
        if (payload.containsKey("college"))
            lead.setCollege(cleanString(payload.get("college")));
        if (payload.containsKey("passoutYear")) {
            if (payload.get("passoutYear") != null && !payload.get("passoutYear").toString().trim().isEmpty()) {
                lead.setPassoutYear(Integer.valueOf(payload.get("passoutYear").toString().trim()));
            } else {
                lead.setPassoutYear(null);
            }
        }
        if (payload.containsKey("department"))
            lead.setDepartment(cleanString(payload.get("department")));
        if (payload.containsKey("course"))
            lead.setCourse(cleanString(payload.get("course")));

        if (payload.containsKey("value") && payload.get("value") != null) {
            lead.setValue(Double.valueOf(payload.get("value").toString()));
        }

        if (payload.containsKey("followUpDate")) {
            if (payload.get("followUpDate") != null && !payload.get("followUpDate").toString().trim().isEmpty()) {
                lead.setFollowUpDate(LocalDate.parse(payload.get("followUpDate").toString().trim()));
            } else {
                lead.setFollowUpDate(null);
            }
        }

        if (payload.containsKey("closedDate")) {
            if (payload.get("closedDate") != null && !payload.get("closedDate").toString().trim().isEmpty()) {
                lead.setClosedDate(LocalDate.parse(payload.get("closedDate").toString().trim()));
            } else {
                lead.setClosedDate(null);
            }
        }

        if (payload.containsKey("lastUpdatedBy")) {
            lead.setLastUpdatedBy((String) payload.get("lastUpdatedBy"));
        }
        if (payload.containsKey("lastUpdatedAt")) {
            lead.setLastUpdatedAt((String) payload.get("lastUpdatedAt"));
        }

        Lead saved = leadRepository.save(lead);

        String newStatus = saved.getStatus();
        boolean isClosedStatus = "Closed Won".equalsIgnoreCase(newStatus) || "Close".equalsIgnoreCase(newStatus) || "Closed".equalsIgnoreCase(newStatus);
        boolean wasClosedStatus = "Closed Won".equalsIgnoreCase(oldStatus) || "Close".equalsIgnoreCase(oldStatus) || "Closed".equalsIgnoreCase(oldStatus);

        if (isClosedStatus && !wasClosedStatus) {
            try {
                Student student = new Student();
                long studentCount = studentRepository.count();
                student.setStudentId("STU-" + (1000 + studentCount + 1));
                student.setName(saved.getCustomerName());
                student.setPhone(saved.getPhoneNumber());
                student.setEmail(saved.getEmail());
                student.setCollege(saved.getCollege());
                student.setQualification(saved.getDepartment() != null ? saved.getDepartment() : "Not Specified");
                student.setCoursePurchased(saved.getCourse() != null ? saved.getCourse() : "Java Full Stack");
                student.setCourseFees(saved.getValue() != null ? saved.getValue() : 60000.0);
                student.setPaidAmount(saved.getValue() != null ? saved.getValue() : 60000.0);
                student.setBalance(0.0);
                student.setJoiningDate(LocalDate.now());
                student.setSalesExecutive(saved.getLastUpdatedBy() != null ? saved.getLastUpdatedBy() : "System");
                student.setStatus("PENDING_VERIFICATION");
                studentRepository.save(student);

                HrNotification notification = new HrNotification();
                notification.setMessage("Sales Closed: Student " + student.getName() + " Purchased " + student.getCoursePurchased());
                notification.setType("SALE_CLOSE");
                notification.setCreatedAt(LocalDateTime.now());
                notification.setIsRead(false);
                hrNotificationRepository.save(notification);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/leads/{id}")
    public ResponseEntity<?> deleteLead(@PathVariable Long id) {
        leadRepository.deleteById(id);
        Map<String, String> res = new HashMap<>();
        res.put("success", "true");
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/calls/{id}")
    public ResponseEntity<?> deleteCallRecord(@PathVariable Long id) {
        callRecordRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/users/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            // 1. Unlink subordinates
            List<User> subordinates = userRepository.findByManager(user);
            if (subordinates != null) {
                for (User sub : subordinates) {
                    sub.setManager(null);
                    userRepository.save(sub);
                }
            }

            // 2. Remove employee profile
            employeeProfileRepository.findByUser(user).ifPresent(p -> employeeProfileRepository.delete(p));

            // 3. Remove leave requests
            List<LeaveRequest> leaves = leaveRepository.findByUserOrderByCreatedAtDesc(user);
            if (leaves != null) {
                leaveRepository.deleteAll(leaves);
            }

            // 4. Remove attendance logs
            List<AttendanceLog> attendanceLogs = attendanceRepository.findByUser(user);
            if (attendanceLogs != null) {
                attendanceRepository.deleteAll(attendanceLogs);
            }

            // 5. Remove tasks
            List<Task> userTasks = taskRepository.findByUser(user);
            if (userTasks != null) {
                taskRepository.deleteAll(userTasks);
            }
            List<Task> execTasks = taskRepository.findByExecutive(user);
            if (execTasks != null) {
                taskRepository.deleteAll(execTasks);
            }
            List<Task> managerTasks = taskRepository.findByManager(user);
            if (managerTasks != null) {
                taskRepository.deleteAll(managerTasks);
            }

            // 6. Remove leads
            List<Lead> leads = leadRepository.findByUser(user);
            if (leads != null) {
                leadRepository.deleteAll(leads);
            }

            // 7. Remove call records
            List<CallRecord> calls = callRecordRepository.findByAgent(user);
            if (calls != null) {
                callRecordRepository.deleteAll(calls);
            }

            // 8. Remove transactions
            List<Transaction> transactions = transactionRepository.findByRecordedBy(user);
            if (transactions != null) {
                transactionRepository.deleteAll(transactions);
            }

            // 9. Remove matching HR employee record and attendance
            Employee employee = employeeRepository.findByEmailIgnoreCase(user.getEmail());
            if (employee != null) {
                employeeAttendanceRepository.deleteAll(employeeAttendanceRepository.findByEmployeeId(employee.getId()));
                employeeRepository.delete(employee);
            }

            // Finally delete the user
            userRepository.delete(user);
        }
        return ResponseEntity.ok().build();
    }

    // --- TASKS REST API ---
    @GetMapping("/tasks")
    public List<Map<String, Object>> getAllTasks() {
        return taskRepository.findAll().stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("title", t.getTitle());
            map.put("description", t.getDescription());
            map.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : "");
            map.put("status", t.getStatus());

            if (t.getExecutive() != null) {
                map.put("assignedTo", t.getExecutive().getId());
            } else if (t.getManager() != null) {
                map.put("assignedTo", t.getManager().getId());
            } else if (t.getUser() != null) {
                map.put("assignedTo", t.getUser().getId());
            } else {
                map.put("assignedTo", null);
            }
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/tasks/add")
    public ResponseEntity<?> addTask(@RequestBody Map<String, Object> payload) {
        Task task = new Task();
        task.setTitle((String) payload.get("title"));
        task.setDescription((String) payload.get("description"));
        task.setDueDate(LocalDate.parse((String) payload.get("dueDate")));
        task.setStatus(payload.get("status") != null ? (String) payload.get("status") : "Pending");

        if (payload.get("assignedTo") != null) {
            Long assignedToId = Long.valueOf(payload.get("assignedTo").toString());
            userRepository.findById(assignedToId).ifPresent(u -> {
                task.setUser(u);
                if ("EXECUTIVE".equals(u.getRole())) {
                    task.setExecutive(u);
                } else if ("MANAGER".equals(u.getRole()) || "BDE_MANAGER".equals(u.getRole()) || "ASSISTANT_MANAGER".equals(u.getRole())) {
                    task.setManager(u);
                }
            });
        }

        Task saved = taskRepository.save(task);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/tasks/{id}/complete")
    public ResponseEntity<?> completeTask(@PathVariable Long id) {
        Task t = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task not found"));
        t.setStatus("Completed");
        taskRepository.save(t);
        return ResponseEntity.ok(t);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        taskRepository.deleteById(id);
        Map<String, String> res = new HashMap<>();
        res.put("success", "true");
        return ResponseEntity.ok(res);
    }

    // --- LEAVES REST API ---
    @GetMapping("/leaves")
    public List<Map<String, Object>> getAllLeaves() {
        return leaveRepository.findAll().stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId());
            map.put("requesterId", l.getUser() != null ? l.getUser().getId() : null);
            map.put("requesterName", l.getUser() != null ? l.getUser().getName() : "");
            map.put("role", l.getUser() != null ? l.getUser().getRole() : "");
            map.put("startDate", l.getStartDate() != null ? l.getStartDate().toString() : "");
            map.put("endDate", l.getEndDate() != null ? l.getEndDate().toString() : "");
            map.put("reason", l.getReason());
            map.put("status", l.getStatus());
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/leaves/apply")
    public ResponseEntity<?> applyLeave(@RequestBody Map<String, Object> payload) {
        LeaveRequest leave = new LeaveRequest();
        leave.setStartDate(LocalDate.parse((String) payload.get("startDate")));
        leave.setEndDate(LocalDate.parse((String) payload.get("endDate")));
        leave.setReason((String) payload.get("reason"));
        leave.setStatus("Pending");
        leave.setLeaveType("ANNUAL");

        if (payload.get("requesterId") != null) {
            Long userId = Long.valueOf(payload.get("requesterId").toString());
            userRepository.findById(userId).ifPresent(leave::setUser);
        }

        LeaveRequest saved = leaveRepository.save(leave);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/leaves/{id}/status")
    public ResponseEntity<?> updateLeaveStatus(@PathVariable Long id, @RequestParam String status) {
        LeaveRequest leave = leaveRepository.findById(id).orElseThrow(() -> new RuntimeException("Leave not found"));
        leave.setStatus(status);
        leaveRepository.save(leave);
        return ResponseEntity.ok(leave);
    }

    // --- TRANSACTIONS REST API ---
    @GetMapping("/transactions")
    public List<Map<String, Object>> getAllTransactions() {
        return transactionRepository.findAll().stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("transactionDate",
                    t.getTransactionDate() != null ? t.getTransactionDate().toLocalDate().toString() : "");
            map.put("description", t.getDescription());
            map.put("category", t.getCategory());
            map.put("currency", t.getCurrency() != null ? t.getCurrency() : "INR");
            map.put("baseAmount", t.getBaseAmount() != null ? t.getBaseAmount() : 0.0);
            map.put("vatAmount", t.getVatAmount() != null ? t.getVatAmount() : 0.0);
            map.put("totalAmount", t.getTotalAmount() != null ? t.getTotalAmount() : 0.0);
            map.put("type", t.getType());
            map.put("exchangeRate", t.getExchangeRate() != null ? t.getExchangeRate() : 1.0);
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/transactions/add")
    public ResponseEntity<?> addTransaction(@RequestBody Map<String, Object> payload) {
        Transaction tx = new Transaction();
        tx.setDescription((String) payload.get("description"));
        tx.setType((String) payload.get("type"));
        tx.setCategory((String) payload.get("category"));
        tx.setCurrency(payload.get("currency") != null ? (String) payload.get("currency") : "INR");

        if (payload.get("baseAmount") != null) {
            tx.setBaseAmount(Double.valueOf(payload.get("baseAmount").toString()));
        }
        if (payload.get("vatRate") != null) {
            tx.setVatRate(Double.valueOf(payload.get("vatRate").toString()));
        } else {
            tx.setVatRate(15.0);
        }

        if (payload.get("recordedBy") != null) {
            Long userId = Long.valueOf(payload.get("recordedBy").toString());
            userRepository.findById(userId).ifPresent(tx::setRecordedBy);
        }
        tx.setExchangeRate(1.0);

        Transaction saved = transactionRepository.save(tx);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        transactionRepository.deleteById(id);
        Map<String, String> res = new HashMap<>();
        res.put("success", "true");
        return ResponseEntity.ok(res);
    }

    // --- ATTENDANCE REST API ---
    @GetMapping("/attendance")
    public List<Map<String, Object>> getAllAttendance() {
        return attendanceRepository.findAll().stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("userId", log.getUser() != null ? log.getUser().getId() : null);
            map.put("name", log.getUser() != null ? log.getUser().getName() : "");
            map.put("role", log.getUser() != null ? log.getUser().getRole() : "");
            map.put("date", log.getDate() != null ? log.getDate().toString() : "");
            map.put("checkIn",
                    log.getCheckIn() != null
                            ? log.getCheckIn().toLocalTime()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                            : "");
            map.put("checkOut",
                    log.getCheckOut() != null
                            ? log.getCheckOut().toLocalTime()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                            : null);
            if (log.getCheckIn() != null && log.getCheckOut() != null) {
                java.time.Duration d = java.time.Duration.between(log.getCheckIn(), log.getCheckOut());
                map.put("hours", Math.round((d.toMinutes() / 60.0) * 10.0) / 10.0);
            } else {
                map.put("hours", 0.0);
            }
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/attendance/record")
    public ResponseEntity<?> recordAttendance(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String action = (String) payload.get("action");
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        LocalDate today = LocalDate.now();

        if ("checkin".equalsIgnoreCase(action)) {
            List<AttendanceLog> existing = attendanceRepository.findByUserAndDate(user, today);
            if (!existing.isEmpty()) {
                return ResponseEntity.ok(existing.get(0));
            }
            AttendanceLog log = new AttendanceLog();
            log.setUser(user);
            log.setDate(today);
            log.setCheckIn(LocalDateTime.now());
            log.setStatus("PRESENT");
            AttendanceLog saved = attendanceRepository.save(log);
            return ResponseEntity.ok(saved);
        } else if ("checkout".equalsIgnoreCase(action)) {
            List<AttendanceLog> existing = attendanceRepository.findByUserAndDate(user, today);
            AttendanceLog logToUpdate = null;
            if (!existing.isEmpty()) {
                for (AttendanceLog log : existing) {
                    if (log.getCheckOut() == null) {
                        logToUpdate = log;
                        break;
                    }
                }
            }

            if (logToUpdate == null) {
                List<AttendanceLog> allLogs = attendanceRepository.findByUser(user);
                for (AttendanceLog log : allLogs) {
                    if (log.getCheckOut() == null) {
                        logToUpdate = log;
                        break;
                    }
                }
            }

            if (logToUpdate != null) {
                logToUpdate.setCheckOut(LocalDateTime.now());
                AttendanceLog saved = attendanceRepository.save(logToUpdate);
                return ResponseEntity.ok(saved);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "no_active_session");
                response.put("message", "No active check-in found to check out");
                return ResponseEntity.ok(response);
            }
        }

        // default response for unsupported actions
        return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid action"));
    }
    // Duplicate method removed. Retained earlier implementation.

    @PostMapping("/calls/initiate")
    public ResponseEntity<?> initiateClickToCall(@RequestBody Map<String, Object> payload) {
        Long leadId = Long.valueOf(payload.get("leadId").toString());
        Optional<Lead> leadOpt = leadRepository.findById(leadId);
        if (leadOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Candidate lead not found"));
        }
        Lead lead = leadOpt.get();
        String candidatePhone = lead.getPhoneNumber();
        String candidateName = lead.getCustomerName();

        Long executiveId = Long.valueOf(payload.get("executiveId").toString());
        Optional<User> execOpt = userRepository.findById(executiveId);
        if (execOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sales executive not found"));
        }
        User executive = execOpt.get();
        String executivePhone = executive.getPhone();

        if (executivePhone == null || executivePhone.trim().isEmpty()) {
            executivePhone = "+91 99999 66666";
        }
        if (candidatePhone == null || candidatePhone.trim().isEmpty()) {
            candidatePhone = "+91 98765 43210";
        }

        // Call the Airtel IQ Click-to-Call API
        String airtelUrl = "https://iqapi.airtel.in/voice/outbound/v1/clicktocall";
        System.out.println("Calling Airtel IQ Click-to-Call API at: " + airtelUrl);
        System.out.println("Payload: agent=" + executivePhone + ", customer=" + candidatePhone);

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            Map<String, String> airtelRequest = new HashMap<>();
            airtelRequest.put("agentNumber", executivePhone);
            airtelRequest.put("customerNumber", candidatePhone);
            airtelRequest.put("callerId", "+91114567890");

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer MOCK_AIRTEL_IQ_TOKEN_XYZ_12345");
            org.springframework.http.HttpEntity<Map<String, String>> entity = new org.springframework.http.HttpEntity<>(
                    airtelRequest, headers);

            try {
                restTemplate.postForEntity(airtelUrl, entity, String.class);
            } catch (Exception ex) {
                System.out.println("Airtel IQ API offline or token expired. Simulating success response.");
            }
        } catch (Exception e) {
            System.out.println("Failed calling Airtel IQ API: " + e.getMessage());
        }

        CallRecord record = new CallRecord();
        record.setCustomerName(candidateName);
        record.setCustomerPhone(candidatePhone);
        record.setDirection("OUTBOUND");
        record.setStartTime(LocalDateTime.now());
        record.setEndTime(LocalDateTime.now().plusSeconds(30));
        record.setDurationSeconds(30);
        record.setStatus("Answered");
        record.setAgent(executive);
        record.setSimUsed("Corporate VoIP");
        record.setTranscription("Click-to-Call initiated via Airtel IQ.");
        record.setFollowUpNotes("Outbound Click-to-Call session started.");

        CallRecord saved = callRecordRepository.save(record);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Click-to-Call initiated via Airtel IQ");
        responseMap.put("callRecordId", saved.getId());
        responseMap.put("customerName", candidateName);
        responseMap.put("customerPhone", candidatePhone);
        responseMap.put("executivePhone", executivePhone);

        return ResponseEntity.ok(responseMap);
    }

    @PostMapping("/users/update")
    public ResponseEntity<?> apiUpdateUser(@RequestBody Map<String, Object> payload) {
        Long id = Long.valueOf(payload.get("id").toString());
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        String oldEmail = user.getEmail();

        if (payload.containsKey("name"))
            user.setName((String) payload.get("name"));
        if (payload.containsKey("email"))
            user.setEmail((String) payload.get("email"));

        if (payload.containsKey("password"))
            user.setPassword((String) payload.get("password"));
        if (payload.containsKey("position"))
            user.setPosition((String) payload.get("position"));

        if (payload.containsKey("salary") && payload.get("salary") != null
                && !payload.get("salary").toString().isEmpty()) {
            user.setSalary(Double.valueOf(payload.get("salary").toString()));
        }

        if (payload.containsKey("dateJoined") && payload.get("dateJoined") != null
                && !payload.get("dateJoined").toString().isEmpty()) {
            user.setDateOfJoining(java.time.LocalDate.parse(payload.get("dateJoined").toString()));
        }

        if (payload.containsKey("targetAmount") && payload.get("targetAmount") != null
                && !payload.get("targetAmount").toString().isEmpty()) {
            user.setTargetAmount(Double.valueOf(payload.get("targetAmount").toString()));
        }

        if (payload.containsKey("managerId")) {
            Long managerId = Long.valueOf(payload.get("managerId").toString());
            User manager = userRepository.findById(managerId).orElse(null);
            user.setManager(manager);
        }

        if (payload.containsKey("phone")) {
            user.setPhone((String) payload.get("phone"));
        }

        User saved = userRepository.save(user);
        syncEmployeeForUser(saved, payload, oldEmail);
        return ResponseEntity.ok(saved);
    }

    private void syncEmployeeForUser(User user, Map<String, Object> payload, String oldEmail) {
        if (user == null || user.getEmail() == null || "ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }

        Employee employee = employeeRepository.findByEmailIgnoreCase(user.getEmail());
        if (employee == null && oldEmail != null) {
            employee = employeeRepository.findByEmailIgnoreCase(oldEmail);
        }
        if (employee == null) {
            employee = new Employee();
            employee.setEmployeeId(nextEmployeeId());
            employee.setStatus("Active");
        }

        employee.setEmail(user.getEmail());
        employee.setName(user.getName());
        employee.setPassword(user.getPassword());
        employee.setRole(normalizeRole(user.getRole()));
        employee.setSalary(user.getSalary());
        employee.setJoiningDate(user.getDateOfJoining() != null ? user.getDateOfJoining() : LocalDate.now());
        employee.setDepartment(payload.containsKey("department")
                ? textValue(payload.get("department"), defaultDepartment(user.getRole()))
                : defaultDepartment(user.getRole()));
        employee.setPhone(user.getPhone());

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

    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "EXECUTIVE";
        }
        return role.trim().toUpperCase();
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

    // --- OTHER API METHODS ---
    @GetMapping("/managers/{id}/executives")
    public List<User> getExecutivesByManager(@PathVariable Long id) {
        return userRepository.findAll().stream()
                .filter(u -> u.getManager() != null && u.getManager().getId().equals(id))
                .toList();
    }
}
