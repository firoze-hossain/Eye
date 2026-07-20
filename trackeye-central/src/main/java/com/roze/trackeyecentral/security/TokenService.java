package com.roze.trackeyecentral.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Issues and validates signed session tokens for dashboard users
 * (admin / supervisor / employee logging into the web app).
 *
 * This replaces the old approach where the "token" was just
 * AES-ECB(userId:timestamp) with no signature and no real verification.
 *
 * Format (JWT-like, self-contained, no external library needed):
 *     base64url(payloadJson) + "." + base64url(HMAC-SHA256(secret, payloadB64))
 *
 * The signature means the token can't be forged or tampered with without the
 * server secret. For very high security you could swap this for the jjwt
 * library, but this is correct and dependency-free.
 */
@Slf4j
@Service
public class TokenService {

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${trackeye.security.jwt-secret:change-me-in-production}")
    private String secret;

    @Value("${trackeye.security.token-validity-hours:12}")
    private long validityHours;

    public String issue(Long userId, Long organizationId, String role) {
        try {
            long exp = System.currentTimeMillis() + validityHours * 60 * 60 * 1000;
            Claims claims = new Claims();
            claims.setUid(userId);
            claims.setOid(organizationId);
            claims.setRole(role);
            claims.setExp(exp);

            String payloadB64 = base64Url(mapper.writeValueAsBytes(claims));
            String sig = sign(payloadB64);
            return payloadB64 + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException("Failed to issue token", e);
        }
    }

    /** Returns the claims if the token is valid and not expired, otherwise null. */
    public Claims verify(String token) {
        try {
            if (token == null) return null;
            if (token.startsWith("Bearer ")) token = token.substring(7).trim();

            String[] parts = token.split("\\.");
            if (parts.length != 2) return null;

            String expectedSig = sign(parts[0]);
            // constant-time comparison
            if (!constantTimeEquals(expectedSig, parts[1])) {
                log.warn("Token signature mismatch");
                return null;
            }

            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            Claims claims = mapper.readValue(payload, Claims.class);

            if (claims.getExp() < System.currentTimeMillis()) {
                log.debug("Token expired");
                return null;
            }
            return claims;
        } catch (Exception e) {
            log.warn("Token verification failed: {}", e.getMessage());
            return null;
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64Url(raw);
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) result |= a.charAt(i) ^ b.charAt(i);
        return result == 0;
    }

    @Data
    public static class Claims {
        private Long uid;
        private Long oid;
        private String role;
        private long exp;
    }
}
