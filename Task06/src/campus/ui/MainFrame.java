package campus.ui;

import campus.model.Event;
import campus.service.RegistrationService;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/** Main window: JMenuBar, a CardLayout holding the two screens, and a status bar. */
public class MainFrame extends JFrame {
    public static final String REGISTER = "register", LIST = "list";
    public final CardLayout cards = new CardLayout();
    public final JPanel content = new JPanel(cards);
    public final RegisterPanel registerPanel;
    public final RegistrationsPanel listPanel;
    public final JLabel statusBar = new JLabel("Starting...");
    public final JMenu fileMenu = new JMenu("File"), viewMenu = new JMenu("View"), helpMenu = new JMenu("Help");
    public final JMenuItem exportItem = new JMenuItem("Export CSV..."), exitItem = new JMenuItem("Exit");
    public final JMenuItem registerItem = new JMenuItem("Register student"), listItem = new JMenuItem("Registrations"), aboutItem = new JMenuItem("About");
    private final RegistrationService service;
    private boolean listStale = true;

    public MainFrame(RegistrationService service, String databaseLabel, Path sampleData) {
        super("Campus Event Registration - Cultural Committee");
        this.service = service;
        registerPanel = new RegisterPanel(service, this::status);
        listPanel = new RegistrationsPanel(service, this::status);
        registerPanel.setOnRegistered(() -> listStale = true);
        content.add(registerPanel, REGISTER);
        content.add(listPanel, LIST);

        fileMenu.setMnemonic(KeyEvent.VK_F); viewMenu.setMnemonic(KeyEvent.VK_V); helpMenu.setMnemonic(KeyEvent.VK_H);
        exportItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        registerItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.CTRL_DOWN_MASK));
        listItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.CTRL_DOWN_MASK));
        fileMenu.add(exportItem); fileMenu.addSeparator(); fileMenu.add(exitItem);
        viewMenu.add(registerItem); viewMenu.add(listItem);
        helpMenu.add(aboutItem);
        JMenuBar bar = new JMenuBar();
        bar.add(fileMenu); bar.add(viewMenu); bar.add(helpMenu);
        setJMenuBar(bar);

        exportItem.addActionListener(this::onExport);
        exitItem.addActionListener(e -> dispose());
        registerItem.addActionListener(e -> showScreen(REGISTER));
        listItem.addActionListener(e -> showScreen(LIST));
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Campus Event Registration\nCSA09 Assignment 4 - Task 6\nDatabase: " + databaseLabel, "About", JOptionPane.INFORMATION_MESSAGE));

        statusBar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        add(content, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 560);

        // start-up work off the EDT: sample data (first run only) + events
        new SwingWorker<List<Event>, Void>() {
            int loaded;
            @Override protected List<Event> doInBackground() throws Exception {
                List<Event> ev = service.loadEvents();
                loaded = service.loadSampleIfEmpty(sampleData);
                return ev;
            }
            @Override protected void done() {
                try {
                    List<Event> ev = get();
                    registerPanel.setEvents(ev);
                    listPanel.setEvents(ev);
                    status("Connected to " + databaseLabel + " - " + ev.size() + " events" + (loaded > 0 ? ", " + loaded + " sample registrations loaded" : ""));
                } catch (ExecutionException ex) {
                    status("Cannot reach the database: " + ex.getCause().getMessage());
                    JOptionPane.showMessageDialog(MainFrame.this, ex.getCause().getMessage() + "\n" + ex.getCause().getCause(), "Database error", JOptionPane.ERROR_MESSAGE);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        }.execute();
    }

    public void showScreen(String name) {
        cards.show(content, name);
        if (LIST.equals(name) && listStale) { listStale = false; listPanel.load(0); }
    }

    private void onExport(ActionEvent e) {
        showScreen(LIST);
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
        chooser.setDialogTitle("Export registrations as CSV");
        chooser.setSelectedFile(new File("registrations.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) listPanel.exportTo(chooser.getSelectedFile().toPath());
    }

    public void status(String s) { statusBar.setText(s); }
}
