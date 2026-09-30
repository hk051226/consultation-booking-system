package hu.konzultacio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {
    public enum Status { CONFIRMED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "slot_id", nullable = false) public Long slotId;
    @Column(name = "student_id", nullable = false) public Long studentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Status status = Status.CONFIRMED;
    @Column(name = "created_at", nullable = false) public Instant createdAt = Instant.now();
    @Column(name = "cancelled_at") public Instant cancelledAt;
}
