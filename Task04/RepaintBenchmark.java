import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Task 4 alternative concept: time per repaint for n stored points (O(n) fillOval calls) versus
 * copying one 400 x 300 off-screen image (O(width x height), independent of n).
 * Painting is done into an image so the measurement is the same with or without a display.
 *
 * Run: java -cp out RepaintBenchmark
 */
public class RepaintBenchmark {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("data"));
        BufferedImage screen = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
        System.out.printf("%-10s %22s %26s %18s %18s%n", "clicks n", "stored points: ms/paint", "BufferedImage copy: ms/paint", "points memory KB", "image memory KB");
        try (PrintWriter csv = new PrintWriter("data/t4_repaint.csv")) {
            csv.println("n,run,points_ms,image_ms");
            for (int n : new int[]{10, 100, 1_000, 10_000, 100_000}) {
                List<Point> pts = new ArrayList<>(n);
                Random r = new Random(n);
                Graphics2D cg = canvas.createGraphics();
                for (int i = 0; i < n; i++) { Point p = new Point(r.nextInt(400), r.nextInt(300)); pts.add(p); cg.fillOval(p.x - 3, p.y - 3, 6, 6); }
                cg.dispose();
                double a = 0, b = 0;
                int reps = n >= 100_000 ? 5 : 20;
                for (int w = 0; w < 3; w++) { paintPoints(screen, pts); paintImage(screen, canvas); }
                for (int k = 0; k < reps; k++) {
                    long t0 = System.nanoTime(); paintPoints(screen, pts);
                    long t1 = System.nanoTime(); paintImage(screen, canvas);
                    long t2 = System.nanoTime();
                    a += (t1 - t0) / 1e6; b += (t2 - t1) / 1e6;
                    csv.printf("%d,%d,%.4f,%.4f%n", n, k + 1, (t1 - t0) / 1e6, (t2 - t1) / 1e6);
                }
                long pointBytes = n * (16L + 8 + 4);         // Point object (header + 2 ints) + reference in the list
                System.out.printf("%-10d %22.3f %26.3f %18.1f %18.1f%n", n, a / reps, b / reps, pointBytes / 1024.0, 400 * 300 * 4 / 1024.0);
            }
        }
        System.out.println("Raw data: data/t4_repaint.csv");
    }
    static void paintPoints(BufferedImage screen, List<Point> pts) {
        Graphics2D g = screen.createGraphics();
        for (Point p : pts) g.fillOval(p.x - 3, p.y - 3, 6, 6);
        g.dispose();
    }
    static void paintImage(BufferedImage screen, BufferedImage canvas) {
        Graphics2D g = screen.createGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
    }
}
