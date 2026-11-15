package campus.dao;

/** Checked wrapper for SQLException so the layers above do not depend on java.sql. */
public class DataAccessException extends Exception {
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
