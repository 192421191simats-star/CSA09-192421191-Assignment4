import java.awt.Window;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * Task 5 - drives the converters with real keyboard and mouse input and captures screenshots.
 *   mode "complete": the Complete-the-Code Converter (lambda)
 *   mode "v1" / "v2": the four styles before / after the requirement change (classpath decides which)
 *   mode "action"   : the shared-Action alternative
 * Run: xvfb-run -a -s "-screen 0 1280x800x24" java -cp out_driver:out:out_v1 Task5Driver v1
 */
public class Task5Driver {
    static ScreenshotKit kit;

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "complete";
        kit = new ScreenshotKit("screenshots");
        switch (mode) {
            case "complete" -> complete();
            case "v1", "v2" -> styles(mode);
            default -> action();
        }
        System.exit(0);
    }

    static JFrame open(String cls) {
        return ScreenshotKit.onEdt(() -> {
            JFrame f = (JFrame) Class.forName(cls).getDeclaredConstructor().newInstance();
            f.setLocation(80, 80); f.setVisible(true); return f;
        });
    }

    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }

    /** Opens the combo's popup with a real click, then clicks the "EUR" row of the popup list. */
    static void chooseEur(JComboBox<?> combo) {
        kit.click(combo);
        kit.pause(300);
        java.awt.Point p = ScreenshotKit.onEdt(() -> {
            javax.swing.plaf.basic.ComboPopup popup = (javax.swing.plaf.basic.ComboPopup) combo.getUI().getAccessibleChild(combo, 0);
            javax.swing.JList<?> list = popup.getList();
            java.awt.Rectangle cell = list.getCellBounds(1, 1);
            java.awt.Point o = list.getLocationOnScreen();
            return new java.awt.Point(o.x + cell.x + cell.width / 2, o.y + cell.y + cell.height / 2);
        });
        kit.click(p.x, p.y);
        kit.pause(200);
    }

    static void complete() throws Exception {
        System.out.println("=== Complete the Code: Converter (lambda) ===");
        JFrame f = ScreenshotKit.onEdt(() -> {
            JFrame c = new Converter();
            java.awt.Dimension packed = c.getSize();
            System.out.println("  packed size " + packed.width + "x" + packed.height + " - the result label (' ') was sized empty;");
            System.out.println("  a result would wrap to a 2nd FlowLayout row outside the window, so the window is made wider");
            c.setSize(packed.width + 110, packed.height);
            c.validate();                                    // re-run the layout for the new width
            c.setLocation(80, 80); c.setVisible(true); return c;
        });
        kit.pause(700);
        JTextField amount = (JTextField) field(f, "amount");
        JLabel result = (JLabel) field(f, "result");
        JButton btn = ScreenshotKit.onEdt(() -> findButton(f));
        if (System.getProperty("debug") != null) for (java.awt.Component k : f.getContentPane().getComponents())
            System.out.println("  DEBUG " + k.getClass().getSimpleName() + " screen=" + ScreenshotKit.onEdt(() -> k.getLocationOnScreen()) + " size=" + k.getSize());
        kit.type(amount, "1000");
        kit.click(btn);
        System.out.println("  1000 INR, USD, Convert clicked -> '" + result.getText() + "'");
        kit.capture("t5_complete_valid_usd.png", "Task 5 Complete the Code - valid amount: 1000 INR, USD, Convert button clicked -> 12.00 USD", f);
        chooseEur((JComboBox<?>) field(f, "target"));
        kit.type(amount, "2500");
        kit.key(KeyEvent.VK_ENTER);                                   // same lambda listener via Enter
        kit.pause(200);
        System.out.println("  2500 INR, EUR, Enter pressed -> '" + result.getText() + "'");
        kit.capture("t5_complete_valid_eur_enter.png", "Task 5 Complete the Code - Enter key in the amount field (same listener): 2500 INR -> 27.50 EUR", f);
        kit.type(amount, "12a");
        kit.click(btn);
        System.out.println("  '12a', Convert -> '" + result.getText() + "'");
        kit.capture("t5_complete_invalid.png", "Task 5 Complete the Code - invalid amount '12a' -> 'Invalid amount'", f);
        ScreenshotKit.onEdt(f::dispose);
    }

    static void styles(String version) throws Exception {
        String[] classes = {"ConverterImplements", "ConverterNamedInner", "ConverterAnonymous", "ConverterLambda"};
        String[] tags = {"a_implements", "b_named_inner", "c_anonymous", "d_lambda"};
        for (int i = 0; i < 4; i++) {
            System.out.println("=== " + version + " " + classes[i] + " ===");
            JFrame f = open(classes[i]);
            kit.pause(500);
            JTextField amount = (JTextField) field(f, "amount");
            JButton btn = (JButton) field(f, "convert");
            JLabel result = (JLabel) field(f, "result");
            if (version.equals("v1")) {
                kit.type(amount, "1000");
                kit.click(btn);
                System.out.println("  1000 + Convert -> '" + result.getText() + "'");
                kit.capture("t5_v1_" + tags[i] + "_valid.png", "Task 5.1 (" + tags[i].replace('_', ' ') + ") - 1000 INR, Convert clicked -> result label 12.00 USD", f);
                kit.type(amount, "abc");
                kit.key(KeyEvent.VK_ENTER);
                kit.pause(200);
                System.out.println("  'abc' + Enter (not wired in v1) -> '" + result.getText() + "'");
                kit.click(btn);
                System.out.println("  'abc' + Convert -> '" + result.getText() + "'");
                kit.capture("t5_v1_" + tags[i] + "_invalid.png", "Task 5.1 (" + tags[i].replace('_', ' ') + ") - 'abc', Convert clicked -> 'Invalid amount'", f);
                ScreenshotKit.onEdt(() -> f.dispatchEvent(new java.awt.event.WindowEvent(f, java.awt.event.WindowEvent.WINDOW_CLOSING)));
                kit.pause(300);
                System.out.println("  WINDOW_CLOSING sent -> WindowAdapter disposed the frame: displayable=" + ScreenshotKit.onEdt(f::isDisplayable));
            } else {
                kit.type(amount, "2500");
                kit.key(KeyEvent.VK_ENTER);                               // requirement change: Enter converts
                kit.pause(600);
                JDialog d = dialog();
                System.out.println("  2500 + Enter -> dialog shown: " + (d != null) + (d != null ? " text: " + dialogText(d) : ""));
                kit.capture("t5_v2_" + tags[i] + "_enter_dialog.png", "Task 5.2 (" + tags[i].replace('_', ' ') + ") - Enter pressed in the amount field -> result shown in a dialog", f, d);
                kit.key(KeyEvent.VK_ENTER); kit.pause(400);               // OK closes the dialog
                kit.type(amount, "abc");
                kit.click(btn);
                kit.pause(600);
                d = dialog();
                System.out.println("  'abc' + Convert -> error dialog: " + (d != null) + (d != null ? " text: " + dialogText(d) : ""));
                kit.capture("t5_v2_" + tags[i] + "_invalid_dialog.png", "Task 5.2 (" + tags[i].replace('_', ' ') + ") - invalid amount -> error dialog", f, d);
                kit.key(KeyEvent.VK_ENTER); kit.pause(400);
                ScreenshotKit.onEdt(f::dispose);
            }
        }
    }

    static void action() throws Exception {
        System.out.println("=== Alternative: one shared Action ===");
        ConverterSharedAction f = ScreenshotKit.onEdt(() -> { ConverterSharedAction c = new ConverterSharedAction(); c.setLocation(80, 80); c.setVisible(true); return c; });
        kit.pause(500);
        kit.type(f.amount, "1000");
        kit.robot.keyPress(KeyEvent.VK_CONTROL); kit.key(KeyEvent.VK_ENTER); kit.robot.keyRelease(KeyEvent.VK_CONTROL);
        kit.pause(200);
        System.out.println("  Ctrl+Enter -> '" + f.result.getText() + "'");
        kit.capture("t5_action_ctrl_enter.png", "Task 5 alternative - Ctrl+Enter shortcut runs the same shared Action: 12.00 USD", f);
        ScreenshotKit.onEdt(() -> f.convertAction.setEnabled(false));
        kit.pause(200);
        kit.capture("t5_action_disabled.png", "Task 5 alternative - convertAction.setEnabled(false) disables the button AND the menu item at once", f);
        int[] objs = ConverterSharedAction.methodReferenceObjects(f);
        System.out.println("  shared Action: button, menu item, text field all hold the same object: "
                + (f.button.getAction() == f.menuItem.getAction() && f.menuItem.getAction() == f.amount.getAction()));
        System.out.println("  method reference written at 2 registrations -> " + objs[0] + " listener objects; stored once -> " + objs[1]);
        ScreenshotKit.onEdt(f::dispose);
    }

    static JButton findButton(java.awt.Container c) {
        for (java.awt.Component k : c.getComponents()) {
            if (k instanceof JButton b && "Convert".equals(b.getText())) return b;   // not the combo's arrow button
            if (k instanceof java.awt.Container cc) { JButton b = findButton(cc); if (b != null) return b; }
        }
        return null;
    }

    static JDialog dialog() {
        for (Window w : Window.getWindows()) if (w instanceof JDialog d && d.isShowing()) return d;
        return null;
    }
    static String dialogText(JDialog d) {
        StringBuilder sb = new StringBuilder();
        collect(d.getContentPane(), sb);
        return sb.toString().trim();
    }
    static void collect(java.awt.Container c, StringBuilder sb) {
        for (java.awt.Component k : c.getComponents()) {
            if (k instanceof JLabel l && l.getText() != null && !l.getText().isBlank()) sb.append(l.getText()).append(' ');
            if (k instanceof java.awt.Container cc) collect(cc, sb);
        }
    }
}
