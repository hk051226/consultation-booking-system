package hu.konzultacio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    public enum Role { STUDENT, TEACHER, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true) public String email;
    @Column(name = "password_hash", nullable = false) public String passwordHash;
    @Column(name = "full_name", nullable = false) public String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Role role;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
}
