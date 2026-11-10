package musicplayer.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/** Opens connections and makes sure the database and its tables exist. */
public class Database {

    private final DatabaseConfig config;
    private boolean ready;

    public Database(DatabaseConfig config) {
        this.config = config;
    }

    public synchronized DatabaseConfig config() { return config; }

    /** Creates the database and tables if they are missing. Safe to call many times. */
    public synchronized void init() {
        if (ready) return;
        if (!config.database().matches("[A-Za-z0-9_]{1,64}")) {
            throw new DatabaseException("The database name may only contain letters, digits and _", null);
        }
        try (Connection c = DriverManager.getConnection(config.serverUrl(), config.user(), config.password());
             Statement st = c.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + config.database()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw friendly(e);
        }
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + " id INT AUTO_INCREMENT PRIMARY KEY,"
                    + " username VARCHAR(30) NOT NULL,"
                    + " email VARCHAR(120) NOT NULL,"
                    + " password VARCHAR(128) NOT NULL,"
                    + " display_name VARCHAR(60) NOT NULL,"
                    + " music_folder VARCHAR(1024) NULL,"
                    + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " last_login TIMESTAMP NULL,"
                    + " UNIQUE KEY uq_users_username (username),"
                    + " UNIQUE KEY uq_users_email (email)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
            // Only the favourite's path is stored - the songs themselves stay in the music folder.
            st.executeUpdate("CREATE TABLE IF NOT EXISTS favourite_songs ("
                    + " user_id INT NOT NULL,"
                    + " song_path VARCHAR(700) NOT NULL,"
                    + " title VARCHAR(255) NULL,"
                    + " artist VARCHAR(255) NULL,"
                    + " added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " PRIMARY KEY (user_id, song_path),"
                    + " CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw friendly(e);
        }
        ready = true;
    }

    /** A fresh connection to our database. Callers close it (try-with-resources). */
    public Connection connect() throws SQLException {
        DatabaseConfig c = config();
        return DriverManager.getConnection(c.databaseUrl(), c.user(), c.password());
    }

    /** Turns a raw SQLException into a message a normal person can act on. */
    public DatabaseException friendly(SQLException e) {
        DatabaseConfig c = config();
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String low = msg.toLowerCase(Locale.ROOT);
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        boolean duplicate = state.startsWith("23") || e.getErrorCode() == 1062;
        String text;
        if (duplicate) text = "That value already exists.";
        else if (e.getErrorCode() == 1045 || state.equals("28000")) text = "MySQL refused the user name or password. Check db.user / db.password in application.properties.";
        else if (low.contains("no suitable driver")) text = "The MySQL driver (Connector/J) is missing from the program.";
        else if (state.startsWith("08") || low.contains("communications link") || low.contains("connection refused")
                || low.contains("could not connect") || low.contains("connect timed out"))
            text = "Cannot reach MySQL at " + c.host() + ":" + c.port() + ". Is the MySQL server running?";
        else if (e.getErrorCode() == 1044 || e.getErrorCode() == 1142)
            text = "The MySQL user \"" + c.user() + "\" is not allowed to create or use the database \"" + c.database() + "\".";
        else text = "Database error: " + msg;
        return new DatabaseException(text, e, duplicate);
    }
}
