package hu.konzultacio;

import hu.konzultacio.domain.User;
import hu.konzultacio.dto.Dtos.TokenResponse;
import hu.konzultacio.security.AuthUser;
import hu.konzultacio.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-0123456789abcdefghij";
    private static final String OTHER = "another-secret-0123456789abcdefghijk";

    private static User teacher() {
        User u = new User();
        u.id = 3L;
        u.email = "t@test.hu";
        u.fullName = "Teszt Oktató";
        u.role = User.Role.TEACHER;
        return u;
    }

    @Test
    void issuedTokenCanBeParsedBack() {
        JwtService jwt = new JwtService(SECRET, 5);
        AuthUser a = jwt.parse(jwt.issue(teacher()).token());
        assertEquals(3L, a.id());
        assertEquals("t@test.hu", a.email());
        assertEquals(User.Role.TEACHER, a.role());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 5);
        String token = jwt.issue(teacher()).token();
        assertThrows(JwtException.class, () -> jwt.parse(token + "x"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtService(OTHER, 5).issue(teacher()).token();
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 5).parse(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1);
        TokenResponse t = jwt.issue(teacher());
        assertThrows(ExpiredJwtException.class, () -> jwt.parse(t.token()));
    }
}
