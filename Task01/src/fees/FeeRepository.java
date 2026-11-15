package fees;

import java.util.Optional;

/** Where payments are stored. */
public interface FeeRepository {
    void save(Payment p) throws PaymentFailedException;
    Optional<Payment> latestFor(String regNo) throws PaymentFailedException;
}
