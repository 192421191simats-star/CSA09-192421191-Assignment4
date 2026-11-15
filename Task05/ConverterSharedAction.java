// Task 5 alternative concept: ONE Action object shared by the Convert button, a menu item, the Enter key
// in the amount field and a Ctrl+Enter keyboard shortcut; plus a method-reference registration.
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class ConverterSharedAction extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    final JLabel result = new JLabel(" ");
    final JButton button;
    final JMenuItem menuItem;

    /** One object holds the behaviour AND the shared state (name, shortcut, enabled flag). */
    final Action convertAction = new AbstractAction("Convert") {
        { putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK)); }
        @Override public void actionPerformed(ActionEvent e) { convert(); }
    };

    public ConverterSharedAction() {
        super("Converter - shared Action");
        setLayout(new FlowLayout());
        button = new JButton(convertAction);                                 // trigger 1
        menuItem = new JMenuItem(convertAction);                             // trigger 2 (shows Ctrl+Enter)
        amount.setAction(convertAction);                                     // trigger 3: Enter in the field
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)         // trigger 4: Ctrl+Enter anywhere
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "convert");
        getRootPane().getActionMap().put("convert", convertAction);
        JMenuBar bar = new JMenuBar(); JMenu m = new JMenu("Tools"); m.add(menuItem); bar.add(m); setJMenuBar(bar);
        add(new JLabel("INR")); add(amount); add(target); add(button); add(result);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    void convert() {
        String cur = (String) target.getSelectedItem();
        try {
            double inr = Double.parseDouble(amount.getText().trim());
            result.setText(String.format("%.2f %s", inr * Rates.of(cur), cur));
        } catch (NumberFormatException ex) {
            result.setText("Invalid amount");
        }
    }

    /** Method-reference variant for comparison: each evaluation of this::convertEvent creates a new object. */
    static int[] methodReferenceObjects(ConverterSharedAction f) {
        JButton b = new JButton("x"); JTextField t = new JTextField();
        b.addActionListener(f::convertEvent);
        t.addActionListener(f::convertEvent);
        ActionListener one = f::convertEvent;
        JButton b2 = new JButton("y"); JTextField t2 = new JTextField();
        b2.addActionListener(one); t2.addActionListener(one);
        boolean sameWhenWrittenTwice = b.getActionListeners()[0] == t.getActionListeners()[0];
        boolean sameWhenStored = b2.getActionListeners()[0] == t2.getActionListeners()[0];
        return new int[]{sameWhenWrittenTwice ? 1 : 2, sameWhenStored ? 1 : 2};
    }
    void convertEvent(ActionEvent e) { convert(); }

    public static void main(String[] args) { javax.swing.SwingUtilities.invokeLater(() -> new ConverterSharedAction().setVisible(true)); }
}
