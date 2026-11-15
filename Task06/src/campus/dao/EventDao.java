package campus.dao;

import campus.db.ConnectionFactory;
import campus.model.Event;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** JDBC access to the events table. */
public class EventDao {
    static final String FIND_ALL = "SELECT event_id, name, event_date, venue, capacity FROM events ORDER BY event_date, event_id";
    private final ConnectionFactory db;

    public EventDao(ConnectionFactory db) { this.db = db; }

    public List<Event> findAll() throws DataAccessException {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(FIND_ALL); ResultSet rs = ps.executeQuery()) {
            List<Event> out = new ArrayList<>();
            while (rs.next()) out.add(new Event(rs.getInt(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getString(4), rs.getInt(5)));
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load events", e);
        }
    }
}
