package com.example.crm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendUserCredentials(String toEmail, String name, String username, String password) {
        // Run in a background thread to prevent blocking user creation requests
        new Thread(() -> {
            if (mailSender == null) {
                System.out.println("JavaMailSender is not configured. Simulating credentials email sending to: " + toEmail);
                return;
            }
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                if (fromEmail != null && !fromEmail.trim().isEmpty()) {
                    helper.setFrom(fromEmail.trim(), "TEAMCORE");
                } else {
                    helper.setFrom("info@whitetracktech.com", "TEAMCORE");
                }
                helper.setTo(toEmail);
                helper.setSubject("Welcome to TEAMCORE - Your Account Credentials");

                String htmlMsg = "<div style='font-family: Inter, Arial, sans-serif; max-width: 600px; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden;'>" +
                        "  <div style='background: #6366f1; padding: 25px; text-align: center;'>" +
                        "    <h1 style='color: white; margin: 0; font-size: 24px; letter-spacing: -1px;'>TEAMCORE</h1>" +
                        "  </div>" +
                        "  <div style='padding: 30px; color: #1e293b; line-height: 1.6;'>" +
                        "    <h2 style='margin-top: 0; color: #6366f1;'>Welcome, " + name + "!</h2>" +
                        "    <p>Your team account has been successfully created. Here are your credentials to log in to the TEAMCORE platform:</p>" +
                        "    <div style='background: #f8fafc; border-radius: 12px; padding: 20px; margin: 20px 0; border: 1px solid #e2e8f0; font-size: 15px;'>" +
                        "      <strong>Login URL:</strong> <a href='http://localhost:5173' style='color: #6366f1; text-decoration: none;'>TEAMCORE Dashboard</a><br/>" +
                        "      <strong>Email/Username:</strong> <span style='font-family: monospace; font-weight: bold;'>" + username + "</span><br/>" +
                        "      <strong>Password:</strong> <span style='font-family: monospace; font-weight: bold;'>" + password + "</span>" +
                        "    </div>" +
                        "    <p style='font-size: 14px; color: #64748b;'>We recommend that you change your password after logging in for the first time.</p>" +
                        "    <hr style='border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;'/>" +
                        "    <p style='font-size: 12px; color: #94a3b8;'>This is an automated notification from the TEAMCORE CRM system.</p>" +
                        "  </div>" +
                        "</div>";

                helper.setText(htmlMsg, true);
                mailSender.send(message);
                System.out.println("Credentials email successfully sent to: " + toEmail);
            } catch (Exception e) {
                System.err.println("Failed to send credentials email to " + toEmail + ": " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}
