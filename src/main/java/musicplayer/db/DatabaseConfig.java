package musicplayer.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Where the MySQL server is. Everything is read from src/main/resources/application.properties
 * (keys: db.host, db.port, db.name, db.user, db.password, db.params).
 */
public final class DatabaseConfig {
    private static final String RESOURCE = "/application.properties";

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final String extraParams;      // advanced: extra JDBC URL parameters, e.g. "useSSL=true"

    public DatabaseConfig(String host, int port, String database, String user, String password, String extraParams) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.extraParams = extraParams == null ? "" : extraParams;
    }

    /** Used only for any key that is missing from application.properties. */
    public static DatabaseConfig defaults() {
        return new DatabaseConfig("localhost", 3306, "latabeats", "root", "", "");
    }

    /** Reads application.properties from the classpath (src/main/resources). */
    public static DatabaseConfig load() {
        Properties p = new Properties();
        try (InputStream in = DatabaseConfig.class.getResourceAsStream(RESOURCE)) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {
            // fall back to defaults
        }
        DatabaseConfig d = defaults();
        int port = d.port;
        try {
            port = Integer.parseInt(p.getProperty("db.port", String.valueOf(d.port)).trim());
        } catch (NumberFormatException ignored) {
            // keep the default port
        }
        return new DatabaseConfig(
                p.getProperty("db.host", d.host).trim(),
                port,
                p.getProperty("db.name", d.database).trim(),
                p.getProperty("db.user", d.user).trim(),
                p.getProperty("db.password", d.password),
                p.getProperty("db.params", d.extraParams).trim());
    }

    /** Connection to the server itself (used once to create the database). */
    public String serverUrl()   { return url(""); }

    /** Connection to our database. */
    public String databaseUrl() { return url(database); }

    private String url(String db) {
        String params = "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
        if (!extraParams.isBlank()) params += "&" + extraParams.trim();
        return "jdbc:mysql://" + host + ":" + port + "/" + db + "?" + params;
    }

    public String host()        { return host; }
    public int port()           { return port; }
    public String database()    { return database; }
    public String user()        { return user; }
    public String password()    { return password; }
    public String extraParams() { return extraParams; }
}
