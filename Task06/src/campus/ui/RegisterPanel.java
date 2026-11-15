package campus.ui;

import campus.dao.DataAccessException;
import campus.model.Event;
import campus.model.Registration;
import campus.model.RegistrationForm;
import campus.service.DuplicateRegistrationException;
import campus.service.RegistrationService;
import campus.service.ValidationException;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** Screen 1 - the registration form. Collects input, shows results; the service decides. */
public class RegisterPanel extends JPanel {
    public final JTextField regNo = new JTextField(14), name = new JTextField(24), email = new JTextField(24);
    public final JComboBox<String> dept = new JComboBox<>(), year = new JComboBox<>(new String[]{"-- select --", "1", "2", "3", "4"});
    public final JComboBox<Object> event = new JComboBox<>(new Object[]{"Loading events..."});
    public final JButton register = new JButton("Register"), clear = new JButton("Clear");
    public final JLabel message = new JLabel(" ");
    private final Map<String, JComponent> fieldOf = new HashMap<>();
    private final Border normalText = UIManager.getBorder("TextField.border");
    private final RegistrationService service;
    private final Consumer<String> status;
    private Runnable onRegistered = () -> { };

    public RegisterPanel(RegistrationService service, Consumer<String> status) {
        super(new GridBagLayout());
        this.service = service; this.status = status;
        setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        dept.addItem("-- select --");
        for (String d : RegistrationService.DEPARTMENTS) dept.addItem(d);
        event.setPrototypeDisplayValue("Inter-college Debate (2026-12-04)  ");
        fieldOf.put("regNo", regNo); fieldOf.put("name", name); fieldOf.put("email", email);
        fieldOf.put("dept", dept); fieldOf.put("year", year); fieldOf.put("event", event);

        JLabel title = new JLabel("Register for an event");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(0, 4, 10, 4);
        add(title, g);
        String[] labels = {"Register No:", "Student name:", "E-mail:", "Department:", "Year:", "Event:"};
        JComponent[] fields = {regNo, name, email, dept, year, event};
        g.gridwidth = 1; g.insets = new Insets(4, 4, 4, 4);
        for (int i = 0; i < labels.length; i++) {
            g.gridy = i + 1; g.gridx = 0; g.fill = GridBagConstraints.NONE; g.weightx = 0; add(new JLabel(labels[i]), g);
            g.gridx = 1; g.fill = GridBagConstraints.NONE; g.anchor = GridBagConstraints.WEST; g.weightx = 1; add(fields[i], g);
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.add(register); buttons.add(javax.swing.Box.createHorizontalStrut(8)); buttons.add(clear);
        g.gridy = 7; g.gridx = 1; g.fill = GridBagConstraints.NONE; g.insets = new Insets(10, 4, 4, 4); add(buttons, g);
        g.gridy = 8; g.gridx = 0; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL; add(message, g);
        g.gridy = 9; g.weighty = 1; add(new JLabel(" "), g);

        register.addActionListener(e -> onRegister());
        clear.addActionListener(e -> clearForm());
        register.setEnabled(false);                         // until the events have loaded
    }

    public void setOnRegistered(Runnable r) { onRegistered = r; }

    public void setEvents(List<Event> events) {
        event.removeAllItems();
        event.addItem("-- select --");
        for (Event e : events) event.addItem(e);
        register.setEnabled(true);
    }

    RegistrationForm form() {
        Object ev = event.getSelectedItem();
        return new RegistrationForm(regNo.getText(), name.getText(), email.getText(), (String) dept.getSelectedItem(),
                (String) year.getSelectedItem(), ev instanceof Event e ? e.id() : null);
    }

    /** Register action: validate on the EDT (fast), then check duplicates and insert in a SwingWorker. */
    void onRegister() {
        clearHighlights();
        RegistrationForm f = form();
        try {
            service.validate(f);
        } catch (ValidationException ex) { showErrors(ex); return; }
        register.setEnabled(false);
        show("Saving registration...", Color.DARK_GRAY);
        new SwingWorker<Registration, Void>() {
            @Override protected Registration doInBackground() throws Exception { return service.register(f); }
            @Override protected void done() {
                register.setEnabled(true);
                try {
                    Registration r = get();
                    show("✔ Registered " + r.regNo() + " (" + r.studentName() + ") for " + r.eventName() + ".", new Color(0x006300));
                    status.accept("Registration saved: " + r.regNo() + " -> " + r.eventName());
                    regNo.setText(""); name.setText(""); email.setText("");
                    onRegistered.run();
                } catch (ExecutionException ex) {
                    Throwable c = ex.getCause();
                    if (c instanceof DuplicateRegistrationException d) {
                        highlight("regNo"); highlight("event");
                        show("✖ " + d.getMessage(), new Color(0xb00020));
                        JOptionPane.showMessageDialog(RegisterPanel.this, d.getMessage() + "\nA student can register only once for each event.",
                                "Duplicate registration", JOptionPane.WARNING_MESSAGE);
                    } else if (c instanceof ValidationException v) {
                        showErrors(v);
                    } else if (c instanceof DataAccessException d) {
                        show("✖ " + d.getMessage(), new Color(0xb00020));
                        JOptionPane.showMessageDialog(RegisterPanel.this, d.getMessage() + "\n" + d.getCause(), "Database error", JOptionPane.ERROR_MESSAGE);
                    } else show("✖ " + c, new Color(0xb00020));
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        }.execute();
    }

    void showErrors(ValidationException ex) {
        StringBuilder sb = new StringBuilder("<html>");
        for (Map.Entry<String, String> e : ex.fieldErrors().entrySet()) { highlight(e.getKey()); sb.append("✖ ").append(e.getValue()).append("<br>"); }
        message.setForeground(new Color(0xb00020));
        message.setText(sb.append("</html>").toString());
        JComponent first = fieldOf.get(ex.fieldErrors().keySet().iterator().next());
        if (first != null) first.requestFocusInWindow();
    }

    private void highlight(String field) {
        JComponent c = fieldOf.get(field);
        if (c != null) c.setBorder(BorderFactory.createLineBorder(new Color(0xd03030), 2));
    }

    private void clearHighlights() {
        for (JComponent c : fieldOf.values()) c.setBorder(c instanceof JTextField ? normalText : null);
        if (!(dept.getBorder() == null)) dept.setBorder(null);
        dept.updateUI(); year.updateUI(); event.updateUI();
    }

    private void show(String text, Color c) { message.setForeground(c); message.setText(text); }

    void clearForm() {
        clearHighlights();
        regNo.setText(""); name.setText(""); email.setText("");
        dept.setSelectedIndex(0); year.setSelectedIndex(0); if (event.getItemCount() > 0) event.setSelectedIndex(0);
        message.setText(" ");
    }
}
