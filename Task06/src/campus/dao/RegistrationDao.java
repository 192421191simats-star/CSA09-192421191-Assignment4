package campus.dao;

import campus.db.ConnectionFactory;
import campus.model.Registration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * JDBC access to the registrations table. Every value is a bound parameter; every resource is closed
 * by try-with-resources. eventId == null means "all events".
 */
public class RegistrationDao {
    static final String EXISTS = "SELECT 1 FROM registrations WHERE reg_no = ? AND event_id = ?";
    static final String INSERT = "INSERT INTO registrations (reg_no, student_name, email, dept, year, event_id, registered_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    static final String COUNT_ALL = "SELECT COUNT(*) FROM registrations";
    static final String COUNT_BY_EVENT = "SELECT COUNT(*) FROM registrations WHERE event_id = ?";
    static final String COLUMNS = "SELECT r.reg_id, r.reg_no, r.student_name, r.email, r.dept, r.year, r.event_id, e.name, r.registered_at"
            + " FROM registrations r JOIN events e ON e.event_id = r.event_id";
    static final String PAGE_ALL = COLUMNS + " ORDER BY r.reg_id LIMIT ? OFFSET ?";
    static final String PAGE_BY_EVENT = COLUMNS + " WHERE r.event_id = ? ORDER BY r.reg_id LIMIT ? OFFSET ?";
    static final String ALL = COLUMNS + " ORDER BY r.reg_id";
    static final String ALL_BY_EVENT = COLUMNS + " WHERE r.event_id = ? ORDER BY r.reg_id";

    private final ConnectionFactory db;
    public RegistrationDao(ConnectionFactory db) { this.db = db; }

    public boolean exists(String regNo, int eventId) throws DataAccessException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(EXISTS)) {
            ps.setString(1, regNo);
            ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new DataAccessException("Could not check for an existing registration", e); }
    }

    public void insert(Registration r) throws DataAccessException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(INSERT)) {
            bind(ps, r);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateKeyException("Already registered (database key)", e);
        } catch (SQLException e) { throw new DataAccessException("Could not save the registration", e); }
    }

    /** Bulk load (sample data, benchmarks): one connection, JDBC batching, one transaction. */
    public int insertAll(List<Registration> rows) throws DataAccessException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(INSERT)) {
            c.setAutoCommit(false);
            int n = 0;
            for (Registration r : rows) {
                bind(ps, r); ps.addBatch();
                if (++n % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
            c.commit();
            return n;
        } catch (SQLException e) { throw new DataAccessException("Bulk insert failed", e); }
    }

    private static void bind(PreparedStatement ps, Registration r) throws SQLException {
        ps.setString(1, r.regNo());
        ps.setString(2, r.studentName());
        ps.setString(3, r.email());
        ps.setString(4, r.dept());
        ps.setInt(5, r.year());
        ps.setInt(6, r.eventId());
        ps.setTimestamp(7, Timestamp.valueOf(r.registeredAt()));
    }

    public int count(Integer eventId) throws DataAccessException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(eventId == null ? COUNT_ALL : COUNT_BY_EVENT)) {
            if (eventId != null) ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return (int) rs.getLong(1); }
        } catch (SQLException e) { throw new DataAccessException("Could not count registrations", e); }
    }

    /** One page: SQL LIMIT / OFFSET. */
    public List<Registration> page(Integer eventId, int limit, int offset) throws DataAccessException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(eventId == null ? PAGE_ALL : PAGE_BY_EVENT)) {
            int i = 1;
            if (eventId != null) ps.setInt(i++, eventId);
            ps.setInt(i++, limit);
            ps.setInt(i, offset);
            List<Registration> out = new ArrayList<>(limit);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(read(rs)); }
            return out;
        } catch (SQLException e) { throw new DataAccessException("Could not load registrations", e); }
    }

    /** Streams every matching row to the consumer (export; the all-at-once alternative). */
    public void forEach(Integer eventId, Consumer<Registration> sink) throws DataAccessException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(eventId == null ? ALL : ALL_BY_EVENT)) {
            if (eventId != null) ps.setInt(1, eventId);
            ps.setFetchSize(500);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) sink.accept(read(rs)); }
        } catch (SQLException e) { throw new DataAccessException("Could not read registrations", e); }
    }

    private static Registration read(ResultSet rs) throws SQLException {
        return new Registration(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getInt(6), rs.getInt(7), rs.getString(8), rs.getTimestamp(9).toLocalDateTime());
    }
}
