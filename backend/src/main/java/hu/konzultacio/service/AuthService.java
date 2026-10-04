package hu.konzultacio.service;

import hu.konzultacio.domain.User;
import hu.konzultacio.dto.Dtos.*;
import hu.konzultacio.exception.ApiException;
import hu.konzultacio.repository.UserRepository;
import hu.konzultacio.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    @Transactional
    public TokenResponse register(RegisterRequest r) {
        if (r.role() == User.Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_ALLOWED", "ADMIN szerepkörrel nem lehet regisztrálni.");
        }
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Ezzel az e-mail címmel már van fiók.");
        }
        User u = new User();
        u.email = email;
        u.passwordHash = encoder.encode(r.password());
        u.fullName = r.fullName().trim();
        u.role = r.role();
        return jwt.issue(users.save(u));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .filter(x -> encoder.matches(r.password(), x.passwordHash))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Hibás e-mail vagy jelszó."));
        return jwt.issue(u);
    }

    @Transactional(readOnly = true)
    public UserView me(Long id) {
        return users.findById(id).map(UserView::of)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_GONE", "A fiók már nem létezik."));
    }
}
