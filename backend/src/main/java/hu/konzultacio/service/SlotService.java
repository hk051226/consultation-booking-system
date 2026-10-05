package hu.konzultacio.service;

import hu.konzultacio.domain.Booking;
import hu.konzultacio.domain.Slot;
import hu.konzultacio.domain.User;
import hu.konzultacio.dto.Dtos.*;
import hu.konzultacio.exception.ApiException;
import hu.konzultacio.repository.BookingRepository;
import hu.konzultacio.repository.SlotRepository;
import hu.konzultacio.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlotService {
    private final SlotRepository slots;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final Notifier notifier;

    public SlotService(SlotRepository slots, BookingRepository bookings, UserRepository users, Notifier notifier) {
        this.slots = slots; this.bookings = bookings; this.users = users; this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public Page<SlotView> search(Long teacherId, Instant from, Instant to, boolean onlyFree, Pageable pageable) {
        Specification<Slot> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("status"), Slot.Status.OPEN));
            p.add(cb.greaterThan(root.<Instant>get("startsAt"), Instant.now()));
            if (teacherId != null) p.add(cb.equal(root.get("teacherId"), teacherId));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.<Instant>get("startsAt"), from));
            if (to != null) p.add(cb.lessThan(root.<Instant>get("startsAt"), to));
            if (onlyFree) {
                Subquery<Long> sq = q.subquery(Long.class);
                Root<Booking> b = sq.from(Booking.class);
                sq.select(cb.count(b)).where(
                        cb.equal(b.get("slotId"), root.get("id")),
                        cb.equal(b.get("status"), Booking.Status.CONFIRMED));
                p.add(cb.lt(sq, root.<Integer>get("capacity")));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        Pageable page = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("startsAt"));
        return slots.findAll(spec, page).map(this::view);
    }

    @Transactional(readOnly = true)
    public List<SlotView> mine(Long teacherId) {
        return slots.findByTeacherIdOrderByStartsAt(teacherId).stream().map(this::view).toList();
    }

    @Transactional
    public SlotView create(Long teacherId, SlotRequest r) {
        validate(r, teacherId, -1L);
        Slot s = new Slot();
        s.teacherId = teacherId;
        apply(s, r);
        return view(slots.save(s));
    }

    @Transactional
    public SlotView update(Long teacherId, Long id, SlotRequest r) {
        Slot s = ownedForUpdate(teacherId, id);
        if (s.status == Slot.Status.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_CANCELLED", "A törölt időpont nem módosítható.");
        }
        validate(r, teacherId, s.id);
        int cap = r.capacity() == null ? 1 : r.capacity();
        if (cap < bookings.countBySlotIdAndStatus(s.id, Booking.Status.CONFIRMED)) {
            throw new ApiException(HttpStatus.CONFLICT, "CAPACITY_BELOW_BOOKINGS", "A kapacitás kisebb, mint a meglévő foglalások száma.");
        }
        apply(s, r);
        return view(s);
    }

    /** "Törlés" = státuszváltás CANCELLED-re; a meglévő foglalások is lemondódnak, a hallgatók e-mailt kapnak. */
    @Transactional
    public void cancel(Long teacherId, Long id) {
        Slot s = ownedForUpdate(teacherId, id);
        if (s.status == Slot.Status.CANCELLED) return;
        s.status = Slot.Status.CANCELLED;
        for (Booking b : bookings.findBySlotIdAndStatus(s.id, Booking.Status.CONFIRMED)) {
            b.status = Booking.Status.CANCELLED;
            b.cancelledAt = Instant.now();
            Long studentId = b.studentId;
            String when = Notifier.fmt(s.startsAt);
            Tx.afterCommit(() -> notifier.send(studentId, "Konzultáció törölve",
                    "Az oktató törölte a(z) " + when + " időpontú konzultációt, a foglalásod megszűnt."));
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherBookingView> bookingsOfTeacher(Long teacherId) {
        List<TeacherBookingView> out = new ArrayList<>();
        for (Slot s : slots.findByTeacherIdOrderByStartsAt(teacherId)) {
            for (Booking b : bookings.findBySlotIdAndStatus(s.id, Booking.Status.CONFIRMED)) {
                User st = users.findById(b.studentId).orElseThrow();
                out.add(new TeacherBookingView(b.id, s.id, s.startsAt, s.endsAt, st.id, st.fullName, st.email));
            }
        }
        return out;
    }

    private Slot ownedForUpdate(Long teacherId, Long id) {
        Slot s = slots.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SLOT_NOT_FOUND", "Nincs ilyen időpont."));
        if (!s.teacherId.equals(teacherId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "Csak a saját időpontodat módosíthatod.");
        }
        return s;
    }

    private void validate(SlotRequest r, Long teacherId, Long excludeId) {
        if (!r.startsAt().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "START_IN_PAST", "Az időpont kezdete legyen a jövőben.");
        }
        if (!r.endsAt().isAfter(r.startsAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE", "A végének a kezdet után kell lennie.");
        }
        if (slots.overlaps(teacherId, Slot.Status.OPEN, r.startsAt(), r.endsAt(), excludeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_OVERLAP", "Átfedés egy másik időpontoddal.");
        }
    }

    private void apply(Slot s, SlotRequest r) {
        s.startsAt = r.startsAt();
        s.endsAt = r.endsAt();
        s.locationOrLink = r.locationOrLink();
        s.capacity = r.capacity() == null ? 1 : r.capacity();
        s.note = r.note();
    }

    private SlotView view(Slot s) {
        String teacher = users.findById(s.teacherId).map(u -> u.fullName).orElse("?");
        long booked = bookings.countBySlotIdAndStatus(s.id, Booking.Status.CONFIRMED);
        return new SlotView(s.id, s.teacherId, teacher, s.startsAt, s.endsAt, s.locationOrLink,
                s.capacity, booked, s.note, s.status);
    }
}
