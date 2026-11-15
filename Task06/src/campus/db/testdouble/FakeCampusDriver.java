package campus.db.testdouble;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TEST DOUBLE - an in-process JDBC driver used only because MySQL is not available where the evidence
 * was produced. It is NOT a database: it understands exactly the SQL statements issued by
 * campus.dao.EventDao and campus.dao.RegistrationDao, keeps the two tables in memory, enforces the
 * same UNIQUE (reg_no, event_id) key as sql/schema.sql (throwing SQLIntegrityConstraintViolationException,
 * vendor code 1062, like MySQL) and walks OFFSET rows one by one, as MySQL does.
 *
 * URL: jdbc:campusdb:<name>?schema=sql/schema.sql&latencyMs=400
 *   schema    - events are loaded from the INSERT INTO events lines of that file
 *   latencyMs - sleep per executed statement, to simulate a network round trip
 *   rowMicros - extra time per returned row, to simulate transferring/decoding rows over the network
 */
public final class FakeCampusDriver implements Driver {
    public static final String PREFIX = "jdbc:campusdb:";
    private static final Map<String, Database> DATABASES = new ConcurrentHashMap<>();

    static {
        try { DriverManager.registerDriver(new FakeCampusDriver()); }
        catch (SQLException e) { throw new ExceptionInInitializerError(e); }
    }

    public static void drop(String name) { DATABASES.remove(name); }

    @Override public boolean acceptsURL(String url) { return url != null && url.startsWith(PREFIX); }

    @Override public Connection connect(String url, java.util.Properties info) throws SQLException {
        if (!acceptsURL(url)) return null;
        String rest = url.substring(PREFIX.length());
        String name = rest.contains("?") ? rest.substring(0, rest.indexOf('?')) : rest;
        Map<String, String> params = new HashMap<>();
        if (rest.contains("?")) for (String kv : rest.substring(rest.indexOf('?') + 1).split("&")) {
            String[] p = kv.split("=", 2); params.put(p[0], p.length > 1 ? p[1] : "");
        }
        long latency = Long.parseLong(params.getOrDefault("latencyMs", "0"));
        long rowNanos = (long) (Double.parseDouble(params.getOrDefault("rowMicros", "0")) * 1000);
        Database db = DATABASES.computeIfAbsent(name, n -> new Database(params.get("schema")));
        return db.connection(latency, rowNanos);
    }

    @Override public DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) { return new DriverPropertyInfo[0]; }
    @Override public int getMajorVersion() { return 1; }
    @Override public int getMinorVersion() { return 0; }
    @Override public boolean jdbcCompliant() { return false; }
    @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }

    // ------------------------------------------------------------------ in-memory tables
    static final class Database {
        final List<Object[]> events = new ArrayList<>();          // event_id, name, event_date, venue, capacity
        final List<Object[]> regs = new ArrayList<>();            // reg_id, reg_no, name, email, dept, year, event_id, registered_at
        final Set<String> uniqueKey = new HashSet<>();
        long nextId = 1;

        Database(String schemaFile) {
            if (schemaFile == null) return;
            Pattern p = Pattern.compile("INSERT INTO events .*VALUES \\((\\d+), '([^']*)', '([^']*)', '([^']*)', (\\d+)\\);");
            try {
                for (String line : Files.readAllLines(Path.of(schemaFile))) {
                    Matcher m = p.matcher(line);
                    if (m.find()) events.add(new Object[]{Integer.parseInt(m.group(1)), m.group(2), Date.valueOf(m.group(3)), m.group(4), Integer.parseInt(m.group(5))});
                }
            } catch (IOException e) { throw new IllegalStateException("cannot read " + schemaFile, e); }
        }

        Connection connection(long latency, long rowNanos) {
            boolean[] closed = {false};
            InvocationHandler h = (proxy, m, args) -> switch (m.getName()) {
                case "prepareStatement" -> statement((String) args[0], latency, rowNanos);
                case "close" -> { closed[0] = true; yield null; }
                case "isClosed" -> closed[0];
                case "isValid", "getAutoCommit" -> true;
                case "setAutoCommit", "commit", "rollback" -> null;
                case "unwrap" -> null;
                case "isWrapperFor" -> false;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "FakeCampusConnection";
                default -> throw new SQLFeatureNotSupportedException("test double: Connection." + m.getName());
            };
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, h);
        }

        PreparedStatement statement(String sql, long latency, long rowNanos) {
            Object[][] params = {new Object[16]};
            List<Object[]> batch = new ArrayList<>();
            InvocationHandler h = (proxy, m, args) -> {
                String n = m.getName();
                if (n.startsWith("set") && args != null && args.length == 2 && args[0] instanceof Integer idx) { params[0][idx] = args[1]; return null; }
                return switch (n) {
                    case "executeQuery" -> {
                        pause(latency);
                        List<Object[]> rows = query(sql, params[0]);
                        if (rowNanos > 0) java.util.concurrent.locks.LockSupport.parkNanos(rows.size() * rowNanos);
                        yield resultSet(rows);
                    }
                    case "executeUpdate" -> { pause(latency); yield update(sql, params[0]); }
                    case "addBatch" -> { batch.add(params[0].clone()); yield null; }
                    case "executeBatch" -> {
                        pause(latency);
                        int[] r = new int[batch.size()];
                        for (int i = 0; i < r.length; i++) r[i] = update(sql, batch.get(i));
                        batch.clear();
                        yield r;
                    }
                    case "clearParameters" -> { params[0] = new Object[16]; yield null; }
                    case "close", "setFetchSize", "setQueryTimeout" -> null;
                    case "isClosed" -> false;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "FakePreparedStatement[" + sql + "]";
                    default -> throw new SQLFeatureNotSupportedException("test double: PreparedStatement." + n);
                };
            };
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class}, h);
        }

        static void pause(long ms) { if (ms > 0) try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

        synchronized List<Object[]> query(String sql, Object[] p) throws SQLException {
            if (sql.startsWith("SELECT event_id, name, event_date, venue, capacity FROM events")) {
                List<Object[]> r = new ArrayList<>(events);
                r.sort(Comparator.comparing((Object[] e) -> (Date) e[2]).thenComparing(e -> (Integer) e[0]));
                return r;
            }
            if (sql.startsWith("SELECT 1 FROM registrations WHERE reg_no = ? AND event_id = ?")) {
                return uniqueKey.contains(p[1] + "#" + p[2]) ? List.<Object[]>of(new Object[]{1}) : List.of();
            }
            if (sql.startsWith("SELECT COUNT(*) FROM registrations")) {
                boolean filtered = sql.contains("WHERE event_id = ?");
                long c = 0;
                for (Object[] r : regs) if (!filtered || r[6].equals(p[1])) c++;
                return List.<Object[]>of(new Object[]{c});
            }
            if (sql.startsWith("SELECT r.reg_id, r.reg_no")) {
                boolean filtered = sql.contains("WHERE r.event_id = ?");
                boolean paged = sql.contains("LIMIT ? OFFSET ?");
                int base = filtered ? 2 : 1;
                int limit = paged ? (Integer) p[base] : Integer.MAX_VALUE, offset = paged ? (Integer) p[base + 1] : 0;
                Map<Integer, String> names = new HashMap<>();
                for (Object[] e : events) names.put((Integer) e[0], (String) e[1]);
                List<Object[]> out = new ArrayList<>();
                int skipped = 0;
                for (Object[] r : regs) {                         // regs are kept in reg_id order
                    if (filtered && !r[6].equals(p[1])) continue;
                    if (skipped < offset) { skipped++; continue; }   // OFFSET rows are read and discarded
                    if (out.size() >= limit) break;
                    out.add(new Object[]{r[0], r[1], r[2], r[3], r[4], r[5], r[6], names.get((Integer) r[6]), r[7]});
                }
                return out;
            }
            throw new SQLFeatureNotSupportedException("test double does not understand: " + sql);
        }

        synchronized int update(String sql, Object[] p) throws SQLException {
            if (sql.startsWith("INSERT INTO registrations")) {
                String key = p[1] + "#" + p[6];
                if (!uniqueKey.add(key))
                    throw new SQLIntegrityConstraintViolationException("Duplicate entry '" + p[1] + "-" + p[6] + "' for key 'registrations.uq_student_event'", "23000", 1062);
                boolean eventExists = events.stream().anyMatch(e -> e[0].equals(p[6]));
                if (!eventExists) { uniqueKey.remove(key); throw new SQLIntegrityConstraintViolationException("Cannot add or update a child row: a foreign key constraint fails (fk_reg_event)", "23000", 1452); }
                regs.add(new Object[]{nextId++, p[1], p[2], p[3], p[4], p[5], p[6], p[7]});
                return 1;
            }
            if (sql.startsWith("DELETE FROM registrations")) { int n = regs.size(); regs.clear(); uniqueKey.clear(); return n; }
            throw new SQLFeatureNotSupportedException("test double does not understand: " + sql);
        }

        static ResultSet resultSet(List<Object[]> rows) {
            int[] cursor = {-1};
            InvocationHandler h = (proxy, m, args) -> {
                String n = m.getName();
                return switch (n) {
                    case "next" -> ++cursor[0] < rows.size();
                    case "getString" -> { Object v = col(rows, cursor[0], args); yield v == null ? null : v.toString(); }
                    case "getInt" -> ((Number) col(rows, cursor[0], args)).intValue();
                    case "getLong" -> ((Number) col(rows, cursor[0], args)).longValue();
                    case "getDate" -> (Date) col(rows, cursor[0], args);
                    case "getTimestamp" -> (Timestamp) col(rows, cursor[0], args);
                    case "getObject" -> col(rows, cursor[0], args);
                    case "wasNull" -> false;
                    case "close" -> null;
                    case "isClosed" -> false;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "FakeResultSet(" + rows.size() + " rows)";
                    default -> throw new SQLFeatureNotSupportedException("test double: ResultSet." + n);
                };
            };
            return (ResultSet) Proxy.newProxyInstance(FakeCampusDriver.class.getClassLoader(), new Class<?>[]{ResultSet.class}, h);
        }

        static Object col(List<Object[]> rows, int cursor, Object[] args) throws SQLException {
            if (!(args[0] instanceof Integer i)) throw new SQLFeatureNotSupportedException("test double: use column indexes");
            return rows.get(cursor)[i - 1];
        }
    }
}
