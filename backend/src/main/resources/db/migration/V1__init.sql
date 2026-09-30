CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE slots (
    id               BIGSERIAL PRIMARY KEY,
    teacher_id       BIGINT       NOT NULL REFERENCES users(id),
    starts_at        TIMESTAMPTZ  NOT NULL,
    ends_at          TIMESTAMPTZ  NOT NULL,
    location_or_link VARCHAR(500),
    capacity         INT          NOT NULL DEFAULT 1 CHECK (capacity > 0),
    note             VARCHAR(1000),
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    CONSTRAINT slots_time_order CHECK (ends_at > starts_at)
);
CREATE INDEX idx_slots_teacher_start ON slots (teacher_id, starts_at);
CREATE INDEX idx_slots_start ON slots (starts_at);

CREATE TABLE bookings (
    id           BIGSERIAL PRIMARY KEY,
    slot_id      BIGINT      NOT NULL REFERENCES slots(id),
    student_id   BIGINT      NOT NULL REFERENCES users(id),
    status       VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ
);
-- Csak az aktív foglalásokra egyedi: lemondás után ugyanarra a slotra újra lehet foglalni.
CREATE UNIQUE INDEX ux_booking_active ON bookings (slot_id, student_id) WHERE status = 'CONFIRMED';
CREATE INDEX idx_bookings_student ON bookings (student_id);
CREATE INDEX idx_bookings_slot ON bookings (slot_id);
