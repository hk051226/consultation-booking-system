package hu.konzultacio;

import hu.konzultacio.domain.Booking;
import hu.konzultacio.domain.Slot;
import hu.konzultacio.dto.Dtos.SlotRequest;
import hu.konzultacio.dto.Dtos.SlotView;
import hu.konzultacio.exception.ApiException;
import hu.konzultacio.repository.BookingRepository;
import hu.konzultacio.repository.SlotRepository;
import hu.konzultacio.repository.UserRepository;
import hu.konzultacio.service.Notifier;
import hu.konzultacio.service.SlotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {
    @Mock SlotRepository slots;
    @Mock BookingRepository bookings;
    @Mock UserRepository users;
    @Mock Notifier notifier;
    @InjectMocks SlotService service;

    private static final long TEACHER = 9L;

    private static Instant tomorrow() { return Instant.now().plus(1, ChronoUnit.DAYS); }

    private static SlotRequest request(Instant start, Instant end, Integer capacity) {
        return new SlotRequest(start, end, "A101", capacity, null);
    }

    private static Slot ownedSlot(long owner) {
        Slot s = new Slot();
        s.id = 1L;
        s.teacherId = owner;
        s.startsAt = tomorrow();
        s.endsAt = s.startsAt.plus(30, ChronoUnit.MINUTES);
        return s;
    }

    private static void assertCode(String code, HttpStatus status, Runnable action) {
        ApiException e = assertThrows(ApiException.class, action::run);
        assertEquals(code, e.getCode());
        assertEquals(status, e.getStatus());
    }

    @Test
    void createdSlotDefaultsToCapacityOne() {
        when(slots.save(any(Slot.class))).thenAnswer(i -> i.getArgument(0));
        Instant start = tomorrow();
        SlotView v = service.create(TEACHER, request(start, start.plus(30, ChronoUnit.MINUTES), null));
        assertEquals(1, v.capacity());
        assertEquals(TEACHER, v.teacherId());
    }

    @Test
    void slotStartingInThePastIsRejected() {
        Instant start = Instant.now().minus(1, ChronoUnit.HOURS);
        assertCode("START_IN_PAST", HttpStatus.BAD_REQUEST,
                () -> service.create(TEACHER, request(start, start.plus(30, ChronoUnit.MINUTES), 1)));
    }

    @Test
    void endBeforeStartIsRejected() {
        Instant start = tomorrow();
        assertCode("INVALID_TIME_RANGE", HttpStatus.BAD_REQUEST,
                () -> service.create(TEACHER, request(start, start.minus(10, ChronoUnit.MINUTES), 1)));
    }

    @Test
    void overlappingSlotOfSameTeacherIsRejected() {
        when(slots.overlaps(eq(TEACHER), eq(Slot.Status.OPEN), any(), any(), eq(-1L))).thenReturn(true);
        Instant start = tomorrow();
        assertCode("SLOT_OVERLAP", HttpStatus.CONFLICT,
                () -> service.create(TEACHER, request(start, start.plus(30, ChronoUnit.MINUTES), 1)));
    }

    @Test
    void teacherCannotUpdateSomeoneElsesSlot() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(ownedSlot(100L)));
        Instant start = tomorrow();
        assertCode("NOT_OWNER", HttpStatus.FORBIDDEN,
                () -> service.update(TEACHER, 1L, request(start, start.plus(30, ChronoUnit.MINUTES), 1)));
    }

    @Test
    void capacityCannotDropBelowExistingBookings() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(ownedSlot(TEACHER)));
        when(bookings.countBySlotIdAndStatus(1L, Booking.Status.CONFIRMED)).thenReturn(2L);
        Instant start = tomorrow();
        assertCode("CAPACITY_BELOW_BOOKINGS", HttpStatus.CONFLICT,
                () -> service.update(TEACHER, 1L, request(start, start.plus(30, ChronoUnit.MINUTES), 1)));
    }

    @Test
    void cancellingSlotCancelsItsBookingsAndNotifiesStudents() {
        Slot s = ownedSlot(TEACHER);
        Booking b1 = booking(11L, 21L);
        Booking b2 = booking(12L, 22L);
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(s));
        when(bookings.findBySlotIdAndStatus(1L, Booking.Status.CONFIRMED)).thenReturn(List.of(b1, b2));

        service.cancel(TEACHER, 1L);

        assertEquals(Slot.Status.CANCELLED, s.status);
        assertEquals(Booking.Status.CANCELLED, b1.status);
        assertEquals(Booking.Status.CANCELLED, b2.status);
        verify(notifier, times(2)).send(any(), any(), any());
    }

    @Test
    void teacherCannotCancelSomeoneElsesSlot() {
        when(slots.findByIdForUpdate(1L)).thenReturn(Optional.of(ownedSlot(100L)));
        assertCode("NOT_OWNER", HttpStatus.FORBIDDEN, () -> service.cancel(TEACHER, 1L));
    }

    private static Booking booking(long id, long studentId) {
        Booking b = new Booking();
        b.id = id;
        b.slotId = 1L;
        b.studentId = studentId;
        return b;
    }
}
