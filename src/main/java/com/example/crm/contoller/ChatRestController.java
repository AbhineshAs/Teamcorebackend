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
                            ((m.getSender().getId().equals(user1) && m.getReceiver().getId().equals(user2)) ||
                             (m.getSender().getId().equals(user2) && m.getReceiver().getId().equals(user1))))
                    .sorted(Comparator.comparing(ChatMessage::getTimestamp))
                    .toList();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> payload) {
        Long senderId = Long.valueOf(payload.get("senderId").toString());
        String content = (String) payload.get("content");
        String spaceName = (String) payload.get("spaceName");
        Long receiverId = payload.get("receiverId") != null ? Long.valueOf(payload.get("receiverId").toString()) : null;

        Optional<User> senderOpt = userRepository.findById(senderId);
        if (senderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sender not found"));
        }

        ChatMessage msg = new ChatMessage();
        msg.setSender(senderOpt.get());
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        msg.setSpaceName(spaceName);

        if (spaceName == null || spaceName.trim().isEmpty()) {
            if (receiverId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Receiver ID is required for direct messages"));
            }
            Optional<User> receiverOpt = userRepository.findById(receiverId);
            if (receiverOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Receiver not found"));
            }
            msg.setReceiver(receiverOpt.get());
        }

        ChatMessage saved = chatMessageRepository.save(msg);
        return ResponseEntity.ok(saved);
    }
}
