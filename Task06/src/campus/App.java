package campus;

import campus.dao.EventDao;
import campus.dao.RegistrationDao;
import campus.db.ConnectionFactory;
import campus.service.RegistrationService;
import campus.ui.MainFrame;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

/**
 * Entry point: wires the layers (db -> dao -> service -> ui).
 *   MySQL:  java -cp out:lib/mysql-connector-j-9.1.0.jar campus.App db.properties
 *   Demo :  java -cp out campus.App db-demo.properties      (in-memory test double, no MySQL needed)
 */
public final class App {
    public static void main(String[] args) throws Exception {
        ConnectionFactory db = new ConnectionFactory(Path.of(args.length > 0 ? args[0] : "db.properties"));
        RegistrationService service = new RegistrationService(new EventDao(db), new RegistrationDao(db));
        String sample = db.property("sample.data");
        String label = db.url().startsWith("jdbc:mysql") ? "MySQL " + db.url() : "in-memory test double (" + db.url() + ")";
        SwingUtilities.invokeLater(() -> new MainFrame(service, label, sample == null ? null : Path.of(sample)).setVisible(true));
    }
}
