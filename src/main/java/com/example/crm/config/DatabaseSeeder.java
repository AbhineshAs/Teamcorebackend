package com.example.crm.config;

import com.example.crm.entity.User;
import com.example.crm.repository.UserRepository;
import com.example.crm.entity.Lead;
import com.example.crm.entity.Student;
import com.example.crm.repository.LeadRepository;
import com.example.crm.repository.StudentRepository;
import com.example.crm.repository.EmployeeRepository;
import com.example.crm.repository.TrainerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final String TEAMCORE_ADMIN_EMAIL = "admin@teamcore.com";
    private static final String TEAMCORE_ADMIN_PASSWORD = "admin@123";
    private static final String WHITEAURAX_ADMIN_EMAIL = "admin@whiteaurax.com";
    private static final String WHITEAURAX_ADMIN_PASSWORD = "password";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TrainerRepository trainerRepository;

    @Override
    public void run(String... args) throws Exception {

        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (TEAMCORE_ADMIN_EMAIL.equalsIgnoreCase(u.getEmail())) {
                if (u.getPassword() != null && u.getPassword().startsWith("data:image")) {
                    u.setEmail("admin_corrupted_" + u.getId() + "@teamcore.com");
                    u.setPassword(TEAMCORE_ADMIN_PASSWORD);
                    try {
                        userRepository.save(u);
                    } catch (Exception e) {
                        System.err.println("Could not rename corrupted admin: " + e.getMessage());
                    }
                }
            }

        }

        ensureAdminUser(TEAMCORE_ADMIN_EMAIL, TEAMCORE_ADMIN_PASSWORD, "TeamCore Admin");
        ensureAdminUser(WHITEAURAX_ADMIN_EMAIL, WHITEAURAX_ADMIN_PASSWORD, "Site Admin");

        // Sync existing closed leads to students table
        try {
            List<Lead> closedLeads = leadRepository.findAll().stream()
                .filter(l -> "Closed Won".equalsIgnoreCase(l.getStatus()) || "Close".equalsIgnoreCase(l.getStatus()) || "Closed".equalsIgnoreCase(l.getStatus()))
                .toList();

            for (Lead lead : closedLeads) {
                boolean studentExists = studentRepository.findAll().stream()
                    .anyMatch(s -> (s.getEmail() != null && s.getEmail().equalsIgnoreCase(lead.getEmail())) 
                                || (s.getPhone() != null && s.getPhone().equals(lead.getPhoneNumber())));
                if (!studentExists) {
                    Student student = new Student();
                    long studentCount = studentRepository.count();
                    student.setStudentId("STU-" + (1000 + studentCount + 1));
                    student.setName(lead.getCustomerName());
                    student.setPhone(lead.getPhoneNumber());
                    student.setEmail(lead.getEmail());
                    student.setCollege(lead.getCollege());
                    student.setQualification(lead.getDepartment() != null ? lead.getDepartment() : "Not Specified");
                    student.setCoursePurchased(lead.getCourse() != null ? lead.getCourse() : "Java Full Stack");
                    student.setCourseFees(lead.getValue() != null ? lead.getValue() : 60000.0);
                    student.setPaidAmount(lead.getValue() != null ? lead.getValue() : 60000.0);
                    student.setBalance(0.0);
                    student.setJoiningDate(lead.getClosedDate() != null ? lead.getClosedDate() : java.time.LocalDate.now());
                    student.setSalesExecutive(lead.getLastUpdatedBy() != null ? lead.getLastUpdatedBy() : "System");
                    student.setStatus("PENDING_VERIFICATION");
                    studentRepository.save(student);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not sync closed leads to students: " + e.getMessage());
        }

        // Sync existing employees of type Trainer/Tech Lead to trainers table
        try {
            List<com.example.crm.entity.Employee> trainerEmployees = employeeRepository.findAll().stream()
                .filter(emp -> "TRAINER".equalsIgnoreCase(emp.getRole()) 
                            || "TECH_LEAD".equalsIgnoreCase(emp.getRole())
                            || (emp.getDepartment() != null && emp.getDepartment().toLowerCase().contains("trainer"))
                            || (emp.getDepartment() != null && emp.getDepartment().toLowerCase().contains("training")))
                .toList();

            for (com.example.crm.entity.Employee emp : trainerEmployees) {
                boolean trainerExists = trainerRepository.findAll().stream()
                    .anyMatch(t -> (t.getEmail() != null && t.getEmail().equalsIgnoreCase(emp.getEmail())) 
                                || (t.getPhone() != null && t.getPhone().equals(emp.getPhone())));
                if (!trainerExists) {
                    com.example.crm.entity.Trainer trainer = new com.example.crm.entity.Trainer();
                    trainer.setName(emp.getName());
                    trainer.setEmail(emp.getEmail());
                    trainer.setPhone(emp.getPhone());
                    trainer.setExperience(emp.getExperience() != null ? emp.getExperience() : 2);
                    trainer.setCourses(emp.getSkills() != null && !emp.getSkills().isBlank() ? emp.getSkills() : "Java, Python");
                    trainer.setAvailableTime("09:00 AM - 06:00 PM");
                    trainer.setStudentCount(0);
                    trainer.setPerformance(5.0);
                    trainer.setRole("TECH_LEAD".equalsIgnoreCase(emp.getRole()) || "TECHNICAL_LEAD".equalsIgnoreCase(emp.getRole()) ? "TECH_LEAD" : "TRAINER");
                    trainerRepository.save(trainer);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not sync trainer employees to trainers directory: " + e.getMessage());
        }
    }

    private void ensureAdminUser(String email, String password, String name) {
        User admin = userRepository.findByEmailIgnoreCase(email);
        if (admin == null) {
            admin = new User();
            admin.setEmail(email);
        }

        admin.setName(admin.getName() == null || admin.getName().isBlank() ? name : admin.getName());
        admin.setPassword(password);
        admin.setRole("ADMIN");
        admin.setPosition(admin.getPosition() == null || admin.getPosition().isBlank() ? "Administrator" : admin.getPosition());
        admin.setSalary(admin.getSalary() == null ? 30000.0 : admin.getSalary());
        admin.setDateOfJoining(admin.getDateOfJoining() == null ? java.time.LocalDate.of(2024, 1, 1) : admin.getDateOfJoining());

        try {
            userRepository.save(admin);
        } catch (Exception e) {
            System.err.println("Could not seed admin user " + email + ": " + e.getMessage());
        }
    }
}
