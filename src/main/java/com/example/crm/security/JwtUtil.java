package com.example.crm.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

public class JwtUtil {
    private static final String SECRET = "AntigravitySuperSecureSecretKey1234567890!"; // 256-bit Key

    public static String generateToken(String email, String role, String name) {
        try {
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            long exp = Instant.now().getEpochSecond() + (24 * 60 * 60); // 24 Hours validity
            String payload = String.format("{\"sub\":\"%s\",\"role\":\"%s\",\"name\":\"%s\",\"exp\":%d}", 
                email, role, name, exp);
            
            String encodedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(header.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            
            String signatureInput = encodedHeader + "." + encodedPayload;
            String signature = sign(signatureInput, SECRET);
            
            return signatureInput + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    public static boolean validateToken(String token) {
        try {
            if (token == null) return false;
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String signatureInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signatureInput, SECRET);
            if (!expectedSignature.equals(parts[2])) {
                return false;
            }

            // Expiry verification
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            if (payload.contains("\"exp\":")) {
                int expIdx = payload.indexOf("\"exp\":") + 6;
                int endIdx = payload.indexOf(",", expIdx);
                if (endIdx == -1) endIdx = payload.indexOf("}", expIdx);
                String expStr = payload.substring(expIdx, endIdx).trim();
                long exp = Long.parseLong(expStr);
                if (exp < Instant.now().getEpochSecond()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int subIdx = payload.indexOf("\"sub\":\"") + 7;
            int endIdx = payload.indexOf("\"", subIdx);
            return payload.substring(subIdx, endIdx);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRoleFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int roleIdx = payload.indexOf("\"role\":\"") + 8;
            int endIdx = payload.indexOf("\"", roleIdx);
            return payload.substring(roleIdx, endIdx);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sign(String input, String secret) throws Exception {
        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256HMAC.init(secretKey);
        byte[] hash = sha256HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
