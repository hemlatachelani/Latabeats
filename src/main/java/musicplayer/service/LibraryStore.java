package musicplayer.service;

import musicplayer.model.Song;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves library extras that MySQL does not hold: moods, play counts, named playlists,
 * recently played, and mood listening time. One text file per user under ~/.nocturne.
 */
public class LibraryStore {

    public static final class SongRecord {
        public final String path, mood, title, artist, album;
        public final int playCount;
        public final double duration;

        public SongRecord(String path, String mood, int playCount, double duration,
                          String title, String artist, String album) {
            this.path = path;
            this.mood = mood;
            this.playCount = playCount;
            this.duration = duration;
            this.title = title;
            this.artist = artist;
            this.album = album;
        }
    }

    public static final class PlaylistRecord {
        public final String name;
        public final List<String> paths;

        public PlaylistRecord(String name, List<String> paths) {
            this.name = name;
            this.paths = paths;
        }
    }

    public static final class Snapshot {
        public final List<SongRecord> songs = new ArrayList<>();
        public final List<PlaylistRecord> playlists = new ArrayList<>();
        public final List<String> recentPaths = new ArrayList<>();
        public final Map<String, Double> moodSeconds = new LinkedHashMap<>();
    }

    private final File file;

    public static LibraryStore forUser(String username) {
        File primary = new File(System.getProperty("user.home"),
                ".latabeats" + File.separator + "library_" + username + ".txt");
        if (primary.exists()) return new LibraryStore(primary);
        File legacy = new File(System.getProperty("user.home"),
                ".nocturne" + File.separator + "library_" + username + ".txt");
        if (legacy.exists()) return new LibraryStore(legacy);
        return new LibraryStore(primary);
    }

    public LibraryStore(File file) {
        this.file = file;
    }

    public Snapshot load() {
        Snapshot snap = new Snapshot();
        if (!file.exists()) return snap;
        Map<String, List<String>> playlistTracks = new LinkedHashMap<>();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("NOCTURNE") || line.startsWith("LATABEATS")) continue;
                String[] p = split(line);
                if (p.length == 0) continue;
                switch (p[0]) {
                    case "S" -> {
                        if (p.length < 8) break;
                        snap.songs.add(new SongRecord(p[1], p[2], parseInt(p[3]), parseDouble(p[4]), p[5], p[6], p[7]));
                    }
                    case "P" -> {
                        if (p.length < 2) break;
                        playlistTracks.putIfAbsent(p[1], new ArrayList<>());
                    }
                    case "T" -> {
                        if (p.length < 3) break;
                        playlistTracks.computeIfAbsent(p[1], k -> new ArrayList<>()).add(p[2]);
                    }
                    case "R" -> {
                        if (p.length >= 2 && !p[1].isEmpty()) snap.recentPaths.add(p[1]);
                    }
                    case "M" -> {
                        if (p.length >= 3) snap.moodSeconds.put(p[1], parseDouble(p[2]));
                    }
                    default -> { }
                }
            }
        } catch (IOException ignored) {
            return snap;
        }
        for (Map.Entry<String, List<String>> e : playlistTracks.entrySet()) {
            snap.playlists.add(new PlaylistRecord(e.getKey(), e.getValue()));
        }
        return snap;
    }

    public synchronized void save(MusicLibrary library, PlaylistService playlists,
                                  PlaybackService playback, ListeningStats stats) {
        Snapshot snap = new Snapshot();
        for (Song s : library.songs()) {
            snap.songs.add(new SongRecord(s.getPath(), s.getMood(), s.getPlayCount(), s.getDurationSeconds(),
                    s.getTitle(), s.getArtist(), s.getAlbum()));
        }
        snap.playlists.addAll(playlists.snapshot());
        for (Song s : playback.recentlyPlayed()) snap.recentPaths.add(s.getPath());
        snap.moodSeconds.putAll(stats.snapshot());
        save(snap);
    }

    public synchronized void save(Snapshot snap) {
        File dir = file.getParentFile();
        if (dir != null) dir.mkdirs();
        File tmp = new File(file.getAbsolutePath() + ".tmp");
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8))) {
            out.write("LATABEATS_LIBRARY 1\n");
            for (SongRecord s : snap.songs) {
                out.write(join("S", s.path, s.mood, Integer.toString(s.playCount),
                        Double.toString(s.duration), s.title, s.artist, s.album));
                out.write('\n');
            }
            for (PlaylistRecord p : snap.playlists) {
                out.write(join("P", p.name));
                out.write('\n');
                for (String path : p.paths) {
                    out.write(join("T", p.name, path));
                    out.write('\n');
                }
            }
            for (String path : snap.recentPaths) {
                out.write(join("R", path));
                out.write('\n');
            }
            for (Map.Entry<String, Double> e : snap.moodSeconds.entrySet()) {
                out.write(join("M", e.getKey(), Double.toString(e.getValue())));
                out.write('\n');
            }
        } catch (IOException ignored) {
            tmp.delete();
            return;
        }
        if (!tmp.renameTo(file)) {
            file.delete();
            tmp.renameTo(file);
        }
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    private static String join(String... parts) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) b.append('\t');
            b.append(esc(parts[i] == null ? "" : parts[i]));
        }
        return b.toString();
    }

    private static String[] split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                parts.add(unesc(cur.toString()));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(unesc(cur.toString()));
        return parts.toArray(String[]::new);
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private static String unesc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 't') b.append('\t');
                else if (n == 'n') b.append('\n');
                else if (n == '\\') b.append('\\');
                else b.append(n);
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
}
