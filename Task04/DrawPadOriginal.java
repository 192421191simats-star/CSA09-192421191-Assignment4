// Task 4 - the program AS REPORTED (class renamed from DrawPad only so all versions can live in one folder;
// imports and main() added so it compiles and runs). The body of the constructor is unchanged.
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DrawPadOriginal extends Frame {
  int count = 0; Label status = new Label("Clicks: 0");
  public DrawPadOriginal() {
    add(status, BorderLayout.SOUTH);
    addMouseListener(new MouseAdapter() {
      public void mouseclicked(MouseEvent e) {          // lower-case 'c': a NEW method, never called by AWT
        count++;
        Graphics g = getGraphics(); g.fillOval(e.getX(), e.getY(), 6, 6);
      }
    });
    setSize(400, 300); setVisible(true);
  }
  public static void main(String[] args) { new DrawPadOriginal(); }
}
