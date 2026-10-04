package hu.konzultacio.controller;

import hu.konzultacio.dto.Dtos.*;
import hu.konzultacio.security.AuthUser;
import hu.konzultacio.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest r) { return auth.register(r); }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal AuthUser me) { return auth.me(me.id()); }
}
