package fees;

/** Checked: invalid input the user can correct. The message is safe to show in the window. */
public class FeeValidationException extends Exception {
    public FeeValidationException(String message) { super(message); }
}
