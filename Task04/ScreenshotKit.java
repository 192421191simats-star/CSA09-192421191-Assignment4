import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * Shared helper for the GUI test drivers: drives REAL mouse and keyboard events through
 * java.awt.Robot (they go through the X server and AWT's event queue exactly like a user's input)
 * and captures the window(s) from the screen into PNG files with a caption naming the test.
 */
public final class ScreenshotKit {
    public final Robot robot;
    private final File dir;

    public ScreenshotKit(String dir) throws AWTException {
        robot = new Robot();
        robot.setAutoDelay(40);
        this.dir = new File(dir);
        this.dir.mkdirs();
    }

    public void pause(int ms) { robot.delay(ms); robot.waitForIdle(); }

    /** Runs code on the event-dispatch thread and returns its result. */
    public static <T> T onEdt(Callable<T> c) {
        AtomicReference<T> r = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> { try { r.set(c.call()); } catch (Exception e) { throw new RuntimeException(e); } });
        } catch (InterruptedException | InvocationTargetException e) { throw new RuntimeException(e); }
        return r.get();
    }
    public static void onEdt(Runnable r) { onEdt(() -> { r.run(); return null; }); }

    public Point centre(Component c) {
        return onEdt(() -> { Point p = c.getLocationOnScreen(); return new Point(p.x + c.getWidth() / 2, p.y + c.getHeight() / 2); });
    }

    public void click(Component c) { Point p = centre(c); click(p.x, p.y); }

    public void click(int x, int y) {
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        pause(150);
    }

    /** Clicks into a text component, selects all and types the text with real key events. */
    public void type(Component field, String text) {
        click(field);
        key(KeyEvent.VK_END);
        for (int i = 0; i < 40; i++) key(KeyEvent.VK_BACK_SPACE);
        for (char ch : text.toCharArray()) typeChar(ch);
        pause(100);
    }

    public void key(int code) { robot.keyPress(code); robot.keyRelease(code); }

    /** Opens a JComboBox popup with a real click and clicks the row with the given index. */
    public void selectComboIndex(javax.swing.JComboBox<?> combo, int index) {
        click(combo);
        pause(300);
        Point p = onEdt(() -> {
            javax.swing.plaf.basic.ComboPopup popup = (javax.swing.plaf.basic.ComboPopup) combo.getUI().getAccessibleChild(combo, 0);
            javax.swing.JList<?> list = popup.getList();
            list.ensureIndexIsVisible(index);
            Rectangle cell = list.getCellBounds(index, index);
            Point o = list.getLocationOnScreen();
            return new Point(o.x + cell.x + cell.width / 2, o.y + cell.y + cell.height / 2);
        });
        click(p.x, p.y);
        pause(200);
    }

    /** Clicks row `index` of a JList with a real mouse click. */
    public void clickListRow(javax.swing.JList<?> list, int index) {
        Point p = onEdt(() -> {
            Rectangle cell = list.getCellBounds(index, index);
            Point o = list.getLocationOnScreen();
            return new Point(o.x + cell.x + 40, o.y + cell.y + cell.height / 2);
        });
        click(p.x, p.y);
    }

    /** Finds a showing modal dialog (e.g. JOptionPane) and returns it, or null. */
    public static javax.swing.JDialog showingDialog() {
        for (Window w : Window.getWindows()) if (w instanceof javax.swing.JDialog d && d.isShowing()) return d;
        return null;
    }

    /** Finds a button by its text inside a container. */
    public static javax.swing.JButton button(java.awt.Container c, String text) {
        for (Component k : c.getComponents()) {
            if (k instanceof javax.swing.JButton b && text.equals(b.getText())) return b;
            if (k instanceof java.awt.Container cc) { javax.swing.JButton b = button(cc, text); if (b != null) return b; }
        }
        return null;
    }

    private void typeChar(char ch) {
        boolean shift = Character.isUpperCase(ch) || "@_:!?\"".indexOf(ch) >= 0;
        int code = switch (ch) {
            case '.' -> KeyEvent.VK_PERIOD; case '-' -> KeyEvent.VK_MINUS; case ' ' -> KeyEvent.VK_SPACE;
            case '@' -> KeyEvent.VK_2; case '_' -> KeyEvent.VK_MINUS; case ',' -> KeyEvent.VK_COMMA;
            default -> KeyEvent.getExtendedKeyCodeForChar(Character.toLowerCase(ch));
        };
        if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(code); robot.keyRelease(code);
        if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    /** Captures the given windows (their bounding box) and writes a PNG with a caption strip on top. */
    public File capture(String file, String caption, Window... windows) throws IOException {
        pause(250);
        Rectangle r = onEdt(() -> {
            Rectangle u = null;
            for (Window w : windows) if (w != null && w.isShowing()) { Rectangle b = new Rectangle(w.getLocationOnScreen(), w.getSize()); u = u == null ? b : u.union(b); }
            return u;
        });
        BufferedImage shot = robot.createScreenCapture(r);
        return save(shot, file, caption);
    }

    public File captureScreen(String file, String caption, Rectangle area) throws IOException {
        pause(250);
        return save(robot.createScreenCapture(area), file, caption);
    }

    private File save(BufferedImage shot, String file, String caption) throws IOException {
        int pad = 6, lineH = 17;
        String[] lines = caption.split("\n");
        int capH = pad * 2 + lineH * lines.length;
        int w = Math.max(shot.getWidth() + 2, 560);
        BufferedImage out = new BufferedImage(w, shot.getHeight() + capH + 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new Color(0xfcfcfb)); g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setColor(new Color(0x1f3a68)); g.fillRect(0, 0, out.getWidth(), capH);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        for (int i = 0; i < lines.length; i++) g.drawString(lines[i], pad + 2, pad + 13 + i * lineH);
        g.drawImage(shot, 1, capH + 1, null);
        g.setColor(new Color(0x9a9a9a)); g.drawRect(0, capH, shot.getWidth() + 1, shot.getHeight() + 1);
        g.dispose();
        File f = new File(dir, file);
        ImageIO.write(out, "png", f);
        System.out.println("  screenshot " + f.getPath() + "  <- " + lines[0]);
        return f;
    }
}
