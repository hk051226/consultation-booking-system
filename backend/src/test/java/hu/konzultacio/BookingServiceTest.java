package hu.konzultacio;

import hu.konzultacio.domain.Booking;
import hu.konzultacio.domain.Slot;
import hu.konzultacio.dto.Dtos.BookingView;
import hu.konzultacio.exception.ApiException;
import hu.konzultacio.repository.BookingRepository;
import hu.konzultacio.repository.SlotRepository;
import hu.konzultacio.service.BookingService;
import hu.konzultacio.service.Notifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Gyors, adatbázis nélküli tesztek a foglalási szabályokra (a párhuzamosságot a BookingConcurrencyTest fedi). */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock SlotRepository slots;
    @Mock BookingRepository bookings;
    @Mock Notifier notifier;
    @InjectMocks BookingService service;

    private static final long STUDENT = 5L;

    private Slot slot(Instant start, int capacity, Slot.Status status) {
        Slot s = new Slot();
        s.id = 1L;
        s.teacherId = 9L;
        s.startsAt = start;
        s.endsAt = start.plus(30, ChronoUnit.MINUTES);
        s.capacity = capacity;
        s.status = status;
        return s;
    }

    private Slot future(int capacity) {
        return slot(Instant.now().plus(1, ChronoUnit.DAYS), capacity, Slot.Status.OPEN);
    }

    private void assertCode(String code, HttpStatus status, Runnable action) {
        ApiException e = assertThrows(ApiException.class, action::run);
        assertEquals(code, e.getCode());
        assertEquals(status, e.getStatus());
    }

    @Test
    void bookingFreeSlotSucceedsAndNotifiesBothSides() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(future(1)));
        BookingView v = service.book(1L, STUDENT);
        assertEquals(Booking.Status.CONFIRMED, v.status());
        verify(bookings).save(any(Booking.class));
        verify(notifier, times(2)).send(any(), any(), any());
    }

    @Test
    void fullSlotIsRejected() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(future(1)));
        when(bookings.countBySlotIdAndStatus(1L, Booking.Status.CONFIRMED)).thenReturn(1L);
        assertCode("SLOT_FULL", HttpStatus.CONFLICT, () -> service.book(1L, STUDENT));
    }

    @Test
    void doubleBookingBySameStudentIsRejected() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(future(3)));
        when(bookings.existsBySlotIdAndStudentIdAndStatus(1L, STUDENT, Booking.Status.CONFIRMED)).thenReturn(true);
        assertCode("ALREADY_BOOKED", HttpStatus.CONFLICT, () -> service.book(1L, STUDENT));
    }

    @Test
    void pastSlotCannotBeBooked() {
        Slot past = slot(Instant.now().minus(1, ChronoUnit.HOURS), 1, Slot.Status.OPEN);
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(past));
        assertCode("SLOT_IN_PAST", HttpStatus.BAD_REQUEST, () -> service.book(1L, STUDENT));
    }

    @Test
    void cancelledSlotCannotBeBooked() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(
                slot(Instant.now().plus(1, ChronoUnit.DAYS), 1, Slot.Status.CANCELLED)));
        assertCode("SLOT_CANCELLED", HttpStatus.CONFLICT, () -> service.book(1L, STUDENT));
    }

    @Test
    void overlappingBookingOfSameStudentIsRejected() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(future(1)));
        when(bookings.studentHasOverlap(eq(STUDENT), eq(Booking.Status.CONFIRMED), any(), any())).thenReturn(true);
        assertCode("STUDENT_OVERLAP", HttpStatus.CONFLICT, () -> service.book(1L, STUDENT));
    }

    @Test
    void studentCannotCancelSomeoneElsesBooking() {
        Booking b = new Booking();
        b.id = 7L;
        b.slotId = 1L;
        b.studentId = 99L;
        when(bookings.findById(7L)).thenReturn(Optional.of(b));
        assertCode("NOT_OWNER", HttpStatus.FORBIDDEN, () -> service.cancel(7L, STUDENT));
    }

    @Test
    void cancellingOwnFutureBookingWorks() {
        Booking b = new Booking();
        b.id = 7L;
        b.slotId = 1L;
        b.studentId = STUDENT;
        when(bookings.findById(7L)).thenReturn(Optional.of(b));
        when(slots.findById(1L)).thenReturn(Optional.of(future(1)));
        service.cancel(7L, STUDENT);
        assertEquals(Booking.Status.CANCELLED, b.status);
        verify(notifier, times(2)).send(any(), any(), any());
    }

    @Test
    void pastBookingCannotBeCancelled() {
        Booking b = new Booking();
        b.id = 7L;
        b.slotId = 1L;
        b.studentId = STUDENT;
        when(bookings.findById(7L)).thenReturn(Optional.of(b));
        when(slots.findById(1L)).thenReturn(Optional.of(
                slot(Instant.now().minus(1, ChronoUnit.HOURS), 1, Slot.Status.OPEN)));
        assertCode("SLOT_IN_PAST", HttpStatus.BAD_REQUEST, () -> service.cancel(7L, STUDENT));
    }
}
