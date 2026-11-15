import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Application logic for Task 3 - kept out of the window: search, paging and reservation. */
public final class LibraryCatalog {
    public record Item(String id, String title, String author, String category, int copies) {
        public String row() { return String.format("%-6s %-48s %-12s %-9s copies %d", id, title, author, category, copies); }
    }

    private final List<Item> items = new ArrayList<>();
    public static final int PAGE_SIZE = 8;

    public LibraryCatalog(Path csv) throws IOException {
        for (String l : Files.readAllLines(csv)) {
            if (l.isBlank() || l.startsWith("id")) continue;
            String[] p = l.split(",");
            items.add(new Item(p[0], p[1], p[2], p[3], Integer.parseInt(p[4].trim())));
        }
    }

    public List<Item> search(String text, String category) {
        String q = text.trim().toLowerCase(Locale.ROOT);
        List<Item> r = new ArrayList<>();
        for (Item i : items)
            if ((category.equals("All") || i.category().equals(category))
                    && (q.isEmpty() || i.title().toLowerCase(Locale.ROOT).contains(q) || i.author().toLowerCase(Locale.ROOT).contains(q)))
                r.add(i);
        return r;
    }

    public static int pages(int count) { return Math.max(1, (count + PAGE_SIZE - 1) / PAGE_SIZE); }

    public String reserve(Item i) {
        if (i.copies() == 0) return "No copy of " + i.id() + " is available - added to the waiting list";
        return "Reserved " + i.id() + " '" + i.title() + "' - collect within 2 days";
    }
}
