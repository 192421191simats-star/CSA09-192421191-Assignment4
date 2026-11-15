package fees;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A completed fee payment (also the data shown on a receipt). */
public record Payment(String receiptNo, String regNo, BigDecimal amount, String bankReference, LocalDateTime paidAt) {
    public String receiptText() {
        return "Receipt " + receiptNo + "\nRegister No: " + regNo + "\nAmount: Rs " + amount + "\nBank ref: " + bankReference
                + "\nPaid at: " + paidAt.withNano(0);
    }
}
