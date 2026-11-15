import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Task 3 - Campus Library Search Screen (AWT).
 *
 *  Frame                         BorderLayout
 *   +- NORTH  top                 BorderLayout
 *   |    +- NORTH  title          Label
 *   |    +- SOUTH  searchRow      FlowLayout(LEFT): Label, TextField, Choice, Button "Search"
 *   +- CENTER results             List            (gets ALL extra space when resized)
 *   +- SOUTH  bottom              BorderLayout
 *        +- CENTER buttonRow      FlowLayout(CENTER): Prev, Next, Reserve
 *        +- SOUTH  status         Label
 *
 * Run: java -cp out LibrarySearchScreen input/catalogue.csv
 */
public class LibrarySearchScreen extends Frame {
    final Label title = new Label("Campus Library - Catalogue Search", Label.CENTER);
    final TextField query = new TextField(28);
    final Choice category = new Choice();
    final Button search = new Button("Search");
    final List results = new List(10, false);
    final Button prev = new Button("Prev"), next = new Button("Next"), reserve = new Button("Reserve");
    final Label status = new Label("Type a title or author and press Search.");
    final Panel top = new Panel(new BorderLayout()), searchRow = new Panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    final Panel bottom = new Panel(new BorderLayout()), buttonRow = new Panel(new FlowLayout(FlowLayout.CENTER, 12, 6));

    private final LibraryCatalog catalog;
    private java.util.List<LibraryCatalog.Item> found = java.util.List.of();
    private int page;

    public LibrarySearchScreen(LibraryCatalog catalog) {
        super("Campus Library Search");
        this.catalog = catalog;
        setLayout(new BorderLayout());

        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setBackground(new Color(0x1f3a68)); title.setForeground(Color.WHITE);
        for (String c : new String[]{"All", "Books", "Journals", "E-books", "Theses"}) category.add(c);
        searchRow.add(new Label("Title / author:")); searchRow.add(query); searchRow.add(category); searchRow.add(search);
        top.add(title, BorderLayout.NORTH);
        top.add(searchRow, BorderLayout.SOUTH);

        results.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        buttonRow.add(prev); buttonRow.add(next); buttonRow.add(reserve);
        status.setBackground(new Color(0xeeeeee));
        bottom.add(buttonRow, BorderLayout.CENTER);
        bottom.add(status, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(results, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        search.addActionListener(e -> doSearch());
        query.addActionListener(e -> doSearch());                  // Enter in the text field
        prev.addActionListener(e -> showPage(page - 1));
        next.addActionListener(e -> showPage(page + 1));
        reserve.addActionListener(e -> doReserve());
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
        setSize(640, 420);
        updateButtons();
    }

    void doSearch() {
        found = catalog.search(query.getText(), category.getSelectedItem());
        showPage(0);
        status.setText(found.size() + " item(s) found for '" + query.getText().trim() + "' in " + category.getSelectedItem());
    }

    void showPage(int p) {
        int pages = LibraryCatalog.pages(found.size());
        page = Math.max(0, Math.min(p, pages - 1));
        results.removeAll();
        int from = page * LibraryCatalog.PAGE_SIZE;
        for (int i = from; i < Math.min(found.size(), from + LibraryCatalog.PAGE_SIZE); i++) results.add(found.get(i).row());
        if (!found.isEmpty()) status.setText("Page " + (page + 1) + " of " + pages + " (" + found.size() + " items)");
        updateButtons();
    }

    void doReserve() {
        int sel = results.getSelectedIndex();
        if (sel < 0) { status.setText("Select an item in the results first."); return; }
        status.setText(catalog.reserve(found.get(page * LibraryCatalog.PAGE_SIZE + sel)));
    }

    void updateButtons() {
        prev.setEnabled(page > 0);
        next.setEnabled((page + 1) * LibraryCatalog.PAGE_SIZE < found.size());
        reserve.setEnabled(!found.isEmpty());
    }

    public static void main(String[] args) throws IOException {
        LibraryCatalog c = new LibraryCatalog(Path.of(args.length > 0 ? args[0] : "input/catalogue.csv"));
        new LibrarySearchScreen(c).setVisible(true);
    }
}
