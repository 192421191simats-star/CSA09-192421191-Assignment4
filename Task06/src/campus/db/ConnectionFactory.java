package campus.db;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens JDBC connections from a properties file (url, user, password). */
public final class ConnectionFactory {
    private final String url, user, password;
    private final Properties props;

    public ConnectionFactory(Path propertiesFile) throws IOException {
        props = new Properties();
        try (Reader r = Files.newBufferedReader(propertiesFile)) { props.load(r); }
        url = props.getProperty("url");
        user = props.getProperty("user", "");
        password = props.getProperty("password", "");
        if (url.startsWith("jdbc:campusdb:")) {
            try { Class.forName("campus.db.testdouble.FakeCampusDriver"); }       // registers the test double
            catch (ClassNotFoundException e) { throw new IOException(e); }
        }
    }

    public ConnectionFactory(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password; this.props = new Properties();
        if (url.startsWith("jdbc:campusdb:")) {
            try { Class.forName("campus.db.testdouble.FakeCampusDriver"); } catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
        }
    }

    public Connection open() throws SQLException { return DriverManager.getConnection(url, user, password); }
    public String url() { return url; }
    public String property(String key) { return props.getProperty(key); }
}
