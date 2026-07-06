package sg.edu.nus.security;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/*
*   AUTHOR: Amelia
*   PURPOSE: In-memory store for revoked JWT access tokens
*/
@Service
public class TokenBlacklistService {

    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public void revokeToken(String token, Date expiryDate) {
        if (token == null || token.isBlank()) {
            return;
        }

        long expiresAt = expiryDate != null ? expiryDate.getTime() : System.currentTimeMillis();
        revokedTokens.put(token, expiresAt);
        cleanupExpiredTokens();
    }

    public boolean isTokenRevoked(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        cleanupExpiredTokens();
        Long expiresAt = revokedTokens.get(token);
        if (expiresAt == null) {
            return false;
        }

        if (expiresAt <= System.currentTimeMillis()) {
            revokedTokens.remove(token);
            return false;
        }

        return true;
    }

    private void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
