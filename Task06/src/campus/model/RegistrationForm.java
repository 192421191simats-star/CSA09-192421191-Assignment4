package campus.model;

/** Raw values exactly as typed/selected in the form (eventId is null when no event is selected). */
public record RegistrationForm(String regNo, String studentName, String email, String dept, String year, Integer eventId) { }
