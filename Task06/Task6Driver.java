import campus.dao.EventDao;
import campus.dao.RegistrationDao;
import campus.db.ConnectionFactory;
import campus.service.RegistrationService;
import campus.ui.MainFrame;
import campus.ui.RegisterPanel;
import campus.ui.RegistrationsPanel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Task 6 Stage 5 - demonstrates the application with real mouse/keyboard input (java.awt.Robot) and
 * records a screenshot for every test case. Database: db-demo.properties (test-double driver,
 * 400 ms per statement so the loading state is visible).
 *
 * Run: xvfb-run -a -s "-screen 0 1280x800x24" java -cp out Task6Driver db-demo.properties
 */
public class Task6Driver {
    static ScreenshotKit kit;
    static MainFrame f;

    public static void main(String[] args) throws Exception {
        kit = new ScreenshotKit("screenshots");
        ConnectionFactory db = new ConnectionFactory(Path.of(args.length > 0 ? args[0] : "db-demo.properties"));
        RegistrationService svc = new RegistrationService(new EventDao(db), new RegistrationDao(db));
        f = ScreenshotKit.onEdt(() -> { MainFrame m = new MainFrame(svc, "in-memory test double", Path.of(db.property("sample.data"))); m.setLocation(40, 30); m.setVisible(true); return m; });
        kit.pause(300);
        System.out.println("startup, before events arrive: Register enabled=" + f.registerPanel.register.isEnabled() + " ; status: " + f.statusBar.getText());
        waitFor(() -> f.registerPanel.register.isEnabled(), 10_000);
        kit.pause(300);
        System.out.println("events loaded: " + f.statusBar.getText());
        RegisterPanel r = f.registerPanel;
        kit.capture("t6_1_register_screen.png", "Task 6 - Register screen (JMenuBar File/View/Help, CardLayout card 'register');\nevents and sample data were loaded in a SwingWorker at start-up", f);

        // ---------- TC1 normal registration
        fill(r, "192311450", "Keerthana S", "keerthana.1450@saveetha.ac.in", 1, 2, 3);
        kit.capture("t6_2a_normal_filled.png", "TC1 normal registration - form filled: 192311450, Keerthana S, CSE, year 2, Inter-college Debate", f);
        kit.click(r.register);
        kit.pause(150);
        waitFor(() -> r.register.isEnabled(), 5000); kit.pause(200);
        System.out.println("TC1 result: " + plain(r.message.getText()));
        kit.capture("t6_2b_normal_registered.png", "TC1 normal registration - saved through RegistrationService -> RegistrationDao (INSERT); confirmation shown", f);

        // ---------- TC2 invalid input
        kit.click(r.clear);
        fill(r, "12AB", "K3erthana", "keerthana@", 0, 0, 0);
        kit.click(r.register);
        kit.pause(300);
        System.out.println("TC2 invalid: " + plain(r.message.getText()));
        kit.capture("t6_3_invalid_input.png", "TC2 invalid input - rejected on the EDT before any database work: one message per field, invalid fields outlined in red", f);

        // ---------- TC3 duplicate registration (a student from input/sample_registrations.csv)
        String[] dup = Files.readAllLines(Path.of("input/sample_registrations.csv")).get(1).split(",");
        kit.click(r.clear);
        fill(r, dup[0], dup[1], dup[2], 1 + RegistrationService.DEPARTMENTS.indexOf(dup[3]), Integer.parseInt(dup[4]), Integer.parseInt(dup[5]));
        kit.click(r.register);
        kit.pause(1500);
        JDialog d = ScreenshotKit.showingDialog();
        System.out.println("TC3 duplicate " + dup[0] + " / event " + dup[5] + ": dialog '" + (d == null ? "none" : d.getTitle()) + "', message: " + plain(r.message.getText()));
        kit.capture("t6_4_duplicate_dialog.png", "TC3 duplicate - " + dup[0] + " is already registered for this event (sample data):\nSELECT 1 ... found the row, warning dialog shown, nothing inserted", f, d);
        kit.click(ScreenshotKit.button(d.getContentPane(), "OK"));

        // ---------- TC4 event full
        kit.click(r.clear);
        fill(r, "192311777", "Arjun Das", "arjun.1777@saveetha.ac.in", 6, 1, 4);
        kit.click(r.register);
        waitFor(() -> r.register.isEnabled(), 5000); kit.pause(200);
        System.out.println("TC4 full event: " + plain(r.message.getText()));
        kit.capture("t6_5_event_full.png", "TC4 event full - Photography Walk already has 3 of 3 places: registration refused", f);

        // ---------- View > Registrations (menu)
        RegistrationsPanel l = f.listPanel;
        kit.click(f.viewMenu);
        kit.pause(300);
        kit.capture("t6_6_view_menu.png", "Task 6 - View menu open: Register student (Ctrl+1) / Registrations (Ctrl+2)", f);
        rawClick(f.listItem);                                // do not wait for idle: capture while the worker runs
        kit.robot.delay(250);
        long lat = edtLatencyMs();
        System.out.println("TC5 list loading: progress visible=" + l.loading.isVisible() + ", event-thread latency during the query " + lat + " ms");
        kit.capture("t6_7_loading.png", "TC5 - Registrations card: the page query runs in a SwingWorker ('Loading...' bar);\nthe window stays responsive (event-thread latency " + lat + " ms during the load)", f);
        waitFor(() -> !l.loading.isVisible(), 8000); kit.pause(200);
        System.out.println("TC5 loaded: " + l.pageLabel.getText() + " ; rows in table " + l.table.getRowCount());
        kit.capture("t6_8_list_page1.png", "TC5 - page 1 loaded with LIMIT 20 OFFSET 0; includes the registration made in TC1", f);
        kit.click(l.next);
        waitFor(() -> !l.loading.isVisible() && l.pageLabel.getText().startsWith("Page 2"), 8000); kit.pause(200);
        System.out.println("TC6 Next: " + l.pageLabel.getText() + " ; first row id " + l.table.getValueAt(0, 0));
        kit.capture("t6_9_list_page2.png", "TC6 paging - Next clicked: page 2 (LIMIT 20 OFFSET 20)", f);

        // ---------- TC7 filter
        kit.selectComboIndex(l.filter, 1);
        waitFor(() -> !l.loading.isVisible() && l.pageLabel.getText().startsWith("Page 1"), 8000); kit.pause(300);
        System.out.println("TC7 filter " + l.filter.getSelectedItem() + ": " + l.pageLabel.getText());
        kit.capture("t6_10_filter_event.png", "TC7 filter - event 'Hackathon 24h' selected: only its registrations (WHERE r.event_id = ?), paging restarts at page 1", f);

        // ---------- TC8 export through File > Export CSV...
        kit.click(f.fileMenu);
        kit.pause(300);
        kit.click(f.exportItem);
        kit.pause(1200);
        JDialog chooser = ScreenshotKit.showingDialog();
        JTextField nameField = findTextField(chooser.getContentPane());
        Path out = Path.of("output/registrations_hackathon.csv");
        kit.type(nameField, "output/registrations_hackathon.csv");      // relative to the folder shown in the chooser
        kit.capture("t6_11_export_chooser.png", "TC8 export - File > Export CSV... opens a JFileChooser (modal dialog)", f, chooser);
        kit.click(ScreenshotKit.button(chooser.getContentPane(), "Save"));
        waitFor(() -> f.statusBar.getText().startsWith("Exported"), 8000); kit.pause(200);
        System.out.println("TC8 export: " + f.statusBar.getText() + " ; file lines " + Files.readAllLines(out).size());
        kit.capture("t6_12_exported.png", "TC8 export - all rows of the current filter written in a SwingWorker; status bar confirms", f);

        // ---------- exit
        kit.click(f.fileMenu); kit.pause(300); kit.click(f.exitItem); kit.pause(500);
        System.out.println("File > Exit -> window displayable = " + ScreenshotKit.onEdt(() -> f.isDisplayable()));
        System.exit(0);
    }

    static void fill(RegisterPanel r, String regNo, String name, String email, int dept, int year, int event) {
        kit.type(r.regNo, regNo);
        kit.type(r.name, name);
        kit.type(r.email, email);
        if (dept > 0) kit.selectComboIndex(r.dept, dept);
        if (year > 0) kit.selectComboIndex(r.year, year);
        if (event > 0) kit.selectComboIndex(r.event, event);
    }

    static void rawClick(java.awt.Component c) {
        java.awt.Point p = kit.centre(c);
        kit.robot.mouseMove(p.x, p.y);
        kit.robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
        kit.robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
    }

    static JTextField findTextField(java.awt.Container c) {
        for (java.awt.Component k : c.getComponents()) {
            if (k instanceof JTextField t && t.isShowing() && t.isEditable()) return t;
            if (k instanceof java.awt.Container cc) { JTextField t = findTextField(cc); if (t != null) return t; }
        }
        return null;
    }

    static long edtLatencyMs() throws Exception {
        long t0 = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> { });
        return (System.nanoTime() - t0) / 1_000_000;
    }

    interface Cond { boolean ok(); }
    static void waitFor(Cond c, long ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            if (ScreenshotKit.onEdt(c::ok)) return;
            kit.robot.delay(50);
        }
        System.out.println("  (timeout waiting)");
    }

    static String plain(String html) { return html.replace("<br>", " | ").replaceAll("<[^>]+>", "").replace("\u2714", "[OK]").replace("\u2716", "[X]").trim(); }
}
