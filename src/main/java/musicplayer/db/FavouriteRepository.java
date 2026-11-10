package musicplayer.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * All SQL for favourite songs. Only the file path (plus title/artist for readability) is stored:
 * the songs themselves are always read from the music folder.
 */
public class FavouriteRepository {
    private final Database db;

    public FavouriteRepository(Database db) {
        this.db = db;
    }

    public Set<String> findPaths(int userId) {
        Set<String> out = new HashSet<>();
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT song_path FROM favourite_songs WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw db.friendly(e);
        }
        return out;
    }

    public void add(int userId, String path, String title, String artist) {
        String sql = "INSERT INTO favourite_songs (user_id, song_path, title, artist) VALUES (?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE title = VALUES(title), artist = VALUES(artist)";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, path);
            ps.setString(3, cut(title, 255));
            ps.setString(4, cut(artist, 255));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw db.friendly(e);
        }
    }

    public void remove(int userId, String path) {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM favourite_songs WHERE user_id = ? AND song_path = ?")) {
            ps.setInt(1, userId);
            ps.setString(2, path);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw db.friendly(e);
        }
    }

    private static String cut(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }
}
