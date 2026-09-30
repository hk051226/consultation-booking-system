package hu.konzultacio.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "slots")
public class Slot {
    public enum Status { OPEN, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "teacher_id", nullable = false) public Long teacherId;
    @Column(name = "starts_at", nullable = false) public Instant startsAt;
    @Column(name = "ends_at", nullable = false) public Instant endsAt;
    @Column(name = "location_or_link") public String locationOrLink;
    @Column(nullable = false) public int capacity = 1;
    public String note;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public Status status = Status.OPEN;
}
