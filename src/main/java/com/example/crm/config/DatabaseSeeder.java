package com.example.crm.config;

import com.example.crm.entity.Lead;
import com.example.crm.entity.User;
import com.example.crm.repository.LeadRepository;
import com.example.crm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Override
    public void run(String... args) throws Exception {
        // 1. Rename corrupted duplicate admin/manager accounts to avoid constraint violations
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if ("admin@teamcore.com".equalsIgnoreCase(u.getEmail())) {
                if (u.getPassword() != null && u.getPassword().startsWith("data:image")) {
                    // This is the corrupted duplicate, rename it
                    u.setEmail("admin_corrupted_" + u.getId() + "@teamcore.com");
                    u.setPassword("admin@123");
                    try {
                        userRepository.save(u);
                    } catch (Exception e) {
                        System.err.println("Could not rename corrupted admin: " + e.getMessage());
                    }
                }
            }
            if ("jees@teamcore.com".equalsIgnoreCase(u.getEmail())) {
                if (u.getPassword() != null && u.getPassword().startsWith("data:image")) {
                    u.setEmail("jees_corrupted_" + u.getId() + "@teamcore.com");
                    u.setPassword("jees@123");
                    try {
                        userRepository.save(u);
                    } catch (Exception e) {
                        System.err.println("Could not rename corrupted manager: " + e.getMessage());
                    }
                }
            }
        }

        // 2. Seed or correct Admin user
        User admin = userRepository.findByEmail("admin@teamcore.com");
        if (admin == null) {
            admin = new User();
            admin.setName("Admin");
            admin.setEmail("admin@teamcore.com");
            admin.setPassword("admin@123");
            admin.setRole("ADMIN");
            admin.setPosition("Administrator");
            admin.setDateOfJoining(LocalDate.of(2024, 1, 1));
            admin = userRepository.save(admin);
        }

        // 3. Seed Manager
        User manager = userRepository.findByEmail("manager@company.com");
        if (manager == null) {
            manager = new User();
            manager.setName("James Carter");
            manager.setEmail("manager@company.com");
            manager.setPassword("password");
            manager.setRole("MANAGER");
            manager.setPosition("Sales Director");
            manager.setSalary(15000.0);
            manager.setDateOfJoining(LocalDate.of(2025, 1, 15));
            manager = userRepository.save(manager);
        }

        // 4. Seed HR
        User hr = userRepository.findByEmail("hr@company.com");
        if (hr == null) {
            hr = new User();
            hr.setName("Alia Bhatt");
            hr.setEmail("hr@company.com");
            hr.setPassword("password");
            hr.setRole("HR");
            hr.setPosition("HR Lead");
            hr.setSalary(10000.0);
            hr.setDateOfJoining(LocalDate.of(2025, 6, 1));
            hr.setManager(manager);
            userRepository.save(hr);
        }

        // 5. Seed John Doe Executive
        User execJohn = userRepository.findByEmail("executive@company.com");
        if (execJohn == null) {
            execJohn = new User();
            execJohn.setName("John Doe");
            execJohn.setEmail("executive@company.com");
            execJohn.setPassword("password");
            execJohn.setRole("EXECUTIVE");
            execJohn.setPosition("Account Manager");
            execJohn.setSalary(8000.0);
            execJohn.setDateOfJoining(LocalDate.of(2026, 2, 15));
            execJohn.setManager(manager);
            execJohn = userRepository.save(execJohn);
        } else {
            if (execJohn.getManager() == null && manager != null) {
                execJohn.setManager(manager);
                execJohn = userRepository.save(execJohn);
            }
        }

        // 6. Seed Jane Smith Executive
        User execJane = userRepository.findByEmail("jane@company.com");
        if (execJane == null) {
            execJane = new User();
            execJane.setName("Jane Smith");
            execJane.setEmail("jane@company.com");
            execJane.setPassword("password");
            execJane.setRole("EXECUTIVE");
            execJane.setPosition("Account Representative");
            execJane.setSalary(7500.0);
            execJane.setDateOfJoining(LocalDate.of(2026, 4, 10));
            execJane.setManager(manager);
            execJane = userRepository.save(execJane);
        }

        // 7. Seed Leads if database is empty of essential leads
        long leadCount = leadRepository.count();
        if (leadCount <= 1) { // Database is empty or only has 1 lead
            // Lead 1: Saudi Aramco
            if (!leadRepository.existsByPhoneNumberOrEmail("+966 13 874 0000", "info@aramco.com.sa")) {
                Lead l = new Lead();
                l.setCustomerName("Saudi Aramco");
                l.setEmail("info@aramco.com.sa");
                l.setPhoneNumber("+966 13 874 0000");
                l.setSource("Website");
                l.setValue(450000.0);
                l.setStatus("New");
                l.setNotes("Interested in enterprise cloud migration.");
                l.setUser(execJohn);
                l.setFollowUpDate(LocalDate.now().plusDays(5));
                l.setFollowUpStatus("PENDING");
                l.setCollege("King Saud University");
                l.setPassoutYear(2026);
                l.setDepartment("Computer Science");
                leadRepository.save(l);
            }

            // Lead 2: NEOM Tech Division
            if (!leadRepository.existsByPhoneNumberOrEmail("+966 11 549 1111", "contact@neom.gov.sa")) {
                Lead l = new Lead();
                l.setCustomerName("NEOM Tech Division");
                l.setEmail("contact@neom.gov.sa");
                l.setPhoneNumber("+966 11 549 1111");
                l.setSource("Referral");
                l.setValue(1200000.0);
                l.setStatus("Interested to Buy");
                l.setNotes("Requested quotation for smart city integration.");
                l.setUser(execJohn);
                l.setFollowUpDate(LocalDate.now().plusDays(8));
                l.setFollowUpStatus("PENDING");
                l.setCollege("KAUST");
                l.setPassoutYear(2025);
                l.setDepartment("Electrical Engineering");
                leadRepository.save(l);
            }

            // Lead 3: Riyadh Bank
            if (!leadRepository.existsByPhoneNumberOrEmail("+966 11 401 3030", "procurement@riyadhbank.com")) {
                Lead l = new Lead();
                l.setCustomerName("Riyadh Bank");
                l.setEmail("procurement@riyadhbank.com");
                l.setPhoneNumber("+966 11 401 3030");
                l.setSource("Cold Call");
                l.setValue(350000.0);
                l.setStatus("Contacted");
                l.setNotes("Spoke with CFO. Follow up on Tuesday.");
                l.setUser(execJane);
                l.setFollowUpDate(LocalDate.now().plusDays(2));
                l.setFollowUpStatus("PENDING");
                l.setCollege("King Abdulaziz University");
                l.setPassoutYear(2026);
                l.setDepartment("Finance");
                leadRepository.save(l);
            }

            // Lead 4: SABIC Industrial
            if (!leadRepository.existsByPhoneNumberOrEmail("+966 11 225 1000", "sales@sabic.com")) {
                Lead l = new Lead();
                l.setCustomerName("SABIC Industrial");
                l.setEmail("sales@sabic.com");
                l.setPhoneNumber("+966 11 225 1000");
                l.setSource("LinkedIn");
                l.setValue(650000.0);
                l.setStatus("Closed Won");
                l.setNotes("Deal finalized and contract signed.");
                l.setUser(execJohn);
                l.setFollowUpDate(LocalDate.now().minusDays(5));
                l.setFollowUpStatus("COMPLETED");
                l.setCollege("King Fahd University");
                l.setPassoutYear(2024);
                l.setDepartment("Chemical Engineering");
                leadRepository.save(l);
            }

            // Lead 5: STC Telecom
            if (!leadRepository.existsByPhoneNumberOrEmail("+966 11 452 7000", "partner@stc.com.sa")) {
                Lead l = new Lead();
                l.setCustomerName("STC Telecom");
                l.setEmail("partner@stc.com.sa");
                l.setPhoneNumber("+966 11 452 7000");
                l.setSource("Website");
                l.setValue(500000.0);
                l.setStatus("Closed Lost");
                l.setNotes("Opted for internal solutions due to budget.");
                l.setUser(execJane);
                l.setFollowUpDate(LocalDate.now().minusDays(10));
                l.setFollowUpStatus("COMPLETED");
                l.setCollege("King Saud University");
                l.setPassoutYear(2025);
                l.setDepartment("Information Systems");
                leadRepository.save(l);
            }
        }
    }
}
