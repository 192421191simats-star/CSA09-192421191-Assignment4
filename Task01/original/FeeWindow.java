// Task 1 - the junior developer's program EXACTLY as given; only the import lines were added.
// It is compiled to record the compiler's verdict (see output/original_compile.txt).
import java.awt.Button;
import java.awt.Frame;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;

public class FeeWindow extends Frame implements ActionListener {
  TextField regNo = new TextField(); TextField amount = new TextField();
  Button pay = new Button("Pay"), clear = new Button("Clear"), receipt = new Button("Receipt");
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand() == "Pay") {
      double a = Double.parseDouble(amount.getText());
      Connection c = DriverManager.getConnection(URL, USER, PWD);
      c.createStatement().executeUpdate("INSERT INTO fees VALUES('" + regNo.getText() + "'," + a + ")");
      Thread.sleep(3000);          // wait for bank confirmation
      System.out.println("Paid");
    } else { regNo.setText(""); amount.setText(""); }
  }
}
