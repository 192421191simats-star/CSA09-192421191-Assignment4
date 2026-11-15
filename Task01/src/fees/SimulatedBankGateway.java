package fees;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/** Stand-in for the bank: waits for "confirmation" and declines amounts over Rs 1,50,000. */
public class SimulatedBankGateway implements PaymentGateway {
    private final long delayMs;
    private final AtomicInteger ref = new AtomicInteger(880_100);
    public SimulatedBankGateway(long delayMs) { this.delayMs = delayMs; }

    @Override public String charge(String regNo, BigDecimal amount) throws PaymentFailedException {
        try { Thread.sleep(delayMs); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new PaymentFailedException("Payment interrupted", e); }
        if (amount.compareTo(new BigDecimal("150000")) > 0) throw new PaymentFailedException("Bank declined the payment (limit Rs 1,50,000)", null);
        return "BNK" + ref.incrementAndGet();
    }
}
