package fees;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * MySQL implementation (schema in sql/fees.sql). Every value is a bound parameter (no SQL injection),
 * and every connection/statement/result set is closed by try-with-resources, success or failure.
 */
public class JdbcFeeRepository implements FeeRepository {
    static final String INSERT = "INSERT INTO fee_payments (receipt_no, reg_no, amount, bank_ref, paid_at) VALUES (?, ?, ?, ?, ?)";
    static final String LATEST = "SELECT receipt_no, reg_no, amount, bank_ref, paid_at FROM fee_payments WHERE reg_no = ? ORDER BY paid_at DESC LIMIT 1";
    private final String url, user, password;

    public JdbcFeeRepository(String url, String user, String password) { this.url = url; this.user = user; this.password = password; }

    @Override public void save(Payment p) throws PaymentFailedException {
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(INSERT)) {
            ps.setString(1, p.receiptNo());
            ps.setString(2, p.regNo());
            ps.setBigDecimal(3, p.amount());
            ps.setString(4, p.bankReference());
            ps.setTimestamp(5, Timestamp.valueOf(p.paidAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PaymentFailedException("Payment taken by the bank (" + p.bankReference() + ") but not recorded - contact the accounts office", e);
        }
    }

    @Override public Optional<Payment> latestFor(String regNo) throws PaymentFailedException {
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(LATEST)) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Payment(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4), rs.getTimestamp(5).toLocalDateTime()));
            }
        } catch (SQLException e) {
            throw new PaymentFailedException("Could not read payments", e);
        }
    }
}
