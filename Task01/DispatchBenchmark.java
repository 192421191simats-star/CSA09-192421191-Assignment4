import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Task 1 alternative concept: one listener with an if-else chain on the action command (up to k string
 * comparisons) versus one listener object per button (direct call). Also the memory of k listeners.
 *
 * Run: java -cp out DispatchBenchmark
 */
public class DispatchBenchmark {
    static long counter;

    @SuppressWarnings("deprecation")
    static long allocated() { return ((com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().getId()); }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("data"));
        System.out.printf("%-6s %26s %24s %22s%n", "k", "if-else chain: ns/event", "own listener: ns/event", "bytes for k listeners");
        try (PrintWriter csv = new PrintWriter("data/t1_dispatch.csv")) {
            csv.println("k,chain_ns,direct_ns,listener_bytes");
            for (int w = 0; w < 1000; w++) { final int id = w; ActionListener warm = e -> counter += id; if (warm == null) return; }   // load the lambda class first
            for (int k : new int[]{3, 10, 50, 100}) {
                String[] cmds = new String[k];
                for (int i = 0; i < k; i++) cmds[i] = "Command-" + i;
                // shared listener: compares the command with each known command in turn
                ActionListener chain = e -> {
                    String c = e.getActionCommand();
                    for (int i = 0; i < k; i++) if (cmds[i].equals(c)) { counter += i; return; }
                };
                // one listener object per button
                ActionListener[] own = null;
                long bytes = Long.MAX_VALUE;           // k listener objects + the array that holds them (min of 5 tries)
                for (int t = 0; t < 5; t++) {
                    long a0 = allocated();
                    own = new ActionListener[k];
                    for (int i = 0; i < k; i++) { final int id = i; own[i] = e -> counter += id; }
                    bytes = Math.min(bytes, allocated() - a0);
                }
                int n = 2_000_000;
                Random r = new Random(1);
                ActionEvent[] evs = new ActionEvent[1024];
                int[] which = new int[1024];
                for (int i = 0; i < 1024; i++) { which[i] = r.nextInt(k); evs[i] = new ActionEvent(new Object(), ActionEvent.ACTION_PERFORMED, new String(cmds[which[i]])); }
                for (int w = 0; w < 3; w++) { run(chain, evs, null, which, n / 4); run(null, evs, own, which, n / 4); }
                long t0 = System.nanoTime(); run(chain, evs, null, which, n);
                long t1 = System.nanoTime(); run(null, evs, own, which, n);
                long t2 = System.nanoTime();
                System.out.printf("%-6d %26.2f %24.2f %22d%n", k, (t1 - t0) / (double) n, (t2 - t1) / (double) n, bytes);
                csv.printf("%d,%.3f,%.3f,%d%n", k, (t1 - t0) / (double) n, (t2 - t1) / (double) n, bytes);
            }
        }
        System.out.println("(events pick a random button; the chain needs (k+1)/2 string comparisons on average)  checksum " + (counter & 0xff));
    }

    static void run(ActionListener chain, ActionEvent[] evs, ActionListener[] own, int[] which, int n) {
        for (int i = 0; i < n; i++) {
            int j = i & 1023;
            if (chain != null) chain.actionPerformed(evs[j]); else own[which[j]].actionPerformed(evs[j]);
        }
    }
}
