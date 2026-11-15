import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Task 2 alternative concept: time per department change when the doctor list comes from a Map loaded
 * once, versus fetching it from storage on every change. "Storage" here is a real file read and scan
 * (like a query without an index) plus an optional simulated network round trip to a database server.
 *
 * Run: java -cp out DoctorLookupBenchmark
 */
public class DoctorLookupBenchmark {
    record Doctor(String id, String name, String dept) { }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("data"));
        System.out.printf("%-6s %-8s %16s %22s %26s %14s%n", "d", "m", "map: us/change", "re-read file: us/change", "+2 ms DB round trip: us", "map memory KB");
        try (PrintWriter csv = new PrintWriter("data/t2_lookup.csv")) {
            csv.println("departments,doctors,map_us,reread_us,reread_plus_rtt_us,map_kb");
            for (int[] dm : new int[][]{{5, 11}, {20, 200}, {50, 2_000}, {100, 10_000}}) {
                int d = dm[0], m = dm[1];
                Path file = Files.createTempFile("doctors", ".csv");
                List<String> lines = new ArrayList<>();
                for (int i = 0; i < m; i++) lines.add("Dept" + (i % d) + ",D" + i + ",Doctor " + i);
                Files.write(file, lines);
                Runtime rt = Runtime.getRuntime();
                System.gc(); long before = rt.totalMemory() - rt.freeMemory();
                Map<String, List<Doctor>> map = load(file);                          // loaded ONCE
                System.gc(); long after = rt.totalMemory() - rt.freeMemory();
                Random rnd = new Random(1);
                int changes = 2_000;
                long sink = 0;
                for (int w = 0; w < 200; w++) { sink += map.get("Dept" + rnd.nextInt(d)).size(); sink += query(file, "Dept" + rnd.nextInt(d)).size(); }
                long t0 = System.nanoTime();
                for (int i = 0; i < changes; i++) sink += map.get("Dept" + rnd.nextInt(d)).size();
                double mapUs = (System.nanoTime() - t0) / 1e3 / changes;
                int qn = 300;
                t0 = System.nanoTime();
                for (int i = 0; i < qn; i++) sink += query(file, "Dept" + rnd.nextInt(d)).size();
                double qUs = (System.nanoTime() - t0) / 1e3 / qn;
                int rn = 50;
                t0 = System.nanoTime();
                for (int i = 0; i < rn; i++) { Thread.sleep(2); sink += query(file, "Dept" + rnd.nextInt(d)).size(); }
                double rUs = (System.nanoTime() - t0) / 1e3 / rn;
                double kb = Math.max(0, after - before) / 1024.0;
                System.out.printf("%-6d %-8d %16.3f %22.1f %26.1f %14.1f%n", d, m, mapUs, qUs, rUs, kb);
                csv.printf("%d,%d,%.4f,%.2f,%.2f,%.1f%n", d, m, mapUs, qUs, rUs, kb);
                Files.delete(file);
                if (sink == 42) System.out.print("");
            }
        }
        System.out.println("Raw data: data/t2_lookup.csv");
    }

    static Map<String, List<Doctor>> load(Path f) throws IOException {
        Map<String, List<Doctor>> m = new HashMap<>();
        for (String l : Files.readAllLines(f)) { String[] p = l.split(","); m.computeIfAbsent(p[0], k -> new ArrayList<>()).add(new Doctor(p[1], p[2], p[0])); }
        return m;
    }
    static List<Doctor> query(Path f, String dept) throws IOException {
        List<Doctor> r = new ArrayList<>();
        for (String l : Files.readAllLines(f)) { String[] p = l.split(","); if (p[0].equals(dept)) r.add(new Doctor(p[1], p[2], p[0])); }
        return r;
    }
}
