package campus.model;

import java.time.LocalDateTime;

/** One student's registration for one event (eventName is filled when read with a JOIN). */
public record Registration(long id, String regNo, String studentName, String email, String dept, int year,
                           int eventId, String eventName, LocalDateTime registeredAt) {
    public Registration withId(long newId) {
        return new Registration(newId, regNo, studentName, email, dept, year, eventId, eventName, registeredAt);
    }
}
