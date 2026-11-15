// Task 4.2 - the FIRST defect fixed (mouseClicked spelled correctly, label updated), but still drawing
// with getGraphics(): used to construct the failing interaction (dots vanish after minimise/restore).
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DrawPadGetGraphics extends Frame {
  int count = 0; Label status = new Label("Clicks: 0");
  public DrawPadGetGraphics() {
    super("Draw Pad (getGraphics)");
    add(status, BorderLayout.SOUTH);
    addMouseListener(new MouseAdapter() {
      @Override public void mouseClicked(MouseEvent e) {
        count++;
        status.setText("Clicks: " + count);
        Graphics g = getGraphics(); g.fillOval(e.getX() - 3, e.getY() - 3, 6, 6);   // drawn once, never remembered
      }
    });
    setSize(400, 300);
  }
  public static void main(String[] args) { new DrawPadGetGraphics().setVisible(true); }
}
