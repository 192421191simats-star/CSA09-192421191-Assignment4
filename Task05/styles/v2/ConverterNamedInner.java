// Task 5.1 (b) - a named inner class.
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class ConverterNamedInner extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    final JLabel result = new JLabel(" ");
    final JButton convert = new JButton("Convert");

    /** Named inner class: has access to the frame's fields through ConverterNamedInner.this. */
    class ConvertHandler implements ActionListener {
        @Override public void actionPerformed(ActionEvent e) {
            String cur = (String) target.getSelectedItem();
            try {
                double inr = Double.parseDouble(amount.getText().trim());
                JOptionPane.showMessageDialog(ConverterNamedInner.this, String.format("%.2f INR = %.2f %s", inr, inr * Rates.of(cur), cur));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(ConverterNamedInner.this, "Invalid amount", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public ConverterNamedInner() {
        super("Converter (b) named inner class");
        setLayout(new FlowLayout());
        add(new JLabel("INR")); add(amount); add(target); add(convert); add(result);
        ConvertHandler handler = new ConvertHandler();       // one handler object, reused
        convert.addActionListener(handler);
        amount.addActionListener(handler);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        pack();
    }

    public static void main(String[] args) { javax.swing.SwingUtilities.invokeLater(() -> new ConverterNamedInner().setVisible(true)); }
}
