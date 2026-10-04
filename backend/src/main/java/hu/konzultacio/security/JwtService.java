package hu.konzultacio.security;

import hu.konzultacio.domain.User;
import hu.konzultacio.dto.Dtos.TokenResponse;
import hu.konzultacio.dto.Dtos.UserView;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // legalább 32 bájt kell (HS256)
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public TokenResponse issue(User u) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(String.valueOf(u.id))
                .claim("email", u.email)
                .claim("role", u.role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
        return new TokenResponse(token, "Bearer", ttl.toSeconds(), UserView.of(u));
    }

    /** Érvénytelen/lejárt tokennél JwtException-t dob. */
    public AuthUser parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthUser(Long.valueOf(c.getSubject()), c.get("email", String.class),
                User.Role.valueOf(c.get("role", String.class)));
    }
}
