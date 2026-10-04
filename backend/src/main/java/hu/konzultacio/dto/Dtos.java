package hu.konzultacio.dto;

import hu.konzultacio.domain.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @NotBlank @Size(max = 255) String fullName,
                                  @NotNull User.Role role) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserView(Long id, String email, String fullName, User.Role role) {
        public static UserView of(User u) { return new UserView(u.id, u.email, u.fullName, u.role); }
    }

    public record TokenResponse(String token, String tokenType, long expiresInSeconds, UserView user) {}

    public record SlotRequest(@NotNull Instant startsAt, @NotNull Instant endsAt,
                              @Size(max = 500) String locationOrLink,
                              @Min(1) @Max(100) Integer capacity,
                              @Size(max = 1000) String note) {}

    public record SlotView(Long id, Long teacherId, String teacherName, Instant startsAt, Instant endsAt,
                           String locationOrLink, int capacity, long booked, String note, Slot.Status status) {}

    public record BookingView(Long id, Long slotId, Long teacherId, Instant startsAt, Instant endsAt,
                              String locationOrLink, Booking.Status status, Instant createdAt) {}

    public record TeacherBookingView(Long bookingId, Long slotId, Instant startsAt, Instant endsAt,
                                     Long studentId, String studentName, String studentEmail) {}

    public record TeacherView(Long id, String fullName) {}
}
