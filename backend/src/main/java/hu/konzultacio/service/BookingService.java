package hu.konzultacio.service;

import hu.konzultacio.domain.Booking;
import hu.konzultacio.domain.Slot;
import hu.konzultacio.dto.Dtos.BookingView;
import hu.konzultacio.exception.ApiException;
import hu.konzultacio.repository.BookingRepository;
import hu.konzultacio.repository.SlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class BookingService {
    private final SlotRepository slots;
    private final BookingRepository bookings;
    private final Notifier notifier;

    public BookingService(SlotRepository slots, BookingRepository bookings, Notifier notifier) {
        this.slots = slots; this.bookings = bookings; this.notifier = notifier;
    }

    /**
     * A slot sorát PESSIMISTIC_WRITE zárral (SELECT ... FOR UPDATE) olvassuk, így az azonos slotra
     * érkező párhuzamos foglalások sorban futnak, és a kapacitás-ellenőrzés mindig friss adatot lát.
     */
    @Transactional
    public BookingView book(Long slotId, Long studentId) {
        Slot s = slots.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SLOT_NOT_FOUND", "Nincs ilyen időpont."));
        if (s.status != Slot.Status.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_CANCELLED", "Ez az időpont törölve lett.");
        }
        if (!s.startsAt.isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SLOT_IN_PAST", "Múltbeli időpont nem foglalható.");
        }
        if (bookings.existsBySlotIdAndStudentIdAndStatus(slotId, studentId, Booking.Status.CONFIRMED)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_BOOKED", "Erre az időpontra már foglaltál.");
        }
        if (bookings.countBySlotIdAndStatus(slotId, Booking.Status.CONFIRMED) >= s.capacity) {
            throw new ApiException(HttpStatus.CONFLICT, "SLOT_FULL", "Az időpont betelt.");
        }
        if (bookings.studentHasOverlap(studentId, Booking.Status.CONFIRMED, s.startsAt, s.endsAt)) {
            throw new ApiException(HttpStatus.CONFLICT, "STUDENT_OVERLAP", "Van egy időben átfedő foglalásod.");
        }
        Booking b = new Booking();
        b.slotId = slotId;
        b.studentId = studentId;
        bookings.save(b);

        String when = Notifier.fmt(s.startsAt);
        Long teacherId = s.teacherId;
        Tx.afterCommit(() -> {
            notifier.send(studentId, "Foglalás megerősítve", "Sikeres foglalás: " + when + ".");
            notifier.send(teacherId, "Új foglalás", "Egy hallgató lefoglalta a(z) " + when + " időpontodat.");
        });
        return view(b, s);
    }

    @Transactional
    public void cancel(Long bookingId, Long studentId) {
        Booking b = bookings.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Nincs ilyen foglalás."));
        if (!b.studentId.equals(studentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "Csak a saját foglalásodat mondhatod le.");
        }
        if (b.status == Booking.Status.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_CANCELLED", "A foglalás már le van mondva.");
        }
        Slot s = slots.findById(b.slotId).orElseThrow();
        if (!s.startsAt.isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SLOT_IN_PAST", "Múltbeli foglalás nem mondható le.");
        }
        b.status = Booking.Status.CANCELLED;
        b.cancelledAt = Instant.now();

        String when = Notifier.fmt(s.startsAt);
        Long teacherId = s.teacherId;
        Tx.afterCommit(() -> {
            notifier.send(studentId, "Foglalás lemondva", "A(z) " + when + " időpontú foglalásod lemondtad.");
            notifier.send(teacherId, "Foglalás lemondva", "Egy hallgató lemondta a(z) " + when + " időpontot.");
        });
    }

    @Transactional(readOnly = true)
    public List<BookingView> mine(Long studentId) {
        return bookings.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(b -> view(b, slots.findById(b.slotId).orElseThrow())).toList();
    }

    private BookingView view(Booking b, Slot s) {
        return new BookingView(b.id, s.id, s.teacherId, s.startsAt, s.endsAt, s.locationOrLink, b.status, b.createdAt);
    }
}
