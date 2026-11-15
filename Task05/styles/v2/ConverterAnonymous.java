// Task 5.1 (c) - an anonymous inner class.
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

public class ConverterAnonymous extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    final JLabel result = new JLabel(" ");
    final JButton convert = new JButton("Convert");

    public ConverterAnonymous() {
        super("Converter (c) anonymous inner class");
        setLayout(new FlowLayout());
        add(new JLabel("INR")); add(amount); add(target); add(convert); add(result);
        ActionListener onConvert = new ActionListener() {    // anonymous object now stored so it can be shared
            @Override public void actionPerformed(ActionEvent e) {
                String cur = (String) target.getSelectedItem();
                try {
                    double inr = Double.parseDouble(amount.getText().trim());
                    JOptionPane.showMessageDialog(ConverterAnonymous.this, String.format("%.2f INR = %.2f %s", inr, inr * Rates.of(cur), cur));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(ConverterAnonymous.this, "Invalid amount", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        convert.addActionListener(onConvert);
        amount.addActionListener(onConvert);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        pack();
    }

    public static void main(String[] args) { javax.swing.SwingUtilities.invokeLater(() -> new ConverterAnonymous().setVisible(true)); }
}
