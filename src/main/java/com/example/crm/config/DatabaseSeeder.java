package com.example.crm.config;

import com.example.crm.entity.User;
import com.example.crm.repository.UserRepository;
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
