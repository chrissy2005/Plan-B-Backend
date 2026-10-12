package org.planb.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.planb.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service 
public class JwtService {
    
    private final SecretKey signingKey;
    private final long expirationMilliseconds;
    
    public JwtService(@Value("${security.jwt.secret}") String based64Secret, @Value("${security.jwt.expiration-ms}") long expirationMilliseconds) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(based64Secret));
        this.expirationMilliseconds = expirationMilliseconds;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMilliseconds)))
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getPayload().getSubject();
    }

    public Long extractUserId(String token) {
        Object userId = getClaims(token).getPayload().get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        try {
            Claims claims = getClaims(token).getPayload();
            String tokenEmail = claims.getSubject();
            Date expiration = claims.getExpiration();
            return tokenEmail.equals(expectedEmail) && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private Jws<Claims> getClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
    }

}
