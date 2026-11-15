package campus.ui;

import campus.model.Registration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Holds only the rows currently displayed (one page). */
public class RegistrationTableModel extends AbstractTableModel {
    private static final String[] COLS = {"ID", "Reg No", "Name", "E-mail", "Dept", "Year", "Event", "Registered at"};
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private List<Registration> rows = new ArrayList<>();

    public void setRows(List<Registration> newRows) { rows = newRows; fireTableDataChanged(); }
    public Registration row(int i) { return rows.get(i); }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int c) { return COLS[c]; }
    @Override public Class<?> getColumnClass(int c) { return c == 0 ? Long.class : c == 5 ? Integer.class : String.class; }

    @Override public Object getValueAt(int r, int c) {
        Registration x = rows.get(r);
        return switch (c) {
            case 0 -> x.id(); case 1 -> x.regNo(); case 2 -> x.studentName(); case 3 -> x.email();
            case 4 -> x.dept(); case 5 -> x.year(); case 6 -> x.eventName(); default -> x.registeredAt().format(TIME);
        };
    }
}
