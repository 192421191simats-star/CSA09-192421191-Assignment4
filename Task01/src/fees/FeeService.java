package fees;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/** All fee rules live here - no AWT/Swing types, so it can be unit-tested without a window. */
public class FeeService {
    public static final BigDecimal MAX = new BigDecimal("200000");
    private final PaymentGateway bank;
    private final FeeRepository repo;
    private int receiptSeq = 24_001;

    public FeeService(PaymentGateway bank, FeeRepository repo) { this.bank = bank; this.repo = repo; }

    /** Fast checks only - safe to call on the event-dispatch thread. Returns the parsed amount. */
    public BigDecimal validate(String regNo, String amountText) throws FeeValidationException {
        String r = regNo == null ? "" : regNo.trim();
        if (r.isEmpty()) throw new FeeValidationException("Enter the register number.");
        if (!r.matches("\\d{9}")) throw new FeeValidationException("Register number must be 9 digits.");
        String a = amountText == null ? "" : amountText.trim();
        if (a.isEmpty()) throw new FeeValidationException("Enter the amount.");
        BigDecimal amt;
        try { amt = new BigDecimal(a); } catch (NumberFormatException e) { throw new FeeValidationException("Amount must be a number, e.g. 45000 or 45000.50."); }
        if (amt.signum() <= 0) throw new FeeValidationException("Amount must be greater than zero.");
        if (amt.scale() > 2) throw new FeeValidationException("Amount can have at most 2 decimal places.");
        if (amt.compareTo(MAX) > 0) throw new FeeValidationException("Amount cannot exceed Rs 2,00,000 in one payment.");
        return amt;
    }

    /** Slow (bank + database) - call from a background thread. */
    public Payment pay(String regNo, String amountText) throws FeeValidationException, PaymentFailedException {
        BigDecimal amt = validate(regNo, amountText);
        String bankRef = bank.charge(regNo.trim(), amt);
        Payment p;
        synchronized (this) { p = new Payment("RCPT-" + receiptSeq++, regNo.trim(), amt, bankRef, LocalDateTime.now()); }
        repo.save(p);
        return p;
    }

    public Payment receiptFor(String regNo) throws FeeValidationException, PaymentFailedException {
        String r = regNo == null ? "" : regNo.trim();
        if (!r.matches("\\d{9}")) throw new FeeValidationException("Enter a valid 9-digit register number to view the receipt.");
        Optional<Payment> p = repo.latestFor(r);
        return p.orElseThrow(() -> new FeeValidationException("No payment found for " + r + "."));
    }
}
