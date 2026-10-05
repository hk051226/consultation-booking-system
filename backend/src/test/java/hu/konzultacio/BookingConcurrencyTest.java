package hu.konzultacio;

import hu.konzultacio.domain.Booking;
import hu.konzultacio.domain.Slot;
import hu.konzultacio.domain.User;
import hu.konzultacio.exception.ApiException;
import hu.konzultacio.repository.BookingRepository;
import hu.konzultacio.repository.SlotRepository;
import hu.konzultacio.repository.UserRepository;
import hu.konzultacio.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingConcurrencyTest extends IntegrationTestBase {
    @Autowired UserRepository users;
    @Autowired SlotRepository slots;
    @Autowired BookingRepository bookings;
    @Autowired BookingService bookingService;

    @Test
    void onlyOneOfManyConcurrentBookingsSucceeds() throws Exception {
        User teacher = user("teacher", User.Role.TEACHER);
        Slot slot = new Slot();
        slot.teacherId = teacher.id;
        slot.startsAt = Instant.now().plus(1, ChronoUnit.DAYS);
        slot.endsAt = slot.startsAt.plus(30, ChronoUnit.MINUTES);
        slot.capacity = 1;
        slots.save(slot);

        int n = 12;
        List<User> students = IntStream.range(0, n).mapToObj(i -> user("student" + i, User.Role.STUDENT)).toList();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (User s : students) {
            results.add(pool.submit(() -> {
                go.await();
                try { bookingService.book(slot.id, s.id); return true; }
                catch (ApiException e) { return false; }
            }));
        }
        go.countDown();
        int ok = 0;
        for (Future<Boolean> f : results) if (f.get(30, TimeUnit.SECONDS)) ok++;
        pool.shutdown();

        assertEquals(1, ok, "pontosan egy foglalás sikerülhet");
        assertEquals(1, bookings.countBySlotIdAndStatus(slot.id, Booking.Status.CONFIRMED));
    }

    private User user(String prefix, User.Role role) {
        User u = new User();
        u.email = prefix + "-" + UUID.randomUUID() + "@test.hu";
        u.passwordHash = "x";
        u.fullName = prefix;
        u.role = role;
        return users.save(u);
    }
}
