import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Task 1.1 - runtime evidence for the original design. The original does not compile (see
 * original_compile.txt), so this class keeps its STRUCTURE (one listener, logic inside it, == on the
 * action command, string-built SQL, sleep on the event thread) with the minimum needed to run:
 * exceptions wrapped, and a fake "database" that just records the SQL text it would execute.
 */
public class OriginalBehaviourDemo extends Frame implements ActionListener {
    final TextField regNo = new TextField(12), amount = new TextField(8);
    final Button pay = new Button("Pay"), clear = new Button("Clear"), receipt = new Button("Receipt");
    final Label hint = new Label("(no feedback area in the original design)");
    static final List<String> executedSql = new ArrayList<>();
    static final List<String> console = new ArrayList<>();

    public OriginalBehaviourDemo() {
        super("FeeWindow (original structure)");
        setLayout(new FlowLayout());
        add(new Label("Reg No")); add(regNo); add(new Label("Amount")); add(amount); add(pay); add(clear); add(receipt); add(hint);
        pay.addActionListener(this); clear.addActionListener(this); receipt.addActionListener(this);
        setSize(560, 110);
    }

    @Override public void actionPerformed(ActionEvent e) {
        try {
            if (e.getActionCommand() == "Pay") {
                double a = Double.parseDouble(amount.getText());
                executedSql.add("INSERT INTO fees VALUES('" + regNo.getText() + "'," + a + ")");
                Thread.sleep(3000);          // wait for bank confirmation  (ON THE EVENT-DISPATCH THREAD)
                console.add("Paid");
                System.out.println("Paid");
            } else { regNo.setText(""); amount.setText(""); }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
