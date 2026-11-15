import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JDialog;

/**
 * Task 2 - drives the appointment desk with real input and records screenshots and the event trace
 * from opening the window to closing it.
 *
 * Run: xvfb-run -a -s "-screen 0 1280x800x24" java -cp out Task2Driver input/doctors.csv
 */
public class Task2Driver {
    public static void main(String[] args) throws Exception {
        ScreenshotKit kit = new ScreenshotKit("screenshots");
        AppointmentService svc = new AppointmentService(Path.of(args.length > 0 ? args[0] : "input/doctors.csv"));
        AppointmentDesk d = ScreenshotKit.onEdt(() -> { AppointmentDesk w = new AppointmentDesk(svc); w.setLocation(60, 60); w.setVisible(true); return w; });
        kit.pause(900);
        d.trace("--- driver: window shown");
        kit.capture("t2_1_opened.png", "Task 2 - desk opened: departments loaded once from input/doctors.csv into a Map; doctor list empty", d);

        d.trace("--- driver: type patient ID P104233");
        kit.type(d.patientId, "P104233");
        d.trace("--- driver: choose department Cardiology");
        kit.selectComboIndex(d.department, 1);
        kit.capture("t2_2_department_cardiology.png", "Task 2.3(a) - department changed to Cardiology: doctor list refreshed to its 3 doctors", d);
        d.trace("--- driver: change department to Paediatrics");
        kit.selectComboIndex(d.department, 3);
        kit.capture("t2_3_department_paediatrics.png", "Task 2.3(a) - department changed to Paediatrics: list replaced by Paediatrics doctors", d);

        d.trace("--- driver: click first doctor, choose 2nd slot, click Book");
        kit.clickListRow(d.doctors, 0);
        kit.selectComboIndex(d.slot, 1);
        kit.click(d.book);
        kit.capture("t2_4_booked.png", "Task 2.3(b) - valid booking: confirmation shown, booked slot removed from the free slots", d);

        d.trace("--- driver: invalid patient ID '12345'");
        kit.type(d.patientId, "12345");
        kit.click(d.book);
        kit.capture("t2_5_invalid_id.png", "Task 2.3(b) - invalid patient ID: error message, nothing booked", d);

        d.trace("--- driver: Clear, then Book with no department");
        kit.click(d.clear);
        kit.type(d.patientId, "P200011");
        kit.click(d.book);
        kit.capture("t2_6_no_department.png", "Task 2.3(b) - no department selected: error message", d);

        d.trace("--- driver: close button pressed while a booking is in progress");
        EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
        q.postEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));       // what the title-bar close button sends
        kit.pause(900);
        JDialog dlg = ScreenshotKit.showingDialog();
        kit.capture("t2_7_close_confirm.png", "Task 2 - closing with a booking in progress: confirmation dialog appears (DO_NOTHING_ON_CLOSE + windowClosing)", d, dlg);
        JButton no = ScreenshotKit.button(dlg.getContentPane(), "No");
        kit.click(no);
        kit.pause(500);
        kit.capture("t2_8_after_no.png", "Task 2 - answered No: the desk stays open with the entered data", d);

        d.trace("--- driver: close again and answer Yes");
        q.postEvent(new WindowEvent(d, WindowEvent.WINDOW_CLOSING));
        kit.pause(900);
        dlg = ScreenshotKit.showingDialog();
        kit.click(ScreenshotKit.button(dlg.getContentPane(), "Yes"));
        kit.pause(800);
        Files.createDirectories(Path.of("output"));
        Files.write(Path.of("output/t2_event_trace.txt"), d.trace);
        System.out.println("window displayable after Yes: " + ScreenshotKit.onEdt(() -> d.isDisplayable()));
        System.out.println("event trace written to output/t2_event_trace.txt (" + d.trace.size() + " lines)");
        System.exit(0);
    }
}
