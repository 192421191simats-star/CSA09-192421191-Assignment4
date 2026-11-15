import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

/**
 * Task 1 - evidence for the diagnosis (original structure) and the redesign, using real input.
 * Run: xvfb-run -a -s "-screen 0 1280x800x24" java -cp out Task1Driver
 */
public class Task1Driver {
    static ScreenshotKit kit;
    static final List<String> uncaught = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        kit = new ScreenshotKit("screenshots");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> { uncaught.add(t.getName() + ": " + e); System.out.println("  UNCAUGHT on " + t.getName() + ": " + e); });
        original();
        redesigned();
        System.exit(0);
    }

    /** Click without waiting for the event thread to become idle (it may be frozen). */
    static void rawClick(java.awt.Component c) {
        Point p = kit.centre(c);
        kit.robot.mouseMove(p.x, p.y);
        kit.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        kit.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    /** Time until the event-dispatch thread runs a posted task = how long the window is unresponsive. */
    static long edtLatencyMs() throws Exception {
        long t0 = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> { });
        return (System.nanoTime() - t0) / 1_000_000;
    }

    static void original() throws Exception {
        System.out.println("=== Original structure (OriginalBehaviourDemo) ===");
        OriginalBehaviourDemo w = ScreenshotKit.onEdt(() -> { OriginalBehaviourDemo f = new OriginalBehaviourDemo(); f.setLocation(60, 60); f.setVisible(true); return f; });
        kit.pause(700);
        // D4: sleep on the EDT freezes the window
        kit.type(w.regNo, "192311001");
        kit.type(w.amount, "45000");
        rawClick(w.pay);
        kit.robot.delay(200);
        long frozen = edtLatencyMs();
        System.out.println("  D4: after clicking Pay, a task posted to the event thread waited " + frozen + " ms (window frozen: no repaint, no typing, no clicks)");
        System.out.println("      SQL that would run: " + OriginalBehaviourDemo.executedSql.get(0) + " ; console: " + OriginalBehaviourDemo.console);
        kit.capture("t1_1_original_after_pay.png", "Task 1.1 - original: Pay clicked -> window was frozen for " + frozen + " ms (sleep on the EDT);\nthe only 'feedback' is 'Paid' printed to the console - the user sees nothing", w);
        // D7: SQL built by concatenation
        kit.type(w.regNo, "1'); DROP TABLE fees; --");
        kit.type(w.amount, "100");
        rawClick(w.pay);
        kit.robot.delay(200); edtLatencyMs();
        System.out.println("  D7: register number typed as 1'); DROP TABLE fees; --  -> SQL text: " + OriginalBehaviourDemo.executedSql.get(1));
        // D3: invalid amount
        kit.type(w.amount, "45k");
        kit.click(w.pay);
        kit.pause(300);
        System.out.println("  D3: amount '45k' -> " + (uncaught.isEmpty() ? "no exception?" : uncaught.get(uncaught.size() - 1)) + " ; message to the user: none");
        kit.capture("t1_2_original_invalid_amount.png", "Task 1.1 - original: amount '45k' -> NumberFormatException escapes on the event thread;\nno message in the window", w);
        // D6: Receipt button clears the form
        kit.type(w.regNo, "192311001"); kit.type(w.amount, "45000");
        kit.click(w.receipt);
        System.out.println("  D6: Receipt clicked -> fields now regNo='" + w.regNo.getText() + "' amount='" + w.amount.getText() + "' (the else branch cleared them)");
        kit.capture("t1_3_original_receipt_clears.png", "Task 1.1 - original: Receipt clicked -> the else branch CLEARS the form instead of showing a receipt", w);
        // D2: == on the action command
        kit.type(w.regNo, "192311001"); kit.type(w.amount, "45000");
        String cmd = new String("Pay");                                 // equal text, different String object
        ScreenshotKit.onEdt(() -> w.actionPerformed(new ActionEvent(w.pay, ActionEvent.ACTION_PERFORMED, cmd)));
        System.out.println("  D2: ActionEvent with command new String(\"Pay\") -> treated as Clear: regNo='" + w.regNo.getText() + "' ; \"Pay\".equals(cmd)=" + "Pay".equals(cmd) + ", \"Pay\" == cmd is false");
        // D9: closing
        EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
        q.postEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        kit.pause(500);
        System.out.println("  D8: close button (WINDOW_CLOSING) -> window still displayable = " + ScreenshotKit.onEdt(() -> w.isDisplayable()) + " (no WindowListener)");
        ScreenshotKit.onEdt(w::dispose);
    }

    static void redesigned() throws Exception {
        System.out.println("\n=== Redesigned FeeWindow + FeeService (bank delay 3 s, in-memory repository) ===");
        fees.FeeService svc = new fees.FeeService(new fees.SimulatedBankGateway(3000), new fees.InMemoryFeeRepository());
        fees.FeeWindow w = ScreenshotKit.onEdt(() -> { fees.FeeWindow f = new fees.FeeWindow(svc); f.setLocation(60, 60); f.setVisible(true); return f; });
        kit.pause(700);
        kit.click(w.pay);
        System.out.println("  empty form + Pay -> " + w.status.getText());
        kit.capture("t1_4_redesign_empty.png", "Task 1.2 - redesign: Pay with an empty form -> clear error message in the window", w);
        kit.type(w.regNo, "192311001"); kit.type(w.amount, "45k");
        kit.click(w.pay);
        System.out.println("  amount '45k' + Pay -> " + w.status.getText());
        kit.capture("t1_5_redesign_bad_amount.png", "Task 1.2 - redesign: amount '45k' -> validation message, nothing sent to the bank", w);
        kit.type(w.amount, "45000.50");
        kit.click(w.pay);
        kit.pause(300);
        long lat = edtLatencyMs();
        System.out.println("  valid + Pay -> during the 3 s bank wait: event-thread latency " + lat + " ms, Pay enabled=" + w.pay.isEnabled() + ", status: " + w.status.getText());
        kit.capture("t1_6_redesign_processing.png", "Task 1.2 - redesign: during the 3-second bank wait the window stays responsive (event thread latency " + lat + " ms);\nPay is disabled to prevent a double payment, progress message shown", w);
        kit.pause(3300);
        System.out.println("  after the worker finished -> " + w.status.getText());
        kit.capture("t1_7_redesign_paid.png", "Task 1.2 - redesign: payment completed in the background -> confirmation with receipt number", w);
        kit.robot.mouseMove(kit.centre(w.receipt).x, kit.centre(w.receipt).y);
        kit.robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); kit.robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);   // modal dialog: do not wait for idle
        kit.robot.delay(900);
        JDialog d = ScreenshotKit.showingDialog();
        kit.capture("t1_8_redesign_receipt.png", "Task 1.2 - redesign: Receipt button now shows the receipt", w, d);
        kit.click(ScreenshotKit.button(d.getContentPane(), "OK"));
        EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
        q.postEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
        kit.pause(500);
        System.out.println("  close button -> window displayable = " + ScreenshotKit.onEdt(() -> w.isDisplayable()) + " (DISPOSE_ON_CLOSE; no connection is held open between operations)");
    }
}
