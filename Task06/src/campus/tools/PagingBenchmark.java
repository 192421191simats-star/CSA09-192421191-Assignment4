package campus.tools;

import campus.dao.DataAccessException;
import campus.dao.RegistrationDao;
import campus.db.ConnectionFactory;
import campus.model.Registration;
import campus.ui.RegistrationTableModel;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * Task 6 alternative concept: 100,000 registrations loaded into the JTable all at once versus one page
 * (LIMIT 20 OFFSET k) at a time in a SwingWorker. Measures loading time, retained heap and the
 * worst delay seen by the event-dispatch thread (window responsiveness) while loading.
 *
 * Run: java -Xmx1g -cp out campus.tools.PagingBenchmark          (test-double driver)
 *      java -Xmx1g -cp out campus.tools.PagingBenchmark "jdbc:campusdb:bench?schema=sql/schema.sql&latencyMs=1&rowMicros=5" network
 *      java -Xmx1g -cp out:lib/mysql-connector-j.jar campus.tools.PagingBenchmark db.properties mysql   (inserts 100,000 rows)
 */
public class PagingBenchmark {
    static final int N = 100_000;
    static RegistrationTableModel model;

    public static void main(String[] args) throws Exception {
        // args[0]: a properties file, or a JDBC URL of the test double; args[1]: tag for the CSV name
        String target = args.length > 0 ? args[0] : "jdbc:campusdb:bench?schema=sql/schema.sql";
        ConnectionFactory db = target.startsWith("jdbc:") ? new ConnectionFactory(target, "b", "b") : new ConnectionFactory(Path.of(target));
        RegistrationDao dao = new RegistrationDao(db);
        if (dao.count(null) < N) {
            List<Registration> rows = new ArrayList<>(N);
            LocalDateTime t = LocalDateTime.of(2026, 11, 1, 8, 0);
            for (int i = 0; i < N; i++)
                rows.add(new Registration(0, String.valueOf(190_000_000 + i), "Student " + i, "student" + i + "@saveetha.ac.in",
                        "CSE", 1 + i % 4, 1 + i % 6, null, t.plusSeconds(i * 17L)));
            long t0 = System.nanoTime();
            dao.insertAll(rows);
            System.out.printf("seeded %,d registrations in %.0f ms (JDBC batch)%n", N, (System.nanoTime() - t0) / 1e6);
        }
        SwingUtilities.invokeAndWait(() -> { model = new RegistrationTableModel(); new JTable(model); });
        Files.createDirectories(Path.of("data"));

        // warm-up of every path
        for (int w = 0; w < 3; w++) { loadAll(dao); dao.page(null, 20, 0); dao.page(null, 20, N / 2); }

        int reps = 5;
        double[] allEdt = new double[reps], allEdtLag = new double[reps], allWorker = new double[reps], allWorkerLag = new double[reps], allSet = new double[reps];
        double[] cnt = new double[reps], p0 = new double[reps], pMid = new double[reps], pLast = new double[reps], pLag = new double[reps];
        for (int r = 0; r < reps; r++) {
            // (a) naive: all rows loaded inside the event handler (on the EDT)
            Probe pr = Probe.begin();
            long t0 = System.nanoTime();
            SwingUtilities.invokeAndWait(() -> { try { model.setRows(loadAll(dao)); } catch (DataAccessException e) { throw new RuntimeException(e); } });
            allEdt[r] = ms(t0); allEdtLag[r] = pr.finish();
            // (b) all rows, but in a background thread; the EDT only swaps the list in
            pr = Probe.begin();
            t0 = System.nanoTime();
            List<Registration> all = loadAll(dao);
            long t1 = System.nanoTime();
            SwingUtilities.invokeAndWait(() -> model.setRows(all));
            allSet[r] = ms(t1); allWorker[r] = ms(t0); allWorkerLag[r] = pr.finish();
            // (c) paged in a background thread
            pr = Probe.begin();
            t0 = System.nanoTime(); List<Registration> first = dao.page(null, 20, 0); p0[r] = ms(t0);
            t0 = System.nanoTime(); int c = dao.count(null); cnt[r] = ms(t0);
            SwingUtilities.invokeAndWait(() -> model.setRows(first));
            t0 = System.nanoTime(); dao.page(null, 20, N / 2); pMid[r] = ms(t0);
            t0 = System.nanoTime(); dao.page(null, 20, N - 20); pLast[r] = ms(t0);
            pLag[r] = pr.finish();
            if (c != N) throw new IllegalStateException("count " + c);
        }
        long heapAll = retained(() -> loadAll(dao));
        long heapPage = retained(() -> dao.page(null, 20, 0));

        String tag = args.length > 1 ? args[1] : "inmemory";
        try (PrintWriter out = new PrintWriter("data/t6_paging_" + tag + ".csv")) {
            out.println("approach,load_ms,edt_max_delay_ms,retained_heap_kb,rows_in_memory");
            row(out, "all at once on the EDT", med(allEdt), med(allEdtLag), heapAll, N);
            row(out, "all at once in a SwingWorker", med(allWorker), med(allWorkerLag), heapAll, N);
            row(out, "paged LIMIT 20 OFFSET 0 (SwingWorker)", med(p0), med(pLag), heapPage, 20);
            row(out, "paged LIMIT 20 OFFSET 50000", med(pMid), med(pLag), heapPage, 20);
            row(out, "paged LIMIT 20 OFFSET 99980", med(pLast), med(pLag), heapPage, 20);
            row(out, "COUNT(*) for the page indicator", med(cnt), med(pLag), 0, 0);
        }
        System.out.printf("%n%-40s %12s %18s %18s %10s%n", "approach (" + String.format("%,d", N) + " rows, median of " + reps + ")", "load ms", "EDT max delay ms", "retained heap", "rows held");
        System.out.printf("%-40s %12.1f %18.1f %15.1f MB %10s%n", "all at once on the EDT", med(allEdt), med(allEdtLag), heapAll / 1048576.0, "100,000");
        System.out.printf("%-40s %12.1f %18.1f %15.1f MB %10s%n", "all at once in a SwingWorker", med(allWorker), med(allWorkerLag), heapAll / 1048576.0, "100,000");
        System.out.printf("%-40s %12.3f %18.1f %15.1f KB %10s%n", "paged, first page (OFFSET 0)", med(p0), med(pLag), heapPage / 1024.0, "20");
        System.out.printf("%-40s %12.3f %18s %18s %10s%n", "paged, middle page (OFFSET 50,000)", med(pMid), "", "", "20");
        System.out.printf("%-40s %12.3f %18s %18s %10s%n", "paged, last page (OFFSET 99,980)", med(pLast), "", "", "20");
        System.out.printf("%-40s %12.3f%n", "COUNT(*) for 'Page x of y'", med(cnt));
        System.out.printf("(model.setRows of 100,000 rows on the EDT: %.2f ms - JTable paints only the visible rows)%n", med(allSet));
        System.out.println("Raw data: data/t6_paging_" + tag + ".csv  (driver: " + db.url() + ")");
        System.exit(0);
    }

    static List<Registration> loadAll(RegistrationDao dao) throws DataAccessException {
        List<Registration> all = new ArrayList<>();
        dao.forEach(null, all::add);
        return all;
    }

    interface Load { Object get() throws Exception; }
    static long retained(Load l) throws Exception {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 3; i++) { System.gc(); Thread.sleep(100); }
        long before = rt.totalMemory() - rt.freeMemory();
        Object keep = l.get();
        for (int i = 0; i < 3; i++) { System.gc(); Thread.sleep(100); }
        long after = rt.totalMemory() - rt.freeMemory();
        if (keep.hashCode() == 42) System.out.print("");
        return Math.max(0, after - before);
    }

    static void row(PrintWriter o, String a, double ms, double lag, long heap, int rows) { o.printf("%s,%.3f,%.1f,%.1f,%d%n", a, ms, lag, heap / 1024.0, rows); }
    static double ms(long t0) { return (System.nanoTime() - t0) / 1e6; }
    static double med(double[] a) { double[] c = a.clone(); Arrays.sort(c); return c[c.length / 2]; }

    /** Posts an empty task to the EDT every 5 ms and records the longest wait = how long the window was frozen. */
    static final class Probe extends Thread {
        final AtomicLong maxNs = new AtomicLong();
        volatile boolean running = true;
        static Probe begin() { Probe p = new Probe(); p.setDaemon(true); p.start(); return p; }
        @Override public void run() {
            while (running) {
                long t0 = System.nanoTime();
                try { SwingUtilities.invokeAndWait(() -> { }); } catch (Exception e) { return; }
                maxNs.accumulateAndGet(System.nanoTime() - t0, Math::max);
                try { Thread.sleep(5); } catch (InterruptedException e) { return; }
            }
        }
        double finish() throws InterruptedException { running = false; join(); return maxNs.get() / 1e6; }
    }
}
