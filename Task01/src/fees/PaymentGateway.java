package fees;

import java.math.BigDecimal;

/** The bank. Slow (seconds) - must never be called on the event-dispatch thread. */
public interface PaymentGateway {
    /** Returns the bank reference, or throws if declined. */
    String charge(String regNo, BigDecimal amount) throws PaymentFailedException;
}
