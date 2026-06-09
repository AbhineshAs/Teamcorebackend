package com.example.crm.config;

import com.example.crm.entity.User;
import com.example.crm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {

        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if ("admin@teamcore.com".equalsIgnoreCase(u.getEmail())) {
                if (u.getPassword() != null && u.getPassword().startsWith("data:image")) {
                    u.setEmail("admin_corrupted_" + u.getId() + "@teamcore.com");
                    u.setPassword("admin@123");
                    try {
                        userRepository.save(u);
                    } catch (Exception e) {
                        System.err.println("Could not rename corrupted admin: " + e.getMessage());
                    }
                }
            }

        }
    }
}
