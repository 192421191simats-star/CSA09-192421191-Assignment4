package fees;

/** Checked: the bank declined or the payment could not be recorded. Keeps the technical cause. */
public class PaymentFailedException extends Exception {
    public PaymentFailedException(String message, Throwable cause) { super(message, cause); }
}
