package hu.konzultacio.controller;

import hu.konzultacio.dto.Dtos.BookingView;
import hu.konzultacio.security.AuthUser;
import hu.konzultacio.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookings;

    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<BookingView> mine(@AuthenticationPrincipal AuthUser me) { return bookings.mine(me.id()); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { bookings.cancel(id, me.id()); }
}
