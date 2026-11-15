package fees;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/**
 * Redesigned fee window: collects input and shows results ONLY. Each button has its own listener;
 * the slow payment runs in a SwingWorker so the window stays responsive.
 */
public class FeeWindow extends JFrame {
    public final JTextField regNo = new JTextField(12);
    public final JTextField amount = new JTextField(10);
    public final JButton pay = new JButton("Pay"), clear = new JButton("Clear"), receipt = new JButton("Receipt");
    public final JLabel status = new JLabel("Enter the register number and amount.");
    private final FeeService service;

    public FeeWindow(FeeService service) {
        super("Student Fee Payment");
        this.service = service;
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        form.add(new JLabel("Register No:")); form.add(regNo);
        form.add(new JLabel("Amount (Rs):")); form.add(amount);
        JPanel buttons = new JPanel();
        pay.setMnemonic(KeyEvent.VK_P);
        buttons.add(pay); buttons.add(clear); buttons.add(receipt);
        status.setBorder(BorderFactory.createEmptyBorder(4, 12, 10, 12));
        add(form, BorderLayout.NORTH); add(buttons, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);

        pay.addActionListener(e -> onPay());              // one listener per button: no string comparison
        clear.addActionListener(e -> onClear());
        receipt.addActionListener(e -> onReceipt());
        getRootPane().setDefaultButton(pay);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);   // the window really closes
        pack();
        setSize(Math.max(getWidth(), 620), getHeight());   // room for the longest status message
    }

    private void show(String text, boolean error) {
        status.setForeground(error ? new Color(0xb00020) : new Color(0x006300));
        status.setText(text);
    }

    void onPay() {
        try {
            service.validate(regNo.getText(), amount.getText());     // instant feedback on the EDT
        } catch (FeeValidationException ex) { show(ex.getMessage(), true); return; }
        pay.setEnabled(false);
        status.setForeground(Color.DARK_GRAY);
        status.setText("Processing payment - waiting for bank confirmation...");
        String r = regNo.getText(), a = amount.getText();
        new SwingWorker<Payment, Void>() {
            @Override protected Payment doInBackground() throws Exception { return service.pay(r, a); }   // worker thread
            @Override protected void done() {                                                           // back on the EDT
                pay.setEnabled(true);
                try {
                    Payment p = get();
                    show("Paid Rs " + p.amount() + " - receipt " + p.receiptNo() + " (bank ref " + p.bankReference() + ")", false);
                } catch (ExecutionException ex) {
                    show(ex.getCause().getMessage(), true);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }
        }.execute();
    }

    void onClear() { regNo.setText(""); amount.setText(""); show("Cleared.", false); regNo.requestFocusInWindow(); }

    void onReceipt() {
        try {
            Payment p = service.receiptFor(regNo.getText());
            JOptionPane.showMessageDialog(this, p.receiptText(), "Fee receipt", JOptionPane.INFORMATION_MESSAGE);
        } catch (FeeValidationException | PaymentFailedException ex) {
            show(ex.getMessage(), true);
        }
    }

    public static void main(String[] args) {
        FeeService s = new FeeService(new SimulatedBankGateway(3000), new InMemoryFeeRepository());
        SwingUtilities.invokeLater(() -> new FeeWindow(s).setVisible(true));
    }
}
