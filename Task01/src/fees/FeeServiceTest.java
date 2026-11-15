package fees;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Task 1.3 evidence: the rules are tested WITHOUT any window, bank delay or database. */
public class FeeServiceTest {
    static final List<String[]> rows = new ArrayList<>();
    static void check(String id, String what, String expected, String actual) {
        rows.add(new String[]{id, what, expected, actual, expected.equals(actual) ? "PASS" : "FAIL"});
    }
    static String attempt(ThrowingCall c) {
        try { return c.call(); } catch (Exception e) { return e.getClass().getSimpleName() + ": " + e.getMessage(); }
    }
    interface ThrowingCall { String call() throws Exception; }

    public static void main(String[] args) {
        InMemoryFeeRepository repo = new InMemoryFeeRepository();
        PaymentGateway instantBank = (r, a) -> "TESTREF";             // no 3-second wait in tests
        FeeService s = new FeeService(instantBank, repo);
        check("F1", "valid payment", "Rs 45000.50 TESTREF", attempt(() -> { Payment p = s.pay("192311001", "45000.50"); return "Rs " + p.amount() + " " + p.bankReference(); }));
        check("F2", "empty register number", "FeeValidationException: Enter the register number.", attempt(() -> s.pay("", "100").toString()));
        check("F3", "letters in amount", "FeeValidationException: Amount must be a number, e.g. 45000 or 45000.50.", attempt(() -> s.pay("192311001", "45k").toString()));
        check("F4", "negative amount", "FeeValidationException: Amount must be greater than zero.", attempt(() -> s.pay("192311001", "-500").toString()));
        check("F5", "3 decimal places", "FeeValidationException: Amount can have at most 2 decimal places.", attempt(() -> s.pay("192311001", "10.005").toString()));
        check("F6", "over the limit", "FeeValidationException: Amount cannot exceed Rs 2,00,000 in one payment.", attempt(() -> s.pay("192311001", "250000").toString()));
        check("F7", "SQL injection text as register number", "FeeValidationException: Register number must be 9 digits.",
                attempt(() -> s.pay("1'); DROP TABLE fees; --", "100").toString()));
        check("F8", "receipt after payment", "RCPT-24001", attempt(() -> s.receiptFor("192311001").receiptNo()));
        check("F9", "receipt with no payment", "FeeValidationException: No payment found for 192311999.", attempt(() -> s.receiptFor("192311999").receiptNo()));
        FeeService declining = new FeeService(new SimulatedBankGateway(0), repo);
        check("F10", "bank declines (over Rs 1,50,000)", "PaymentFailedException: Bank declined the payment (limit Rs 1,50,000)", attempt(() -> declining.pay("192311002", "160000").toString()));
        check("F11", "nothing stored for rejected payments", "1", String.valueOf(repo.size()));
        check("F12", "amount kept exactly (BigDecimal, no double rounding)", "0.30", attempt(() -> new BigDecimal("0.10").add(s.validate("192311001", "0.20")).toString()));

        System.out.printf("%-4s %-40s %-62s %s%n", "ID", "test", "actual", "result");
        int pass = 0;
        for (String[] r : rows) { System.out.printf("%-4s %-40s %-62s %s%n", r[0], r[1], r[3], r[4]); if (r[4].equals("PASS")) pass++; }
        System.out.println(pass + " / " + rows.size() + " FeeService tests passed (no window, no database, no 3-second wait)");
    }
}
