package campus.tools;

import campus.dao.EventDao;
import campus.dao.RegistrationDao;
import campus.db.ConnectionFactory;
import campus.model.RegistrationForm;
import campus.service.RegistrationService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 1 requirements as tests, run against the service + JDBC DAO with the test-double driver
 * (no window). With MySQL: java -cp out:lib/mysql-connector-j.jar campus.tools.RegistrationServiceTest db.properties
 * (uses a fresh schema; the sample data must NOT be loaded for the counts below).
 */
public class RegistrationServiceTest {
    static final List<String[]> rows = new ArrayList<>();
    interface Call { String run() throws Exception; }
    static String attempt(Call c) { try { return c.run(); } catch (Exception e) { return e.getClass().getSimpleName() + ": " + e.getMessage(); } }
    static void check(String id, String req, String what, String expected, String actual) {
        rows.add(new String[]{id, req, what, expected, actual, actual.equals(expected) || actual.startsWith(expected) ? "PASS" : "FAIL"});
    }

    public static void main(String[] args) throws Exception {
        ConnectionFactory db = args.length > 0 ? new ConnectionFactory(Path.of(args[0]))
                : new ConnectionFactory("jdbc:campusdb:unittest?schema=sql/schema.sql", "t", "t");
        RegistrationService s = new RegistrationService(new EventDao(db), new RegistrationDao(db));
        check("T1", "-", "events load from the database", "6", attempt(() -> String.valueOf(s.loadEvents().size())));
        RegistrationForm ok = new RegistrationForm("192311101", "Divya Raj", "divya.1101@saveetha.ac.in", "CSE", "3", 1);
        check("T2", "R1-R6", "valid registration accepted", "192311101 -> Hackathon 24h", attempt(() -> { var r = s.register(ok); return r.regNo() + " -> " + r.eventName(); }));
        check("T3", "R7", "same student, same event again", "DuplicateRegistrationException: 192311101 is already registered for Hackathon 24h.", attempt(() -> s.register(ok).toString()));
        check("T4", "R7", "same student, different event", "192311101 -> Tech Quiz", attempt(() -> { var r = s.register(new RegistrationForm("192311101", "Divya Raj", "divya.1101@saveetha.ac.in", "CSE", "3", 6)); return r.regNo() + " -> " + r.eventName(); }));
        check("T5", "R1", "register no '12AB'", "ValidationException: Register number must be exactly 9 digits.", attempt(() -> s.register(new RegistrationForm("12AB", "Divya Raj", "d@x.in", "CSE", "3", 1)).toString()));
        check("T6", "R2", "name '7Divya'", "ValidationException: Name may contain only letters", attempt(() -> s.register(new RegistrationForm("192311102", "7Divya", "d@x.in", "CSE", "3", 1)).toString()));
        check("T7", "R3", "e-mail 'divya@'", "ValidationException: E-mail address is not valid.", attempt(() -> s.register(new RegistrationForm("192311102", "Divya", "divya@", "CSE", "3", 1)).toString()));
        check("T8", "R4-R6", "no department, year or event", "ValidationException: Select a department. Select the year of study (1-4). Select an event.", attempt(() -> s.register(new RegistrationForm("192311102", "Divya", "d@x.in", "-- select --", "-- select --", null)).toString()));
        check("T9", "R1-R6", "every field empty -> one message per field", "6 errors", attempt(() -> { try { s.validate(new RegistrationForm("", "", "", null, null, null)); return "0"; } catch (campus.service.ValidationException v) { return v.fieldErrors().size() + " errors"; } }));
        check("T10", "R1", "SQL text in register no is rejected, not executed", "ValidationException: Register number must be exactly 9 digits.", attempt(() -> s.register(new RegistrationForm("1' OR '1'='1", "Divya", "d@x.in", "CSE", "3", 1)).toString()));
        for (int i = 0; i < 3; i++) { int k = i; attempt(() -> s.register(new RegistrationForm("19231120" + k, "Student " + "ABC".charAt(k), "s" + k + "@x.in", "ECE", "2", 4)).toString()); }
        check("T11", "R8", "4th registration for Photography Walk (capacity 3)", "ValidationException: Photography Walk is full (3 places).", attempt(() -> s.register(new RegistrationForm("192311299", "Late Comer", "late@x.in", "IT", "1", 4)).toString()));
        RegistrationDao dao = new RegistrationDao(db);
        check("T12", "R7", "DB key blocks a duplicate that bypasses the service check", "DuplicateKeyException: Already registered (database key)",
                attempt(() -> { dao.insert(new campus.model.Registration(0, "192311101", "Divya Raj", "d@x.in", "CSE", 3, 1, null, java.time.LocalDateTime.now())); return "inserted"; }));
        for (int i = 0; i < 45; i++) { int k = i; attempt(() -> s.register(new RegistrationForm(String.valueOf(192320000 + k), "Bulk Student", "b" + k + "@x.in", "MECH", "1", 5)).toString()); }
        check("T13", "R10", "page 1 of all (50 rows, 20 per page)", "20 rows, page 1 of 3", attempt(() -> { var p = s.page(null, 0); return p.rows().size() + " rows, page " + (p.pageIndex() + 1) + " of " + p.pageCount(); }));
        check("T14", "R10", "last page", "10 rows, page 3 of 3", attempt(() -> { var p = s.page(null, 2); return p.rows().size() + " rows, page " + (p.pageIndex() + 1) + " of " + p.pageCount(); }));
        check("T15", "R10", "filter Battle of Bands", "45 rows total, first page 20", attempt(() -> { var p = s.page(5, 0); return p.totalRows() + " rows total, first page " + p.rows().size(); }));
        check("T16", "R10", "page beyond the end is clamped", "page 3", attempt(() -> "page " + (s.page(null, 99).pageIndex() + 1)));
        Path tmp = Files.createTempFile("export", ".csv");
        check("T17", "R11", "export filter Hackathon (all pages)", "1 rows, file has 2 lines", attempt(() -> { int n = s.exportCsv(1, tmp); return n + " rows, file has " + Files.readAllLines(tmp).size() + " lines"; }));
        check("T18", "R11", "export all", "50 rows", attempt(() -> s.exportCsv(null, tmp) + " rows"));
        Files.delete(tmp);

        System.out.printf("%-4s %-6s %-52s %-80s %s%n", "ID", "Req", "test", "actual", "result");
        int pass = 0;
        for (String[] r : rows) { System.out.printf("%-4s %-6s %-52s %-80s %s%n", r[0], r[1], r[2], r[4], r[5]); if (r[5].equals("PASS")) pass++; }
        System.out.println(pass + " / " + rows.size() + " tests passed  (database: " + db.url() + ")");
    }
}
