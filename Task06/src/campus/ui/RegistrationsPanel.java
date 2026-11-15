package campus.ui;

import campus.model.Event;
import campus.model.Page;
import campus.service.RegistrationService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;

/** Screen 2 - registrations table, filter by event, paging (LIMIT/OFFSET in a SwingWorker), export. */
public class RegistrationsPanel extends JPanel {
    public final JComboBox<Object> filter = new JComboBox<>(new Object[]{"All events"});
    public final JButton refresh = new JButton("Refresh"), prev = new JButton("< Prev"), next = new JButton("Next >"), export = new JButton("Export CSV...");
    public final JLabel pageLabel = new JLabel("No data loaded");
    public final JProgressBar loading = new JProgressBar();
    public final RegistrationTableModel model = new RegistrationTableModel();
    public final JTable table = new JTable(model);
    private final RegistrationService service;
    private final Consumer<String> status;
    private Page current;
    private int generation;                        // ignores results of an older, slower load
    private boolean listening = true;

    public RegistrationsPanel(RegistrationService service, Consumer<String> status) {
        super(new BorderLayout(0, 6));
        this.service = service; this.status = status;
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.add(new JLabel("Event:")); top.add(filter); top.add(refresh);
        loading.setIndeterminate(true); loading.setVisible(false); loading.setStringPainted(true); loading.setString("Loading...");
        top.add(loading);
        filter.setPrototypeDisplayValue("Inter-college Debate (2026-12-04)  ");
        table.setAutoCreateRowSorter(false);
        table.setFillsViewportHeight(true);
        int[] widths = {40, 80, 150, 210, 50, 40, 150, 120};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        JPanel bottom = new JPanel(new BorderLayout());
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        paging.add(prev); paging.add(next); paging.add(pageLabel);
        bottom.add(paging, BorderLayout.WEST);
        bottom.add(export, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        filter.addItemListener(e -> { if (listening && e.getStateChange() == java.awt.event.ItemEvent.SELECTED) load(0); });   // Filter action
        refresh.addActionListener(e -> load(current == null ? 0 : current.pageIndex()));
        prev.addActionListener(e -> load(current.pageIndex() - 1));
        next.addActionListener(e -> load(current.pageIndex() + 1));
        prev.setEnabled(false); next.setEnabled(false);
    }

    public void setEvents(List<Event> events) {
        listening = false;
        filter.removeAllItems();
        filter.addItem("All events");
        for (Event e : events) filter.addItem(e);
        listening = true;
    }

    public Integer selectedEventId() { return filter.getSelectedItem() instanceof Event e ? e.id() : null; }

    /** Filter / paging action: the query runs in a SwingWorker; the EDT only swaps in the finished page. */
    public void load(int pageIndex) {
        Integer eventId = selectedEventId();
        int gen = ++generation;
        setBusy(true);
        status.accept("Loading registrations" + (eventId == null ? "" : " for " + filter.getSelectedItem()) + "...");
        new SwingWorker<Page, Void>() {
            @Override protected Page doInBackground() throws Exception { return service.page(eventId, pageIndex); }
            @Override protected void done() {
                if (gen != generation) return;                  // a newer request superseded this one
                setBusy(false);
                try {
                    current = get();
                    model.setRows(current.rows());
                    pageLabel.setText("Page " + (current.pageIndex() + 1) + " of " + current.pageCount() + "  (" + current.totalRows() + " registrations)");
                    prev.setEnabled(current.hasPrevious()); next.setEnabled(current.hasNext());
                    status.accept("Showing " + current.rows().size() + " of " + current.totalRows() + " registrations"
                            + (eventId == null ? "" : " for " + filter.getSelectedItem()));
                } catch (ExecutionException ex) {
                    status.accept("Load failed: " + ex.getCause().getMessage());
                    JOptionPane.showMessageDialog(RegistrationsPanel.this, ex.getCause().getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        loading.setVisible(busy);
        refresh.setEnabled(!busy); export.setEnabled(!busy);
        if (busy) { prev.setEnabled(false); next.setEnabled(false); }
        revalidate();
    }

    /** Export action (all pages of the current filter) in a SwingWorker. */
    public void exportTo(Path file) {
        Integer eventId = selectedEventId();
        setBusy(true);
        status.accept("Exporting to " + file.getFileName() + "...");
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { return service.exportCsv(eventId, file); }
            @Override protected void done() {
                setBusy(false);
                if (current != null) { prev.setEnabled(current.hasPrevious()); next.setEnabled(current.hasNext()); }
                try { status.accept("Exported " + get() + " registrations to " + file.getFileName()); }
                catch (ExecutionException ex) { status.accept("Export failed: " + ex.getCause().getMessage()); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        }.execute();
    }
}
