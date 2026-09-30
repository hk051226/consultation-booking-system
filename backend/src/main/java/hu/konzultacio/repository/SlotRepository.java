package hu.konzultacio.repository;

import hu.konzultacio.domain.Slot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends JpaRepository<Slot, Long>, JpaSpecificationExecutor<Slot> {

    /** SELECT ... FOR UPDATE: a foglalás és a slot módosítása ezen a soron szerializálódik. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);

    List<Slot> findByTeacherIdOrderByStartsAt(Long teacherId);

    @Query("select count(s) > 0 from Slot s where s.teacherId = :teacherId and s.status = :status "
            + "and s.startsAt < :winEnd and s.endsAt > :winStart and s.id <> :excludeId")
    boolean overlaps(@Param("teacherId") Long teacherId, @Param("status") Slot.Status status,
                     @Param("winStart") Instant winStart, @Param("winEnd") Instant winEnd,
                     @Param("excludeId") Long excludeId);
}
