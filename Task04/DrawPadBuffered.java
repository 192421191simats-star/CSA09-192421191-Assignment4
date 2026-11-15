// Task 4 alternative concept: every dot is drawn ONCE onto an off-screen BufferedImage;
// paint() only copies that image to the screen.
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class DrawPadBuffered extends Frame {
    private final BufferedImage canvas = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
    private final Label status = new Label("Clicks: 0");
    private int clicks;

    public DrawPadBuffered() {
        super("Draw Pad (BufferedImage)");
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Graphics2D g = canvas.createGraphics();
                g.setColor(Color.BLACK);
                g.fillOval(e.getX() - 3, e.getY() - 3, 6, 6);  // drawn once into the image
                g.dispose();
                status.setText("Clicks: " + ++clicks);
                repaint();
            }
        });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
        setSize(400, 300);
    }

    @Override public void paint(Graphics g) { g.drawImage(canvas, 0, 0, null); }   // one image copy, whatever n is

    public static void main(String[] args) { new DrawPadBuffered().setVisible(true); }
}
