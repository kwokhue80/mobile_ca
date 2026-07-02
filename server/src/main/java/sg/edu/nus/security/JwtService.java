package sg.edu.nus.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import sg.edu.nus.features.user.account.User;

/*
*   AUTHOR: Amelia
*   PURPOSE: JWT related functions
*/
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    // Generate token
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .subject(user.getEmailAddress())
            .claim("userId", user.getId().toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    // Authenticate token
    public Claims authenticateToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    // Unused for current flow
    // public String extractEmail(String token) {
    //     return extractClaims(token).getSubject();
    // }

    // public String extractUserId(String token) {
    //     return extractClaims(token).get("userId", String.class);
    // }

    // public boolean isTokenValid(String token, User user) {
    //     String email = extractEmail(token);
    //     return email.equals(user.getEmailAddress()) && !isTokenExpired(token);
    // }

    // private boolean isTokenExpired(String token) {
    //     Date expiration = extractClaims(token).getExpiration();
    //     return expiration.before(new Date());
    // }

    // private Claims extractClaims(String token) {
    //     return Jwts.parser()
    //         .verifyWith(getSigningKey())
    //         .build()
    //         .parseSignedClaims(token)
    //         .getPayload();
    // }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
