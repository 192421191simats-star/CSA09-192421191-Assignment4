import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextField;

/**
 * Task 3 alternative concept: the same screen built (a) with ONE GridBagLayout panel and
 * (b) with absolute positioning (setLayout(null) + setBounds) designed for 640 x 420.
 */
public final class LibraryAlternatives {
    private LibraryAlternatives() { }

    static Label title() {
        Label t = new Label("Campus Library - Catalogue Search", Label.CENTER);
        t.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18)); t.setBackground(new Color(0x1f3a68)); t.setForeground(Color.WHITE);
        return t;
    }
    static Choice categories() { Choice c = new Choice(); for (String s : new String[]{"All", "Books", "Journals", "E-books", "Theses"}) c.add(s); return c; }
    static List results() { List l = new List(10); l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); for (int i = 1; i <= 8; i++) l.add("L000" + i + " sample result row " + i); return l; }

    /** (a) One container, GridBagLayout: 5 columns, the results row has weighty = 1. */
    public static Frame gridBag() {
        Frame f = new Frame("Library - GridBagLayout");
        Panel p = new Panel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 5; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; p.add(title(), g);
        g.gridy = 1; g.gridwidth = 1; g.fill = GridBagConstraints.NONE; g.weightx = 0; g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; p.add(new Label("Title / author:"), g);
        g.gridx = 1; p.add(new TextField(28), g);
        g.gridx = 2; p.add(categories(), g);
        g.gridx = 3; p.add(new Button("Search"), g);
        g.gridx = 4; g.weightx = 1; p.add(new Label(""), g);                          // filler keeps the row left-aligned
        g.gridx = 0; g.gridy = 2; g.gridwidth = 5; g.fill = GridBagConstraints.BOTH; g.weightx = 1; g.weighty = 1; p.add(results(), g);
        Panel buttons = new Panel(); buttons.add(new Button("Prev")); buttons.add(new Button("Next")); buttons.add(new Button("Reserve"));
        g.gridy = 3; g.fill = GridBagConstraints.NONE; g.weighty = 0; g.anchor = GridBagConstraints.CENTER; p.add(buttons, g);
        g.gridy = 4; g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST; p.add(new Label("Status: ready"), g);
        f.add(p);
        f.setSize(640, 420);
        return f;
    }

    /** (b) Absolute positioning: every component placed with setBounds for a 640 x 420 window. */
    public static Frame absolute() {
        Frame f = new Frame("Library - null layout");
        f.setLayout(null);
        Label t = title(); t.setBounds(0, 0, 640, 30); f.add(t);
        Label l = new Label("Title / author:"); l.setBounds(8, 38, 95, 24); f.add(l);
        TextField q = new TextField(28); q.setBounds(105, 38, 240, 24); f.add(q);
        Choice c = categories(); c.setBounds(352, 38, 90, 24); f.add(c);
        Button s = new Button("Search"); s.setBounds(450, 38, 70, 24); f.add(s);
        List r = results(); r.setBounds(0, 70, 640, 270); f.add(r);
        Button b1 = new Button("Prev"), b2 = new Button("Next"), b3 = new Button("Reserve");
        b1.setBounds(222, 348, 55, 26); b2.setBounds(285, 348, 55, 26); b3.setBounds(348, 348, 70, 26);
        f.add(b1); f.add(b2); f.add(b3);
        Label st = new Label("Status: ready"); st.setBounds(0, 386, 640, 22); f.add(st);
        f.setSize(640, 420);
        return f;
    }
}
