package campus.service;

/** Rule R7: a student may register only once for an event. */
public class DuplicateRegistrationException extends Exception {
    public DuplicateRegistrationException(String message) { super(message); }
}
