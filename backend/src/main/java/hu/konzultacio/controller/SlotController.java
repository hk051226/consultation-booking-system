package hu.konzultacio.controller;

import hu.konzultacio.dto.Dtos.*;
import hu.konzultacio.security.AuthUser;
import hu.konzultacio.service.BookingService;
import hu.konzultacio.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotController {
    private final SlotService slots;
    private final BookingService bookings;

    public SlotController(SlotService slots, BookingService bookings) { this.slots = slots; this.bookings = bookings; }

    @GetMapping
    public Page<SlotView> search(@RequestParam(required = false) Long teacherId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                 @RequestParam(defaultValue = "true") boolean onlyFree,
                                 Pageable pageable) {
        return slots.search(teacherId, from, to, onlyFree, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public List<SlotView> mine(@AuthenticationPrincipal AuthUser me) { return slots.mine(me.id()); }

    @GetMapping("/mine/bookings")
    @PreAuthorize("hasRole('TEACHER')")
    public List<TeacherBookingView> mineBookings(@AuthenticationPrincipal AuthUser me) {
        return slots.bookingsOfTeacher(me.id());
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public SlotView create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SlotRequest r) {
        return slots.create(me.id(), r);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public SlotView update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody SlotRequest r) {
        return slots.update(me.id(), id, r);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { slots.cancel(me.id(), id); }

    @PostMapping("/{id}/bookings")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingView book(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return bookings.book(id, me.id());
    }
}
