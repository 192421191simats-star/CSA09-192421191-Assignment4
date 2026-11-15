import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Task 3 - drives the library screen (real typing, clicks, selection) and tests resizing at three
 * window sizes, recording the bounds of every region; then the GridBagLayout and null-layout
 * alternatives, including the time to compute a layout.
 *
 * Run: xvfb-run -a -s "-screen 0 1400x900x24" java -cp out Task3Driver input/catalogue.csv
 */
public class Task3Driver {
    public static void main(String[] args) throws Exception {
        ScreenshotKit kit = new ScreenshotKit("screenshots");
        Files.createDirectories(Path.of("data"));
        LibraryCatalog cat = new LibraryCatalog(Path.of(args.length > 0 ? args[0] : "input/catalogue.csv"));
        LibrarySearchScreen s = ScreenshotKit.onEdt(() -> { LibrarySearchScreen w = new LibrarySearchScreen(cat); w.setLocation(40, 40); w.setVisible(true); return w; });
        kit.pause(800);

        // ---------------- functional interactions
        System.out.println("=== Interactions ===");
        kit.type(s.query, "java");
        kit.click(s.search);
        System.out.println("  search 'java' -> status: " + s.status.getText() + " ; rows shown " + s.results.getItemCount());
        kit.capture("t3_1_search_java.png", "Task 3.3 - typed 'java', clicked Search: matching items listed in the results area, status shows the count", s);
        java.awt.Point o = ScreenshotKit.onEdt(() -> s.results.getLocationOnScreen());
        kit.click(o.x + 120, o.y + 25);                       // second row
        kit.click(s.reserve);
        System.out.println("  selected row " + s.results.getSelectedIndex() + ", Reserve -> " + s.status.getText());
        kit.capture("t3_2_reserve.png", "Task 3.3 - second result selected and Reserve clicked: reservation message in the status bar", s);
        kit.type(s.query, "");
        kit.click(s.search);
        kit.click(s.next);
        System.out.println("  empty search (all 60) + Next -> " + s.status.getText() + " ; Prev enabled " + s.prev.isEnabled());
        kit.capture("t3_3_paging.png", "Task 3.3 - all 60 items, Next clicked: page 2 of 8, Prev now enabled", s);

        // ---------------- resizing test
        System.out.println("\n=== 3.4 Resizing: bounds of each region (x, y, width x height) ===");
        int[][] sizes = {{520, 340}, {800, 520}, {1100, 720}};
        Component[] regions = {s.title, s.searchRow, s.query, s.search, s.results, s.buttonRow, s.next, s.status};
        String[] names = {"title", "search row", "text field", "Search button", "results area", "button row", "Next button", "status"};
        try (PrintWriter csv = new PrintWriter("data/t3_resize_bounds.csv")) {
            csv.println("window,region,x,y,width,height");
            for (int[] sz : sizes) {
                ScreenshotKit.onEdt(() -> { s.setSize(sz[0], sz[1]); s.validate(); });
                kit.pause(500);
                System.out.printf("  window %dx%d%n", sz[0], sz[1]);
                for (int i = 0; i < regions.length; i++) {
                    Rectangle b = bounds(regions[i]);
                    System.out.printf("    %-14s %4d,%4d  %4d x %-4d%n", names[i], b.x, b.y, b.width, b.height);
                    csv.printf("%dx%d,%s,%d,%d,%d,%d%n", sz[0], sz[1], names[i], b.x, b.y, b.width, b.height);
                }
                kit.capture("t3_4_resize_" + sz[0] + "x" + sz[1] + ".png", "Task 3.4 - window " + sz[0] + " x " + sz[1]
                        + ": results area " + bounds(s.results).width + " x " + bounds(s.results).height
                        + "; Search button " + bounds(s.search).width + " x " + bounds(s.search).height + ", Next " + bounds(s.next).width + " x " + bounds(s.next).height, s);
            }
        }
        ScreenshotKit.onEdt(s::dispose);

        // ---------------- alternatives
        System.out.println("\n=== Alternatives at 1100 x 720 ===");
        Frame gb = ScreenshotKit.onEdt(() -> { Frame f = LibraryAlternatives.gridBag(); f.setLocation(40, 40); f.setVisible(true); return f; });
        kit.pause(600);
        ScreenshotKit.onEdt(() -> { gb.setSize(1100, 720); gb.validate(); });
        kit.capture("t3_alt_gridbag_1100x720.png", "Task 3 alternative - GridBagLayout at 1100 x 720: results grow (weighty = 1), other rows keep their height", gb);
        Frame ab = ScreenshotKit.onEdt(() -> { Frame f = LibraryAlternatives.absolute(); f.setLocation(40, 40); f.setVisible(true); return f; });
        kit.pause(600);
        ScreenshotKit.onEdt(() -> { ab.setSize(1100, 720); ab.validate(); });
        kit.capture("t3_alt_null_layout_1100x720.png", "Task 3 alternative - null layout at 1100 x 720: nothing moves or grows, empty space on the right and bottom", ab);
        ScreenshotKit.onEdt(() -> { ab.setSize(480, 300); ab.validate(); });
        kit.capture("t3_alt_null_layout_480x300.png", "Task 3 alternative - null layout at 480 x 300: Search button, list, buttons and status are cut off", ab);

        // layout computation time
        System.out.println("\n=== Layout computation time (invalidate + validate, mean of 2,000) and containers ===");
        LibrarySearchScreen n = ScreenshotKit.onEdt(() -> { LibrarySearchScreen w = new LibrarySearchScreen(cat); w.setLocation(40, 40); w.setVisible(true); return w; });
        kit.pause(500);
        Frame[] designs = {n, gb, ab};
        String[] dn = {"nested Border/Flow panels", "single GridBagLayout panel", "null layout (setBounds)"};
        try (PrintWriter csv = new PrintWriter("data/t3_layout_time.csv")) {
            csv.println("design,containers,components,us_per_layout");
            for (int d = 0; d < 3; d++) {
                Frame f = designs[d];
                double us = ScreenshotKit.onEdt(() -> {
                    f.setSize(800, 520);
                    for (int w = 0; w < 500; w++) { invalidateAll(f); f.validate(); }
                    long t0 = System.nanoTime();
                    for (int r = 0; r < 2000; r++) { invalidateAll(f); f.validate(); }
                    return (System.nanoTime() - t0) / 1e3 / 2000;
                });
                int[] cc = count(f);
                System.out.printf("  %-28s containers %2d  components %2d  %8.1f us per full layout%n", dn[d], cc[0], cc[1], us);
                csv.printf("%s,%d,%d,%.2f%n", dn[d], cc[0], cc[1], us);
            }
        }
        System.exit(0);
    }

    static void invalidateAll(Container c) { c.invalidate(); for (Component k : c.getComponents()) { k.invalidate(); if (k instanceof Container cc) invalidateAll(cc); } }

    /** {containers (Frame + Panels), leaf components} */
    static int[] count(Container c) {
        int containers = 1, leaves = 0;
        for (Component k : c.getComponents()) {
            if (k instanceof java.awt.Panel p) { int[] r = count(p); containers += r[0]; leaves += r[1]; } else leaves++;
        }
        return new int[]{containers, leaves};
    }

    static Rectangle bounds(Component c) {
        return ScreenshotKit.onEdt(() -> {
            java.awt.Point p = c.getLocationOnScreen(), f = c.getParent() instanceof Frame ? c.getParent().getLocationOnScreen() : frameOf(c).getLocationOnScreen();
            return new Rectangle(p.x - f.x, p.y - f.y, c.getWidth(), c.getHeight());
        });
    }
    static Container frameOf(Component c) { Container p = c.getParent(); while (!(p instanceof Frame)) p = p.getParent(); return p; }
}
