package hu.konzultacio.repository;

import hu.konzultacio.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    long countBySlotIdAndStatus(Long slotId, Booking.Status status);
    boolean existsBySlotIdAndStudentIdAndStatus(Long slotId, Long studentId, Booking.Status status);
    List<Booking> findBySlotIdAndStatus(Long slotId, Booking.Status status);
    List<Booking> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("select count(b) > 0 from Booking b, Slot s where b.slotId = s.id and b.studentId = :studentId "
            + "and b.status = :status and s.startsAt < :winEnd and s.endsAt > :winStart")
    boolean studentHasOverlap(@Param("studentId") Long studentId, @Param("status") Booking.Status status,
                              @Param("winStart") Instant winStart, @Param("winEnd") Instant winEnd);
}
