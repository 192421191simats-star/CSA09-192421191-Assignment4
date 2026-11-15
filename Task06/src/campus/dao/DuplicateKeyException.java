package campus.dao;

/** The database rejected a row because of the UNIQUE (reg_no, event_id) key. */
public class DuplicateKeyException extends DataAccessException {
    public DuplicateKeyException(String message, Throwable cause) { super(message, cause); }
}
