package campus.model;

import java.time.LocalDate;

/** A cultural event students can register for. */
public record Event(int id, String name, LocalDate date, String venue, int capacity) {
    @Override public String toString() { return name + " (" + date + ")"; }
}
