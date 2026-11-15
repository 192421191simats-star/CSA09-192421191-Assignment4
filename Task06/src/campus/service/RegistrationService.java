package campus.service;

import campus.dao.DataAccessException;
import campus.dao.DuplicateKeyException;
import campus.dao.EventDao;
import campus.dao.RegistrationDao;
import campus.model.Event;
import campus.model.Page;
import campus.model.Registration;
import campus.model.RegistrationForm;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service layer: every rule of the application. No Swing types and no SQL - testable on its own.
 * validate() is fast (no database) and may run on the EDT; all other methods touch the database and
 * must be called from a background thread (SwingWorker).
 */
public class RegistrationService {
    public static final int PAGE_SIZE = 20;
    public static final List<String> DEPARTMENTS = List.of("CSE", "ECE", "EEE", "MECH", "CIVIL", "IT", "AIDS");
    private static final Pattern REG_NO = Pattern.compile("\\d{9}");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z .]{1,59}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");

    private final EventDao events;
    private final RegistrationDao registrations;
    private volatile Map<Integer, Event> eventCache = Map.of();

    public RegistrationService(EventDao events, RegistrationDao registrations) {
        this.events = events; this.registrations = registrations;
    }

    /** Loads the events once (background thread). */
    public List<Event> loadEvents() throws DataAccessException {
        List<Event> list = events.findAll();
        Map<Integer, Event> m = new LinkedHashMap<>();
        for (Event e : list) m.put(e.id(), e);
        eventCache = m;
        return list;
    }

    /** Rules R1-R6. Fast, no database: safe on the event-dispatch thread. */
    public Registration validate(RegistrationForm f) throws ValidationException {
        Map<String, String> err = new LinkedHashMap<>();
        String regNo = trim(f.regNo()), name = trim(f.studentName()).replaceAll("\\s+", " "), email = trim(f.email()).toLowerCase();
        if (regNo.isEmpty()) err.put("regNo", "Register number is required.");
        else if (!REG_NO.matcher(regNo).matches()) err.put("regNo", "Register number must be exactly 9 digits.");
        if (name.isEmpty()) err.put("name", "Name is required.");
        else if (!NAME.matcher(name).matches()) err.put("name", "Name may contain only letters, spaces and dots (2-60 characters).");
        if (email.isEmpty()) err.put("email", "E-mail is required.");
        else if (!EMAIL.matcher(email).matches()) err.put("email", "E-mail address is not valid.");
        if (f.dept() == null || !DEPARTMENTS.contains(f.dept())) err.put("dept", "Select a department.");
        int year = 0;
        try { year = Integer.parseInt(trim(f.year())); } catch (NumberFormatException e) { /* reported below */ }
        if (year < 1 || year > 4) err.put("year", "Select the year of study (1-4).");
        Event ev = f.eventId() == null ? null : eventCache.get(f.eventId());
        if (ev == null) err.put("event", "Select an event.");
        if (!err.isEmpty()) throw new ValidationException(err);
        return new Registration(0, regNo, name, email, f.dept(), year, ev.id(), ev.name(), LocalDateTime.now().withNano(0));
    }

    /** R1-R8. Background thread only. */
    public Registration register(RegistrationForm f) throws ValidationException, DuplicateRegistrationException, DataAccessException {
        Registration r = validate(f);
        if (registrations.exists(r.regNo(), r.eventId()))                       // R7, friendly message
            throw new DuplicateRegistrationException(r.regNo() + " is already registered for " + r.eventName() + ".");
        Event ev = eventCache.get(r.eventId());
        if (registrations.count(r.eventId()) >= ev.capacity())                   // R8
            throw new ValidationException(Map.of("event", ev.name() + " is full (" + ev.capacity() + " places)."));
        try {
            registrations.insert(r);
        } catch (DuplicateKeyException e) {                                       // R7, final guard (two desks at once)
            throw new DuplicateRegistrationException(r.regNo() + " is already registered for " + r.eventName() + ".");
        }
        return r;
    }

    /** R10: one page through LIMIT/OFFSET. Background thread only. */
    public Page page(Integer eventId, int pageIndex) throws DataAccessException {
        int total = registrations.count(eventId);
        int pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int p = Math.max(0, Math.min(pageIndex, pages - 1));
        return new Page(registrations.page(eventId, PAGE_SIZE, p * PAGE_SIZE), p, PAGE_SIZE, total);
    }

    /** R11: every registration of the current filter (all pages) to a CSV file. Background thread only. */
    public int exportCsv(Integer eventId, Path file) throws DataAccessException, IOException {
        int[] n = {0};
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("reg_id,reg_no,student_name,email,dept,year,event,registered_at\n");
            registrations.forEach(eventId, r -> {
                try {
                    w.write(r.id() + "," + r.regNo() + "," + csv(r.studentName()) + "," + r.email() + "," + r.dept() + ","
                            + r.year() + "," + csv(r.eventName()) + "," + r.registeredAt() + "\n");
                    n[0]++;
                } catch (IOException e) { throw new UncheckedIOException(e); }
            });
        } catch (UncheckedIOException e) { throw e.getCause(); }
        return n[0];
    }

    /** Loads sample data when the table is empty (first start). Background thread only. */
    public int loadSampleIfEmpty(Path csv) throws DataAccessException, IOException {
        if (csv == null || !Files.exists(csv) || registrations.count(null) > 0) return 0;
        List<Registration> rows = new java.util.ArrayList<>();
        LocalDateTime t = LocalDateTime.of(2026, 11, 10, 9, 0);
        for (String l : Files.readAllLines(csv)) {
            if (l.isBlank() || l.startsWith("reg_no")) continue;
            String[] p = l.split(",");
            t = t.plusMinutes(37);
            rows.add(new Registration(0, p[0], p[1], p[2], p[3], Integer.parseInt(p[4]), Integer.parseInt(p[5]), null, t));
        }
        return registrations.insertAll(rows);
    }

    public Set<Integer> eventIds() { return eventCache.keySet(); }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static String csv(String s) { return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s; }
}
