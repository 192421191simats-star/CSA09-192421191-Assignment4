import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Task 2 - Hospital Outpatient Appointment Desk (Swing). The window only collects input and shows
 * results; AppointmentService does validation and booking. Every event method writes a trace line.
 *
 * Run: java -cp out AppointmentDesk input/doctors.csv
 */
public class AppointmentDesk extends JFrame implements WindowListener {
    final JTextField patientId = new JTextField(12);
    final JComboBox<String> department = new JComboBox<>();
    final DefaultListModel<AppointmentService.Doctor> doctorModel = new DefaultListModel<>();
    final JList<AppointmentService.Doctor> doctors = new JList<>(doctorModel);
    final JComboBox<String> slot = new JComboBox<>();
    final JButton book = new JButton("Book appointment");
    final JButton clear = new JButton("Clear");
    final JLabel status = new JLabel(" ");
    private final AppointmentService service;
    final List<String> trace = new ArrayList<>();
    private final long t0 = System.nanoTime();
    private boolean bookingInProgress;              // something typed/selected but not yet booked

    void trace(String s) {
        String line = String.format("[%6.0f ms] %s", (System.nanoTime() - t0) / 1e6, s);
        trace.add(line);
        System.out.println("  TRACE " + line);
    }

    public AppointmentDesk(AppointmentService service) {
        super("Outpatient Appointment Desk");
        this.service = service;
        trace("constructor: building components");
        JLabel header = new JLabel("  Outpatient Appointment Desk");
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        header.setOpaque(true); header.setBackground(new Color(0x1f3a68)); header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5); g.anchor = GridBagConstraints.WEST;
        addRow(form, g, 0, "Patient ID:", patientId);
        department.addItem("-- select --");
        for (String d : service.departments()) department.addItem(d);
        addRow(form, g, 1, "Department:", department);
        doctors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        doctors.setVisibleRowCount(4);
        JScrollPane docScroll = new JScrollPane(doctors);
        docScroll.setPreferredSize(new java.awt.Dimension(260, 82));
        addRow(form, g, 2, "Doctor:", docScroll);
        slot.setPrototypeDisplayValue("00:00 (free)");
        addRow(form, g, 3, "Time slot:", slot);

        JPanel buttons = new JPanel();
        buttons.add(book); buttons.add(clear);
        status.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        JPanel south = new JPanel(new BorderLayout());
        south.add(buttons, BorderLayout.NORTH);
        south.add(status, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // ---------------- event wiring
        department.addItemListener(e -> {                                  // ItemEvent
            trace("itemStateChanged(department, " + (e.getStateChange() == ItemEvent.SELECTED ? "SELECTED " : "DESELECTED ") + e.getItem() + ")");
            if (e.getStateChange() == ItemEvent.SELECTED) refreshDoctors();
        });
        doctors.addListSelectionListener(e -> {                            // ListSelectionEvent
            if (e.getValueIsAdjusting()) return;
            trace("valueChanged(doctor list) -> " + doctors.getSelectedValue());
            refreshSlots();
        });
        slot.addActionListener(e -> { trace("actionPerformed(slot) -> " + slot.getSelectedItem()); bookingInProgress = true; });
        patientId.getDocument().addDocumentListener(new DocumentListener() { // DocumentEvent
            public void insertUpdate(DocumentEvent e) { bookingInProgress = true; }
            public void removeUpdate(DocumentEvent e) { bookingInProgress = !patientId.getText().isEmpty(); }
            public void changedUpdate(DocumentEvent e) { }
        });
        book.addActionListener(e -> { trace("actionPerformed(Book)"); doBook(); });
        clear.addActionListener(e -> { trace("actionPerformed(Clear)"); clearForm(); });
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);    // we decide in windowClosing
        addWindowListener(this);                                           // WindowEvent
        pack();
        trace("constructor finished: pack() done");
    }

    private static void addRow(JPanel p, GridBagConstraints g, int row, String label, java.awt.Component c) {
        g.gridx = 0; g.gridy = row; g.fill = GridBagConstraints.NONE; g.weightx = 0; p.add(new JLabel(label), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; p.add(c, g);
    }

    void refreshDoctors() {
        doctorModel.clear();
        slot.removeAllItems();
        Object d = department.getSelectedItem();
        for (AppointmentService.Doctor doc : service.doctorsOf((String) d)) doctorModel.addElement(doc);
        bookingInProgress = true;
        trace("  refreshDoctors(): " + doctorModel.size() + " doctor(s) for " + d);
    }

    void refreshSlots() {
        slot.removeAllItems();
        AppointmentService.Doctor d = doctors.getSelectedValue();
        if (d != null) for (String s : service.freeSlots(d)) slot.addItem(s);
        trace("  refreshSlots(): " + slot.getItemCount() + " free slot(s)");
    }

    void doBook() {
        Object dep = department.getSelectedIndex() <= 0 ? null : department.getSelectedItem();
        AppointmentService.Result r = service.book(patientId.getText(), (String) dep, doctors.getSelectedValue(), (String) slot.getSelectedItem());
        status.setForeground(r.ok() ? new Color(0x006300) : new Color(0xb00020));
        status.setText((r.ok() ? "✔ " : "✖ ") + r.message());
        trace("  book() -> " + (r.ok() ? "CONFIRMED: " : "ERROR: ") + r.message());
        if (r.ok()) { bookingInProgress = false; refreshSlots(); bookingInProgress = false; }
    }

    void clearForm() {
        patientId.setText(""); department.setSelectedIndex(0); doctorModel.clear(); slot.removeAllItems();
        status.setText(" "); bookingInProgress = false;
    }

    // ---------------- WindowListener: every method traced
    @Override public void windowOpened(WindowEvent e) { trace("windowOpened"); }
    @Override public void windowActivated(WindowEvent e) { trace("windowActivated"); }
    @Override public void windowDeactivated(WindowEvent e) { trace("windowDeactivated"); }
    @Override public void windowIconified(WindowEvent e) { trace("windowIconified"); }
    @Override public void windowDeiconified(WindowEvent e) { trace("windowDeiconified"); }
    @Override public void windowClosing(WindowEvent e) {
        trace("windowClosing (booking in progress = " + bookingInProgress + ")");
        if (bookingInProgress) {
            int a = JOptionPane.showConfirmDialog(this, "A booking is in progress. Close the desk and discard it?",
                    "Confirm close", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            trace("  confirm dialog answered: " + (a == JOptionPane.YES_OPTION ? "YES" : "NO"));
            if (a != JOptionPane.YES_OPTION) return;                         // stay open
        }
        dispose();                                                          // -> windowClosed
    }
    @Override public void windowClosed(WindowEvent e) { trace("windowClosed"); }

    public static void main(String[] args) throws IOException {
        AppointmentService s = new AppointmentService(Path.of(args.length > 0 ? args[0] : "input/doctors.csv"));
        SwingUtilities.invokeLater(() -> new AppointmentDesk(s).setVisible(true));
    }
}
