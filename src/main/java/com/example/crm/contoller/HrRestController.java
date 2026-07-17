package com.example.crm.contoller;

import com.example.crm.entity.*;
import com.example.crm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/hr")
public class HrRestController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeAttendanceRepository employeeAttendanceRepository;

    @Autowired
    private EmployeeLoginHistoryRepository employeeLoginHistoryRepository;

    @Autowired
    private TrainerRepository trainerRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentDocumentRepository studentDocumentRepository;

    @Autowired
    private StudentPaymentRepository studentPaymentRepository;

    @Autowired
    private StudentNoteRepository studentNoteRepository;

    @Autowired
    private PlacementCompanyRepository placementCompanyRepository;

    @Autowired
    private PlacementRepository placementRepository;

    @Autowired
    private StudentAttendanceRepository studentAttendanceRepository;

    @Autowired
    private HrNotificationRepository hrNotificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.example.crm.service.EmailService emailService;

    // ==========================================
    // EMPLOYEE ENDPOINTS
    // ==========================================
    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeRepository.findAll());
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@RequestBody Employee employee) {
        String cleanEmail = employee.getEmail() == null ? "" : employee.getEmail().trim();
        if (!cleanEmail.isEmpty() && employeeRepository.existsByEmailIgnoreCase(cleanEmail)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        employee.setEmail(cleanEmail);
        employee.setEmployeeId(nextEmployeeId());
        employee.setRole(normalizeRole(employee.getRole()));
        if (employee.getJoiningDate() == null) {
            employee.setJoiningDate(LocalDate.now());
        }
        if (employee.getStatus() == null) {
            employee.setStatus("Active");
        }
        if (employee.getDepartment() == null || employee.getDepartment().trim().isEmpty()) {
            employee.setDepartment(defaultDepartment(employee.getRole()));
        }

        Employee saved = employeeRepository.save(employee);
        syncUserForEmployee(saved);
        if (saved.getEmail() != null && !saved.getEmail().trim().isEmpty()) {
            String rawPassword = saved.getPassword() != null && !saved.getPassword().isBlank() ? saved.getPassword() : "Welcome@123";
            emailService.sendUserCredentials(
                saved.getEmail().trim(),
                saved.getName(),
                saved.getEmail().trim(),
                rawPassword
            );
        }
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody Employee employeeDetails) {
        return employeeRepository.findById(id).map(employee -> {
            String oldEmail = employee.getEmail();
            String cleanEmail = employeeDetails.getEmail() == null ? employee.getEmail()
                    : employeeDetails.getEmail().trim();
            Employee emailOwner = employeeRepository.findByEmailIgnoreCase(cleanEmail);
            if (emailOwner != null && !emailOwner.getId().equals(employee.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }

            employee.setName(employeeDetails.getName());
            employee.setPhoto(employeeDetails.getPhoto());
            employee.setGender(employeeDetails.getGender());
            employee.setDob(employeeDetails.getDob());
            employee.setEmail(cleanEmail);
            employee.setPhone(employeeDetails.getPhone());
            employee.setAddress(employeeDetails.getAddress());
            employee.setDepartment(employeeDetails.getDepartment() != null ? employeeDetails.getDepartment()
                    : defaultDepartment(employeeDetails.getRole()));
            employee.setRole(normalizeRole(employeeDetails.getRole()));
            employee.setSalary(employeeDetails.getSalary());
            if (employeeDetails.getJoiningDate() != null) {
                employee.setJoiningDate(employeeDetails.getJoiningDate());
            }
            employee.setQualification(employeeDetails.getQualification());
            employee.setExperience(employeeDetails.getExperience());
            employee.setSkills(employeeDetails.getSkills());
            employee.setStatus(employeeDetails.getStatus());
            employee.setPassword(employeeDetails.getPassword());
            employee.setEmergencyContact(employeeDetails.getEmergencyContact());
            employee.setBankDetails(employeeDetails.getBankDetails());
            employee.setAadharNumber(employeeDetails.getAadharNumber());
            employee.setPanNumber(employeeDetails.getPanNumber());
            Employee saved = employeeRepository.save(employee);
            syncUserForEmployee(saved, oldEmail);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        return employeeRepository.findById(id).map(employee -> {
            // Remove associated User if exists
            User user = userRepository.findByEmailIgnoreCase(employee.getEmail());
            if (user != null) {
                userRepository.delete(user);
            }
            employeeAttendanceRepository.deleteAll(employeeAttendanceRepository.findByEmployeeId(employee.getId()));
            employeeRepository.delete(employee);
            return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    private void syncUserForEmployee(Employee employee) {
        syncUserForEmployee(employee, null);
    }

    private void syncUserForEmployee(Employee employee, String oldEmail) {
        if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(employee.getEmail().trim());
        if (user == null && oldEmail != null) {
            user = userRepository.findByEmailIgnoreCase(oldEmail);
        }
        if (user == null) {
            user = new User();
        }

        user.setEmail(employee.getEmail().trim());
        user.setName(employee.getName());
        user.setPassword(employee.getPassword() != null && !employee.getPassword().isBlank() ? employee.getPassword()
                : "Welcome@123");
        user.setRole(normalizeRole(employee.getRole()));
        user.setDateOfJoining(employee.getJoiningDate());
        user.setSalary(employee.getSalary());
        userRepository.save(user);

        // Auto-sync Trainer record if role or department is Trainer/Tech Lead/Manager
        boolean isTrainer = "TRAINER".equalsIgnoreCase(employee.getRole())
                || "TECH_LEAD".equalsIgnoreCase(employee.getRole())
                || "TECHNICAL_LEAD".equalsIgnoreCase(employee.getRole())
                || "MANAGER".equalsIgnoreCase(employee.getRole())
                || "ASSISTANT_MANAGER".equalsIgnoreCase(employee.getRole())
                || (employee.getDepartment() != null && employee.getDepartment().toLowerCase().contains("trainer"))
                || (employee.getDepartment() != null && employee.getDepartment().toLowerCase().contains("technical"))
                || (employee.getDepartment() != null && employee.getDepartment().toLowerCase().contains("training"));

        if (isTrainer) {
            final String targetEmail = employee.getEmail().trim();
            final String searchOld = oldEmail != null ? oldEmail.trim() : null;
            Trainer trainer = trainerRepository.findAll().stream()
                    .filter(t -> (t.getEmail() != null && t.getEmail().equalsIgnoreCase(targetEmail))
                            || (employee.getPhone() != null && employee.getPhone().equals(t.getPhone())))
                    .findFirst().orElse(null);

            if (trainer == null && searchOld != null) {
                trainer = trainerRepository.findAll().stream()
                        .filter(t -> t.getEmail() != null && t.getEmail().equalsIgnoreCase(searchOld))
                        .findFirst().orElse(null);
            }

            if (trainer == null) {
                trainer = new Trainer();
            }

            trainer.setName(employee.getName());
            trainer.setEmail(employee.getEmail().trim());
            trainer.setPhone(employee.getPhone());
            trainer.setExperience(employee.getExperience() != null ? employee.getExperience() : 2);
            trainer.setCourses(employee.getSkills() != null && !employee.getSkills().isBlank() ? employee.getSkills()
                    : "Java, Python");
            trainer.setAvailableTime("09:00 AM - 06:00 PM");
            trainer.setRole("TECH_LEAD".equalsIgnoreCase(employee.getRole())
                    || "TECHNICAL_LEAD".equalsIgnoreCase(employee.getRole()) ? "TECH_LEAD" : "TRAINER");
            if (trainer.getStudentCount() == null) {
                trainer.setStudentCount(0);
            }
            if (trainer.getPerformance() == null) {
                trainer.setPerformance(5.0);
            }
            trainerRepository.save(trainer);
        } else {
            final String targetEmail = employee.getEmail().trim();
            Trainer trainer = trainerRepository.findAll().stream()
                    .filter(t -> t.getEmail() != null && t.getEmail().equalsIgnoreCase(targetEmail))
                    .findFirst().orElse(null);
            if (trainer != null) {
                trainerRepository.delete(trainer);
            }
        }
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
        if (role == null)
            return "Sales Execution";
        String normalized = role.trim().toUpperCase();
        if ("HR".equals(normalized) || "HR_HEAD".equals(normalized)) {
            return "Human Resources";
        }
        if ("MANAGER".equals(normalized) || "BD_MANAGER".equals(normalized) || "ASSISTANT_MANAGER".equals(normalized)) {
            return "Sales Management";
        }
        if ("TRAINER".equals(normalized) || "TECH_LEAD".equals(normalized)) {
            return "Training & Development";
        }
        return "Sales Execution";
    }

    // ==========================================
    // EMPLOYEE ATTENDANCE ENDPOINTS
    // ==========================================
    @GetMapping("/employees/attendance")
    public ResponseEntity<List<EmployeeAttendance>> getEmployeeAttendance() {
        return ResponseEntity.ok(employeeAttendanceRepository.findAll());
    }

    @PostMapping("/employees/attendance/punch")
    public ResponseEntity<?> punchAttendance(@RequestBody Map<String, String> request) {
        String empId = request.get("employeeId");
        String action = request.get("action"); // IN or OUT

        Optional<Employee> empOpt = employeeRepository.findAll().stream()
                .filter(e -> empId.equalsIgnoreCase(e.getEmployeeId()))
                .findFirst();

        if (empOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));
        }

        Employee employee = empOpt.get();
        LocalDate today = LocalDate.now();

        if ("IN".equalsIgnoreCase(action)) {
            // Check if already checked in today
            List<EmployeeAttendance> todayAtt = employeeAttendanceRepository.findAll().stream()
                    .filter(a -> a.getEmployee().getId().equals(employee.getId()) && today.equals(a.getDate()))
                    .toList();
            if (!todayAtt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Already punched in today"));
            }

            EmployeeAttendance att = new EmployeeAttendance();
            att.setEmployee(employee);
            att.setDate(today);
            att.setLoginTime(LocalDateTime.now());
            att.setStatus(LocalDateTime.now().getHour() >= 10 ? "LATE" : "PRESENT");
            att.setLateLogin(LocalDateTime.now().getHour() >= 10);

            EmployeeAttendance saved = employeeAttendanceRepository.save(att);
            return ResponseEntity.ok(saved);
        } else if ("OUT".equalsIgnoreCase(action)) {
            // Find existing check-in today
            Optional<EmployeeAttendance> openAtt = employeeAttendanceRepository.findAll().stream()
                    .filter(a -> a.getEmployee().getId().equals(employee.getId()) && today.equals(a.getDate())
                            && a.getLogoutTime() == null)
                    .findFirst();
            if (openAtt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No active punch-in session found today"));
            }

            EmployeeAttendance att = openAtt.get();
            att.setLogoutTime(LocalDateTime.now());
            // calculate hours worked
            java.time.Duration duration = java.time.Duration.between(att.getLoginTime(), att.getLogoutTime());
            double hours = duration.toMinutes() / 60.0;
            att.setHoursWorked(Math.round(hours * 100.0) / 100.0);
            att.setEarlyLogout(LocalDateTime.now().getHour() < 17);

            EmployeeAttendance saved = employeeAttendanceRepository.save(att);
            return ResponseEntity.ok(saved);
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid action"));
    }

    @GetMapping("/employees/login-history")
    public ResponseEntity<List<EmployeeLoginHistory>> getLoginHistory() {
        return ResponseEntity.ok(employeeLoginHistoryRepository.findAll());
    }

    // ==========================================
    // STUDENT ENDPOINTS
    // ==========================================
    @GetMapping("/students")
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll());
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<?> getStudentById(@PathVariable Long id) {
        Optional<Student> studentOpt = studentRepository.findById(id);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Student student = studentOpt.get();
        List<StudentDocument> docs = studentDocumentRepository.findByStudentId(id);
        List<StudentPayment> payments = studentPaymentRepository.findAll().stream()
                .filter(p -> p.getStudent().getId().equals(id))
                .toList();
        List<StudentNote> notes = studentNoteRepository.findAll().stream()
                .filter(n -> n.getStudent().getId().equals(id))
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("student", student);
        data.put("documents", docs);
        data.put("payments", payments);
        data.put("notes", notes);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/students")
    public ResponseEntity<?> createStudent(@RequestBody Student student) {
        if (student.getEmail() != null && studentRepository.existsByEmail(student.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        long studentCount = studentRepository.count();
        student.setStudentId("STU-" + (1000 + studentCount + 1));
        if (student.getJoiningDate() == null) {
            student.setJoiningDate(LocalDate.now());
        }
        if (student.getStatus() == null) {
            student.setStatus("PENDING_VERIFICATION");
        }
        Student saved = studentRepository.save(student);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student details) {
        return studentRepository.findById(id).map(student -> {
            student.setName(details.getName());
            student.setPhoto(details.getPhoto());
            student.setPhone(details.getPhone());
            student.setAddress(details.getAddress());
            student.setCollege(details.getCollege());
            student.setQualification(details.getQualification());
            student.setCoursePurchased(details.getCoursePurchased());
            student.setCourseFees(details.getCourseFees());
            student.setPaidAmount(details.getPaidAmount());
            student.setBalance(details.getCourseFees() - details.getPaidAmount());
            student.setStatus(details.getStatus());
            if (details.getRating() != null) {
                student.setRating(details.getRating());
            }
            if (details.getMockScore() != null) {
                student.setMockScore(details.getMockScore());
            }
            if (details.getProjectGrade() != null) {
                student.setProjectGrade(details.getProjectGrade());
            }

            if (details.getBatch() != null && details.getBatch().getId() != null) {
                batchRepository.findById(details.getBatch().getId()).ifPresent(student::setBatch);
            } else {
                student.setBatch(null);
            }

            if (details.getTrainer() != null && details.getTrainer().getId() != null) {
                trainerRepository.findById(details.getTrainer().getId()).ifPresent(student::setTrainer);
            } else {
                student.setTrainer(null);
            }

            return ResponseEntity.ok(studentRepository.save(student));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        return studentRepository.findById(id).map(student -> {
            studentRepository.delete(student);
            return ResponseEntity.ok(Map.of("message", "Student deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/students/{id}/verify-document")
    public ResponseEntity<?> verifyDocument(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String docType = request.get("documentType");
        String status = request.get("status"); // Verified, Rejected
        String remarks = request.get("remarks");

        Optional<Student> studentOpt = studentRepository.findById(id);
        if (studentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Student student = studentOpt.get();

        // Find or create document record
        List<StudentDocument> studentDocs = studentDocumentRepository.findByStudentId(id);
        Optional<StudentDocument> docOpt = studentDocs.stream()
                .filter(d -> docType.equalsIgnoreCase(d.getDocumentType()))
                .findFirst();

        StudentDocument doc;
        if (docOpt.isPresent()) {
            doc = docOpt.get();
        } else {
            doc = new StudentDocument();
            doc.setStudent(student);
            doc.setDocumentType(docType);
        }
        doc.setStatus(status);
        doc.setRemarks(remarks);
        studentDocumentRepository.save(doc);

        // Check if all essential documents (Aadhar, PAN) are verified, auto update
        // student status
        List<StudentDocument> updatedDocs = studentDocumentRepository.findByStudentId(id);
        boolean hasAadhar = updatedDocs.stream().anyMatch(
                d -> "Aadhar".equalsIgnoreCase(d.getDocumentType()) && "Verified".equalsIgnoreCase(d.getStatus()));
        boolean hasPan = updatedDocs.stream().anyMatch(
                d -> "PAN".equalsIgnoreCase(d.getDocumentType()) && "Verified".equalsIgnoreCase(d.getStatus()));

        if (hasAadhar && hasPan && "PENDING_VERIFICATION".equalsIgnoreCase(student.getStatus())) {
            student.setStatus("BATCH_NOT_ASSIGNED");
            studentRepository.save(student);

            // Create notification
            HrNotification notif = new HrNotification();
            notif.setMessage("Documents verified for " + student.getName() + ". Ready for Batch allocation.");
            notif.setType("DOCUMENT_VERIFIED");
            notif.setCreatedAt(LocalDateTime.now());
            notif.setIsRead(false);
            hrNotificationRepository.save(notif);
        }

        return ResponseEntity.ok(Map.of("message", "Document verified successfully", "document", doc, "studentStatus",
                student.getStatus()));
    }

    @PostMapping("/students/assign-batch")
    public ResponseEntity<?> assignBatch(@RequestBody Map<String, Long> request) {
        Long studentId = request.get("studentId");
        Long batchId = request.get("batchId");
        Long trainerId = request.get("trainerId");

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<Batch> batchOpt = batchRepository.findById(batchId);
        Optional<Trainer> trainerOpt = trainerRepository.findById(trainerId);

        if (studentOpt.isEmpty() || batchOpt.isEmpty() || trainerOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student, Batch, or Trainer not found"));
        }

        Student student = studentOpt.get();
        Batch batch = batchOpt.get();
        Trainer trainer = trainerOpt.get();

        student.setBatch(batch);
        student.setTrainer(trainer);
        student.setStatus("BATCH_ASSIGNED");
        studentRepository.save(student);

        // Increment trainer student count and set batch
        trainer.setStudentCount(trainer.getStudentCount() + 1);
        trainer.setCurrentBatch(batch.getBatchName());
        trainerRepository.save(trainer);

        // Decrement batch available seats
        if (batch.getAvailableSeats() > 0) {
            batch.setAvailableSeats(batch.getAvailableSeats() - 1);
            batchRepository.save(batch);
        }

        // Add Notification
        HrNotification notif = new HrNotification();
        notif.setMessage("Student " + student.getName() + " assigned to batch: " + batch.getBatchName() + " (Trainer: "
                + trainer.getName() + ")");
        notif.setType("BATCH_ASSIGNED");
        notif.setCreatedAt(LocalDateTime.now());
        notif.setIsRead(false);
        hrNotificationRepository.save(notif);

        return ResponseEntity.ok(Map.of("message", "Batch and Trainer assigned successfully", "student", student));
    }

    // ==========================================
    // BATCH ENDPOINTS
    // ==========================================
    @GetMapping("/batches")
    public ResponseEntity<List<Batch>> getAllBatches() {
        return ResponseEntity.ok(batchRepository.findAll());
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<Batch> getBatchById(@PathVariable Long id) {
        return batchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batches")
    public ResponseEntity<?> createBatch(@RequestBody Batch batch) {
        if (batch.getTrainer() != null && batch.getTrainer().getId() != null) {
            Optional<Trainer> t = trainerRepository.findById(batch.getTrainer().getId());
            t.ifPresent(batch::setTrainer);
        }
        if (batch.getSeats() == null)
            batch.setSeats(30);
        if (batch.getAvailableSeats() == null)
            batch.setAvailableSeats(batch.getSeats());
        if (batch.getStatus() == null)
            batch.setStatus("Upcoming");

        Batch saved = batchRepository.save(batch);

        HrNotification notif = new HrNotification();
        notif.setMessage("New Batch Created: " + batch.getBatchName() + " for " + batch.getCourse());
        notif.setType("BATCH_STARTED");
        notif.setCreatedAt(LocalDateTime.now());
        notif.setIsRead(false);
        hrNotificationRepository.save(notif);

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/batches/{id}")
    public ResponseEntity<?> updateBatch(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Optional<Batch> batchOpt = batchRepository.findById(id);
        if (batchOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Batch batch = batchOpt.get();

        if (payload.containsKey("batchName"))
            batch.setBatchName((String) payload.get("batchName"));
        if (payload.containsKey("course"))
            batch.setCourse((String) payload.get("course"));
        if (payload.containsKey("startingDate")) {
            String startingDateStr = (String) payload.get("startingDate");
            batch.setStartingDate(
                    startingDateStr != null && !startingDateStr.isEmpty() ? LocalDate.parse(startingDateStr) : null);
        }
        if (payload.containsKey("endingDate")) {
            String endingDateStr = (String) payload.get("endingDate");
            batch.setEndingDate(
                    endingDateStr != null && !endingDateStr.isEmpty() ? LocalDate.parse(endingDateStr) : null);
        }
        if (payload.containsKey("duration"))
            batch.setDuration((String) payload.get("duration"));
        if (payload.containsKey("mode"))
            batch.setMode((String) payload.get("mode"));
        if (payload.containsKey("timing"))
            batch.setTiming((String) payload.get("timing"));
        if (payload.containsKey("seats")) {
            int seats = Integer.parseInt(payload.get("seats").toString());
            int diff = seats - batch.getSeats();
            batch.setSeats(seats);
            batch.setAvailableSeats(Math.max(0, batch.getAvailableSeats() + diff));
        }
        if (payload.containsKey("availableSeats"))
            batch.setAvailableSeats(Integer.parseInt(payload.get("availableSeats").toString()));
        if (payload.containsKey("status"))
            batch.setStatus((String) payload.get("status"));

        if (payload.containsKey("trainer") && payload.get("trainer") != null) {
            Map<String, Object> trainerMap = (Map<String, Object>) payload.get("trainer");
            if (trainerMap.containsKey("id") && trainerMap.get("id") != null) {
                Long trainerId = Long.valueOf(trainerMap.get("id").toString());
                Optional<Trainer> trainerOpt = trainerRepository.findById(trainerId);
                if (trainerOpt.isPresent()) {
                    Trainer oldTrainer = batch.getTrainer();
                    Trainer newTrainer = trainerOpt.get();
                    batch.setTrainer(newTrainer);

                    // Reassign students mapped to this batch to the new trainer
                    List<Student> students = studentRepository.findAll().stream()
                            .filter(s -> s.getBatch() != null && s.getBatch().getId().equals(batch.getId()))
                            .toList();
                    for (Student s : students) {
                        s.setTrainer(newTrainer);
                        studentRepository.save(s);
                    }

                    // Update trainer workloads
                    if (oldTrainer != null) {
                        int currentCount = oldTrainer.getStudentCount() != null ? oldTrainer.getStudentCount() : 0;
                        oldTrainer.setStudentCount(Math.max(0, currentCount - students.size()));
                        trainerRepository.save(oldTrainer);
                    }
                    int newCount = newTrainer.getStudentCount() != null ? newTrainer.getStudentCount() : 0;
                    newTrainer.setStudentCount(newCount + students.size());
                    trainerRepository.save(newTrainer);
                }
            }
        }

        Batch saved = batchRepository.save(batch);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/batches/{id}/students")
    public ResponseEntity<List<Student>> getStudentsByBatch(@PathVariable Long id) {
        // Verify batch exists
        if (!batchRepository.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<Student> students = studentRepository.findByBatchId(id);
        return ResponseEntity.ok(students);
    }

    @DeleteMapping("/batches/{id}")
    public ResponseEntity<?> deleteBatch(@PathVariable Long id) {
        return batchRepository.findById(id).map(batch -> {
            List<Student> students = studentRepository.findAll().stream()
                    .filter(s -> s.getBatch() != null && s.getBatch().getId().equals(id))
                    .toList();
            for (Student s : students) {
                s.setBatch(null);
                s.setTrainer(null);
                s.setStatus("BATCH_NOT_ASSIGNED");
                studentRepository.save(s);
            }

            Trainer trainer = batch.getTrainer();
            if (trainer != null) {
                int currentCount = trainer.getStudentCount() != null ? trainer.getStudentCount() : 0;
                trainer.setStudentCount(Math.max(0, currentCount - students.size()));
                trainerRepository.save(trainer);
            }

            batchRepository.delete(batch);
            return ResponseEntity.ok(Map.of("message", "Batch deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/attendance/students")
    public ResponseEntity<List<StudentAttendance>> getStudentAttendance(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String date) {
        List<StudentAttendance> list = studentAttendanceRepository.findAll();
        if (batchId != null) {
            list = list.stream().filter(a -> a.getBatch() != null && a.getBatch().getId().equals(batchId)).toList();
        }
        if (date != null && !date.trim().isEmpty()) {
            LocalDate localDate = LocalDate.parse(date);
            list = list.stream().filter(a -> localDate.equals(a.getDate())).toList();
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/attendance/students")
    public ResponseEntity<?> recordStudentAttendance(@RequestBody List<Map<String, Object>> payloadList) {
        List<StudentAttendance> savedList = new ArrayList<>();
        for (Map<String, Object> payload : payloadList) {
            Long studentId = Long.valueOf(payload.get("studentId").toString());
            Long batchId = Long.valueOf(payload.get("batchId").toString());
            LocalDate date = LocalDate.parse((String) payload.get("date"));
            String status = (String) payload.get("status");

            Optional<Student> studentOpt = studentRepository.findById(studentId);
            Optional<Batch> batchOpt = batchRepository.findById(batchId);

            if (studentOpt.isPresent() && batchOpt.isPresent()) {
                Optional<StudentAttendance> existingOpt = studentAttendanceRepository.findAll().stream()
                        .filter(a -> a.getStudent().getId().equals(studentId) &&
                                a.getBatch().getId().equals(batchId) &&
                                date.equals(a.getDate()))
                        .findFirst();

                StudentAttendance att = existingOpt.orElse(new StudentAttendance());
                att.setStudent(studentOpt.get());
                att.setBatch(batchOpt.get());
                att.setDate(date);
                att.setStatus(status);

                savedList.add(studentAttendanceRepository.save(att));
            }
        }
        return ResponseEntity.ok(savedList);
    }

    @PostMapping("/employees/attendance/manual")
    public ResponseEntity<?> manualEmployeeAttendance(@RequestBody Map<String, Object> request) {
        String empId = (String) request.get("employeeId");
        LocalDate date = LocalDate.parse((String) request.get("date"));
        String status = (String) request.get("status");
        Double hoursWorked = request.get("hoursWorked") != null ? Double.valueOf(request.get("hoursWorked").toString())
                : 8.0;

        Optional<Employee> empOpt = employeeRepository.findAll().stream()
                .filter(e -> empId.equalsIgnoreCase(e.getEmployeeId()))
                .findFirst();

        if (empOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));
        }

        Employee employee = empOpt.get();

        Optional<EmployeeAttendance> existingOpt = employeeAttendanceRepository.findAll().stream()
                .filter(a -> a.getEmployee().getId().equals(employee.getId()) && date.equals(a.getDate()))
                .findFirst();

        EmployeeAttendance att = existingOpt.orElse(new EmployeeAttendance());
        att.setEmployee(employee);
        att.setDate(date);
        att.setStatus(status);
        att.setHoursWorked(hoursWorked);

        if ("PRESENT".equalsIgnoreCase(status) || "LATE".equalsIgnoreCase(status)) {
            if (att.getLoginTime() == null) {
                att.setLoginTime(date.atTime(9, 0));
            }
            if (att.getLogoutTime() == null) {
                att.setLogoutTime(date.atTime(17, 0));
            }
            att.setLateLogin("LATE".equalsIgnoreCase(status));
            att.setEarlyLogout(false);
        } else {
            att.setLoginTime(null);
            att.setLogoutTime(null);
            att.setLateLogin(false);
            att.setEarlyLogout(false);
            att.setHoursWorked(0.0);
        }

        EmployeeAttendance saved = employeeAttendanceRepository.save(att);
        return ResponseEntity.ok(saved);
    }

    // ==========================================
    // TRAINER ENDPOINTS
    // ==========================================
    @GetMapping("/trainers")
    public ResponseEntity<List<Trainer>> getAllTrainers() {
        return ResponseEntity.ok(trainerRepository.findAll());
    }

    @PostMapping("/trainers")
    public ResponseEntity<Trainer> createTrainer(@RequestBody Trainer trainer) {
        if (trainer.getStudentCount() == null)
            trainer.setStudentCount(0);
        if (trainer.getPerformance() == null)
            trainer.setPerformance(5.0);
        if (trainer.getRole() == null || trainer.getRole().isBlank())
            trainer.setRole("TRAINER");
        Trainer saved = trainerRepository.save(trainer);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/trainers/{id}")
    public ResponseEntity<?> updateTrainer(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return trainerRepository.findById(id).map(trainer -> {
            if (payload.containsKey("name"))
                trainer.setName((String) payload.get("name"));
            if (payload.containsKey("email"))
                trainer.setEmail((String) payload.get("email"));
            if (payload.containsKey("phone"))
                trainer.setPhone((String) payload.get("phone"));
            if (payload.containsKey("experience"))
                trainer.setExperience(Integer.parseInt(payload.get("experience").toString()));
            if (payload.containsKey("courses"))
                trainer.setCourses((String) payload.get("courses"));
            if (payload.containsKey("availableTime"))
                trainer.setAvailableTime((String) payload.get("availableTime"));
            if (payload.containsKey("performance"))
                trainer.setPerformance(Double.parseDouble(payload.get("performance").toString()));
            Trainer saved = trainerRepository.save(trainer);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/trainers/{id}")
    public ResponseEntity<?> deleteTrainer(@PathVariable Long id) {
        return trainerRepository.findById(id).map(trainer -> {
            // Unassign batches from this trainer
            List<Batch> batches = batchRepository.findByTrainerId(id);
            for (Batch b : batches) {
                b.setTrainer(null);
                batchRepository.save(b);
            }
            // Unassign students from this trainer
            List<Student> students = studentRepository.findByTrainerId(id);
            for (Student s : students) {
                s.setTrainer(null);
                studentRepository.save(s);
            }
            trainerRepository.delete(trainer);
            return ResponseEntity.ok(Map.of("message", "Trainer deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/trainers/{id}/batches")
    public ResponseEntity<?> getBatchesByTrainer(@PathVariable Long id) {
        if (!trainerRepository.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<Batch> batches = batchRepository.findByTrainerId(id);
        return ResponseEntity.ok(batches);
    }

    // ==========================================
    // PLACEMENT ENDPOINTS
    // ==========================================
    @GetMapping("/placements")
    public ResponseEntity<List<Placement>> getAllPlacements() {
        return ResponseEntity.ok(placementRepository.findAll());
    }

    @GetMapping("/placements/companies")
    public ResponseEntity<List<PlacementCompany>> getAllCompanies() {
        return ResponseEntity.ok(placementCompanyRepository.findAll());
    }

    @PostMapping("/placements/companies")
    public ResponseEntity<PlacementCompany> createCompany(@RequestBody PlacementCompany company) {
        PlacementCompany saved = placementCompanyRepository.save(company);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/placements")
    public ResponseEntity<?> schedulePlacement(@RequestBody Map<String, Object> request) {
        Long studentId = Long.valueOf(request.get("studentId").toString());
        Long companyId = Long.valueOf(request.get("companyId").toString());
        String position = (String) request.get("position");
        Double salary = Double.valueOf(request.get("salary").toString());
        LocalDate interviewDate = LocalDate.parse((String) request.get("interviewDate"));
        String interviewTime = (String) request.get("interviewTime");
        String round = (String) request.get("interviewRound");
        String status = (String) request.get("status");

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Optional<PlacementCompany> companyOpt = placementCompanyRepository.findById(companyId);

        if (studentOpt.isEmpty() || companyOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Student or Placement Company not found"));
        }

        Student student = studentOpt.get();
        PlacementCompany company = companyOpt.get();

        Placement placement = new Placement();
        placement.setStudent(student);
        placement.setCompany(company);
        placement.setPosition(position);
        placement.setSalary(salary);
        placement.setInterviewDate(interviewDate);
        placement.setInterviewTime(interviewTime);
        placement.setInterviewRound(round);
        placement.setStatus(status != null ? status : "Scheduled");
        placement.setPackageAmount(salary * 12 / 100000.0); // Package in Lakhs (LPA)

        Placement saved = placementRepository.save(placement);

        // Update student status to PLACED if selected/placed
        if ("Selected".equalsIgnoreCase(status) || "Placed".equalsIgnoreCase(status)
                || "Joined".equalsIgnoreCase(status)) {
            student.setStatus("PLACED");
            studentRepository.save(student);
        }

        // Add Notification
        HrNotification notif = new HrNotification();
        notif.setMessage("Interview scheduled for " + student.getName() + " at " + company.getCompanyName());
        notif.setType("PLACEMENT_SCHEDULED");
        notif.setCreatedAt(LocalDateTime.now());
        notif.setIsRead(false);
        hrNotificationRepository.save(notif);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/placements/stats")
    public ResponseEntity<?> getPlacementStats() {
        List<Placement> placements = placementRepository.findAll();
        long totalSchedules = placements.size();
        long selected = placements
                .stream().filter(p -> "Selected".equalsIgnoreCase(p.getStatus())
                        || "Placed".equalsIgnoreCase(p.getStatus()) || "Joined".equalsIgnoreCase(p.getStatus()))
                .count();
        long rejected = placements.stream().filter(p -> "Rejected".equalsIgnoreCase(p.getStatus())).count();
        long pending = totalSchedules - selected - rejected;

        double highestLpa = placements.stream().mapToDouble(Placement::getPackageAmount).max().orElse(0.0);
        double averageLpa = placements.stream().mapToDouble(Placement::getPackageAmount).average().orElse(0.0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSchedules", totalSchedules);
        stats.put("selected", selected);
        stats.put("rejected", rejected);
        stats.put("pending", pending);
        stats.put("highestLpa", Math.round(highestLpa * 10.0) / 10.0);
        stats.put("averageLpa", Math.round(averageLpa * 10.0) / 10.0);
        return ResponseEntity.ok(stats);
    }

    // ==========================================
    // NOTIFICATION ENDPOINTS
    // ==========================================
    @GetMapping("/notifications")
    public ResponseEntity<List<HrNotification>> getAllNotifications() {
        List<HrNotification> notifs = hrNotificationRepository.findAll();
        notifs.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt())); // newest first
        return ResponseEntity.ok(notifs);
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id) {
        return hrNotificationRepository.findById(id).map(notif -> {
            notif.setIsRead(true);
            hrNotificationRepository.save(notif);
            return ResponseEntity.ok(Map.of("success", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // DASHBOARD STATS
    // ==========================================
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        long totalStudents = studentRepository.count();
        long pendingVerification = studentRepository.findAll().stream()
                .filter(s -> "PENDING_VERIFICATION".equalsIgnoreCase(s.getStatus())).count();
        long waitingForBatch = studentRepository.findAll().stream()
                .filter(s -> "BATCH_NOT_ASSIGNED".equalsIgnoreCase(s.getStatus())
                        || "DOCUMENTS_PENDING".equalsIgnoreCase(s.getStatus()))
                .count();
        long placedCount = studentRepository.findAll().stream().filter(s -> "PLACED".equalsIgnoreCase(s.getStatus()))
                .count();
        long activeTrainers = trainerRepository.count();
        long totalEmployees = employeeRepository.count();

        LocalDate today = LocalDate.now();
        long todayAttendance = employeeAttendanceRepository.findAll().stream()
                .filter(a -> today.equals(a.getDate()))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("pendingVerification", pendingVerification);
        stats.put("waitingForBatch", waitingForBatch);
        stats.put("placedCount", placedCount);
        stats.put("activeTrainers", activeTrainers);
        stats.put("totalEmployees", totalEmployees);
        stats.put("todayAttendanceCount", todayAttendance);

        // Chart mock/aggregated data
        // Monthly Admissions (Jan - Jun)
        stats.put("monthlyAdmissions", List.of(30, 45, 60, 50, 75, 90));
        // Placements Monthly
        stats.put("monthlyPlacements", List.of(5, 12, 18, 15, 22, 35));

        return ResponseEntity.ok(stats);
    }

    // ==========================================
    // REPORTS & EXPORT
    // ==========================================
    @GetMapping("/reports/export")
    public ResponseEntity<?> getReportData(@RequestParam String type) {
        // Return structured data for frontend to export as CSV
        List<Map<String, Object>> data = new ArrayList<>();

        if ("students".equalsIgnoreCase(type)) {
            List<Student> students = studentRepository.findAll();
            for (Student s : students) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("Student ID", s.getStudentId());
                row.put("Name", s.getName());
                row.put("Email", s.getEmail());
                row.put("Phone", s.getPhone());
                row.put("Course", s.getCoursePurchased());
                row.put("Status", s.getStatus());
                row.put("Joining Date", s.getJoiningDate().toString());
                data.add(row);
            }
        } else if ("employees".equalsIgnoreCase(type)) {
            List<Employee> employees = employeeRepository.findAll();
            for (Employee e : employees) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("Employee ID", e.getEmployeeId());
                row.put("Name", e.getName());
                row.put("Email", e.getEmail());
                row.put("Role", e.getRole());
                row.put("Department", e.getDepartment());
                row.put("Status", e.getStatus());
                data.add(row);
            }
        } else if ("attendance".equalsIgnoreCase(type)) {
            List<EmployeeAttendance> atts = employeeAttendanceRepository.findAll();
            for (EmployeeAttendance a : atts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("Date", a.getDate().toString());
                row.put("Employee ID", a.getEmployee().getEmployeeId());
                row.put("Name", a.getEmployee().getName());
                row.put("Login Time", a.getLoginTime() != null ? a.getLoginTime().toString() : "N/A");
                row.put("Logout Time", a.getLogoutTime() != null ? a.getLogoutTime().toString() : "N/A");
                row.put("Hours Worked", a.getHoursWorked());
                row.put("Status", a.getStatus());
                data.add(row);
            }
        }

        return ResponseEntity.ok(data);
    }
}
