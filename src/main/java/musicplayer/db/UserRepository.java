package musicplayer.db;

import musicplayer.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** All SQL that touches the users table. Every query is a PreparedStatement (no SQL injection). */
public class UserRepository {

    /** A user together with the stored password - it never leaves the db/service layers. */
    public record Stored(User user, String password) { }

    private final Database db;

    public UserRepository(Database db) {
        this.db = db;
    }

    public Optional<Stored> findByLogin(String usernameOrEmail) {
        String sql = "SELECT id, username, email, password, display_name, music_folder FROM users"
                + " WHERE username = ? OR email = ? LIMIT 1";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                User u = new User(rs.getInt("id"), rs.getString("username"), rs.getString("email"),
                        rs.getString("display_name"), rs.getString("music_folder"));
                return Optional.of(new Stored(u, rs.getString("password")));
            }
        } catch (SQLException e) {
            throw db.friendly(e);
        }
    }

    public boolean usernameExists(String username) { return exists("username", username); }
    public boolean emailExists(String email)       { return exists("email", email); }

    private boolean exists(String column, String value) {
        String sql = "SELECT 1 FROM users WHERE " + column + " = ? LIMIT 1";      // column name is a constant above
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw db.friendly(e);
        }
    }

    public User insert(String username, String email, String password, String displayName) {
        String sql = "INSERT INTO users (username, email, password, display_name) VALUES (?, ?, ?, ?)";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, displayName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new User(keys.getInt(1), username, email, displayName, null);
            }
        } catch (SQLException e) {
            throw db.friendly(e);
        }
    }

    public void updateDisplayName(int userId, String name) {
        update("UPDATE users SET display_name = ? WHERE id = ?", name, userId);
    }

    public void updateMusicFolder(int userId, String folder) {
        update("UPDATE users SET music_folder = ? WHERE id = ?", folder, userId);
    }

    public void touchLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw db.friendly(e);
        }
    }

    private void update(String sql, String value, int userId) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw db.friendly(e);
        }
    }
}
