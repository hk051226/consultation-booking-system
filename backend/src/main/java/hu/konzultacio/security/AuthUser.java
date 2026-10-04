package hu.konzultacio.security;

import hu.konzultacio.domain.User;

public record AuthUser(Long id, String email, User.Role role) {}
