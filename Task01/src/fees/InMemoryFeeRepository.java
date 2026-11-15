package fees;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Used by the tests and the offline demo. */
public class InMemoryFeeRepository implements FeeRepository {
    private final List<Payment> rows = new ArrayList<>();
    @Override public synchronized void save(Payment p) { rows.add(p); }
    @Override public synchronized Optional<Payment> latestFor(String regNo) {
        for (int i = rows.size() - 1; i >= 0; i--) if (rows.get(i).regNo().equals(regNo)) return Optional.of(rows.get(i));
        return Optional.empty();
    }
    public synchronized int size() { return rows.size(); }
}
