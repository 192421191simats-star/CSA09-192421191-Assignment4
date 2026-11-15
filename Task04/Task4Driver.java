import java.awt.Frame;
import java.awt.Label;
import java.awt.Point;
import java.lang.reflect.Field;

/**
 * Task 4 - drives each DrawPad version with real mouse clicks (java.awt.Robot) and records
 * screenshots before and after the window is minimised and restored.
 *
 * Run (needs a display): xvfb-run -a -s "-screen 0 1280x800x24" java -cp out Task4Driver
 */
public class Task4Driver {
    static final int[][] CLICKS = {{80, 80}, {150, 120}, {220, 90}, {290, 160}, {200, 200}};

    public static void main(String[] args) throws Exception {
        ScreenshotKit kit = new ScreenshotKit("screenshots");

        // ---------------------------------------------------------------- 4.1 original
        System.out.println("=== 4.1 Original program (as reported) ===");
        Frame original = ScreenshotKit.onEdt(() -> new DrawPadOriginal());           // its constructor calls setVisible(true)
        ScreenshotKit.onEdt(() -> original.setLocation(60, 60));
        kit.pause(600);
        clickAll(kit, original);
        int count = (int) field(original, "count");
        System.out.println("  after 5 real clicks: count field = " + count + ", label = '" + label(original, "status") + "'");
        kit.capture("t4_1_original_after_5_clicks.png", "Task 4.1 - original program after 5 real mouse clicks\nnothing drawn, label still 'Clicks: 0' (mouseclicked is never called)", original);
        ScreenshotKit.onEdt(original::dispose);

        // ---------------------------------------------------------------- 4.2 getGraphics version
        System.out.println("=== 4.2 First defect fixed, still using getGraphics() ===");
        Frame gg = ScreenshotKit.onEdt(() -> { Frame f = new DrawPadGetGraphics(); f.setLocation(60, 60); f.setVisible(true); return f; });
        kit.pause(600);
        clickAll(kit, gg);
        System.out.println("  after 5 clicks: label = '" + label(gg, "status") + "'");
        kit.capture("t4_2a_getGraphics_before_minimise.png", "Task 4.2 - step 1: five clicks with the name fixed: 5 dots drawn, label 'Clicks: 5'", gg);
        minimiseAndRestore(kit, gg);
        kit.capture("t4_2b_getGraphics_after_restore.png", "Task 4.2 - step 2: window minimised (hidden) and restored\nthe 5 dots are GONE: they were never stored, paint() does not redraw them", gg);
        ScreenshotKit.onEdt(gg::dispose);

        // ---------------------------------------------------------------- 4.3 repaired (Complete the Code)
        System.out.println("=== 4.3 Repaired program (Complete the Code) ===");
        Frame fixed = ScreenshotKit.onEdt(() -> { Frame f = new DrawPad(); f.setLocation(60, 60); f.setVisible(true); return f; });
        kit.pause(600);
        clickAll(kit, fixed);
        System.out.println("  after 5 clicks: label = '" + label(fixed, "status") + "', stored points = " + ((java.util.List<?>) field(fixed, "points")).size());
        kit.capture("t4_3a_repaired_before_minimise.png", "Task 4.3 (Complete the Code) - before minimising: 5 clicks, 5 dots, label 'Clicks: 5'", fixed);
        minimiseAndRestore(kit, fixed);
        kit.capture("t4_3b_repaired_after_restore.png", "Task 4.3 (Complete the Code) - after minimise and restore\nall 5 dots redrawn by paint() from the stored points", fixed);
        ScreenshotKit.onEdt(() -> fixed.setSize(520, 360));
        kit.capture("t4_3c_repaired_after_resize.png", "Task 4.3 - after resizing the window (another repaint): dots still there", fixed);
        ScreenshotKit.onEdt(fixed::dispose);

        // ---------------------------------------------------------------- alternative
        System.out.println("=== Alternative: BufferedImage version ===");
        Frame buf = ScreenshotKit.onEdt(() -> { Frame f = new DrawPadBuffered(); f.setLocation(60, 60); f.setVisible(true); return f; });
        kit.pause(600);
        clickAll(kit, buf);
        minimiseAndRestore(kit, buf);
        kit.capture("t4_alt_buffered_after_restore.png", "Task 4 alternative - BufferedImage version after minimise and restore: dots survive", buf);
        ScreenshotKit.onEdt(buf::dispose);
        System.exit(0);
    }

    static void clickAll(ScreenshotKit kit, Frame f) {
        Point o = ScreenshotKit.onEdt(f::getLocationOnScreen);
        for (int[] c : CLICKS) kit.click(o.x + c[0], o.y + c[1]);
        kit.pause(300);
    }

    /**
     * Minimise + restore. setExtendedState(ICONIFIED) is attempted first; this test display has no window
     * manager, so the window is also unmapped and re-mapped (setVisible false/true) - the same X11 expose
     * that a real restore produces, which makes AWT clear the window and call paint().
     */
    static void minimiseAndRestore(ScreenshotKit kit, Frame f) {
        ScreenshotKit.onEdt(() -> f.setExtendedState(Frame.ICONIFIED));
        kit.pause(300);
        ScreenshotKit.onEdt(() -> f.setVisible(false));
        kit.pause(400);
        ScreenshotKit.onEdt(() -> { f.setExtendedState(Frame.NORMAL); f.setVisible(true); });
        kit.pause(700);
        System.out.println("  minimised and restored");
    }

    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    static String label(Object o, String name) throws Exception { return ((Label) field(o, name)).getText(); }
}
