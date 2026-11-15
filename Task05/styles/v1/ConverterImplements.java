// Task 5.1 (a) - the frame itself implements ActionListener.
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class ConverterImplements extends JFrame implements ActionListener {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    final JLabel result = new JLabel(" ");
    final JButton convert = new JButton("Convert");

    public ConverterImplements() {
        super("Converter (a) implements ActionListener");
        setLayout(new FlowLayout());
        add(new JLabel("INR")); add(amount); add(target); add(convert); add(result);
        convert.addActionListener(this);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        pack();
    }

    @Override public void actionPerformed(ActionEvent e) {
        if (e.getSource() == convert) {
            String cur = (String) target.getSelectedItem();
            try {
                double inr = Double.parseDouble(amount.getText().trim());
                result.setText(String.format("%.2f %s", inr * Rates.of(cur), cur));
            } catch (NumberFormatException ex) {
                result.setText("Invalid amount");
            }
        }
    }

    public static void main(String[] args) { javax.swing.SwingUtilities.invokeLater(() -> new ConverterImplements().setVisible(true)); }
}
