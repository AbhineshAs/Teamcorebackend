package com.example.crm.contoller;

import com.example.crm.entity.ChatMessage;
import com.example.crm.entity.User;
import com.example.crm.repository.ChatMessageRepository;
import com.example.crm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatRestController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @RequestParam(required = false) Long user1,
            @RequestParam(required = false) Long user2,
            @RequestParam(required = false) String spaceName) {
        
        // Auto-cleanup messages older than 24 hours
        try {
            chatMessageRepository.deleteByTimestampBefore(LocalDateTime.now().minusHours(24));
        } catch (Exception e) {
            System.err.println("Cleanup of old chat messages failed: " + e.getMessage());
        }

        List<ChatMessage> all = chatMessageRepository.findAll();
        List<ChatMessage> result = new ArrayList<>();

        if (spaceName != null && !spaceName.trim().isEmpty()) {
            result = all.stream()
                    .filter(m -> spaceName.equalsIgnoreCase(m.getSpaceName()))
                    .sorted(Comparator.comparing(ChatMessage::getTimestamp))
                    .toList();
        } else if (user1 != null && user2 != null) {
            result = all.stream()
                    .filter(m -> m.getSpaceName() == null &&
                            m.getSender() != null && m.getReceiver() != null &&
                            ((m.getSender().getId().equals(user1) && m.getReceiver().getId().equals(user2)) ||
                             (m.getSender().getId().equals(user2) && m.getReceiver().getId().equals(user1))))
                    .sorted(Comparator.comparing(ChatMessage::getTimestamp))
                    .toList();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> payload) {
        // Auto-cleanup messages older than 24 hours
        try {
            chatMessageRepository.deleteByTimestampBefore(LocalDateTime.now().minusHours(24));
        } catch (Exception e) {
            System.err.println("Cleanup of old chat messages failed: " + e.getMessage());
        }

        Object senderIdObj = payload.get("senderId");
        Long senderId = senderIdObj != null ? Long.valueOf(senderIdObj.toString()) : null;
        String content = (String) payload.get("content");
        String spaceName = (String) payload.get("spaceName");
        
        Object receiverIdObj = payload.get("receiverId");
        Long receiverId = receiverIdObj != null ? Long.valueOf(receiverIdObj.toString()) : null;

        User sender = null;
        if (senderId != null) {
            sender = userRepository.findById(senderId).orElse(null);
        }
        if (sender == null && payload.get("senderEmail") != null) {
            String email = payload.get("senderEmail").toString().trim();
            sender = userRepository.findByEmailIgnoreCase(email);
        }
        if (sender == null && payload.get("senderEmail") != null) {
            sender = new User();
            sender.setEmail(payload.get("senderEmail").toString().trim());
            sender.setName(payload.get("senderName") != null ? payload.get("senderName").toString() : "Employee");
            sender.setPassword("Welcome@123");
            sender.setRole("EXECUTIVE");
            sender.setDateOfJoining(java.time.LocalDate.now());
            sender.setSalary(25000.0);
            sender = userRepository.save(sender);
        }
        if (sender == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sender not found in backend database"));
        }

        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        msg.setSpaceName(spaceName);

        if (spaceName == null || spaceName.trim().isEmpty()) {
            User receiver = null;
            if (receiverId != null) {
                receiver = userRepository.findById(receiverId).orElse(null);
            }
            if (receiver == null && payload.get("receiverEmail") != null) {
                String email = payload.get("receiverEmail").toString().trim();
                receiver = userRepository.findByEmailIgnoreCase(email);
            }
            if (receiver == null && payload.get("receiverEmail") != null) {
                receiver = new User();
                receiver.setEmail(payload.get("receiverEmail").toString().trim());
                receiver.setName(payload.get("receiverName") != null ? payload.get("receiverName").toString() : "Supervisor");
                receiver.setPassword("Welcome@123");
                receiver.setRole("EXECUTIVE");
                receiver.setDateOfJoining(java.time.LocalDate.now());
                receiver.setSalary(25000.0);
                receiver = userRepository.save(receiver);
            }
            if (receiver == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Receiver not found in backend database"));
            }
            msg.setReceiver(receiver);
        }

        ChatMessage saved = chatMessageRepository.save(msg);
        return ResponseEntity.ok(saved);
    }
}
