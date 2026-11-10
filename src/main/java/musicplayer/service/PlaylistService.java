package musicplayer.service;

import musicplayer.ds.HashTable;
import musicplayer.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Named playlists: each one is a doubly linked list, looked up by name in our hash table.
 */
public class PlaylistService {

    private final HashTable<String, Playlist> byName = new HashTable<>();
    private final List<String> order = new ArrayList<>();

    public List<String> names() {
        return Collections.unmodifiableList(order);
    }

    public Playlist get(String name) {
        return name == null ? null : byName.get(name);
    }

    public List<Song> songs(String name) {
        Playlist p = get(name);
        return p == null ? List.of() : p.toList();
    }

    public boolean isEmpty() {
        return order.isEmpty();
    }

    /** Creates a playlist and returns the name that was actually used (unique). */
    public String create(String requested) {
        String name = unique(clean(requested));
        byName.put(name, new Playlist());
        order.add(name);
        return name;
    }

    public boolean rename(String oldName, String requested) {
        Playlist p = byName.get(oldName);
        if (p == null) return false;
        String name = unique(clean(requested), oldName);
        if (name.equals(oldName)) return true;
        byName.remove(oldName);
        byName.put(name, p);
        int i = order.indexOf(oldName);
        if (i >= 0) order.set(i, name);
        return true;
    }

    public void delete(String name) {
        if (byName.remove(name) != null) order.remove(name);
    }

    public void addSong(String name, Song song) {
        Playlist p = byName.get(name);
        if (p == null || song == null || p.contains(song)) return;
        p.add(song);
    }

    public void removeSong(String name, Song song) {
        Playlist p = byName.get(name);
        if (p != null) p.remove(song);
    }

    /** A song left the library: drop it from every playlist. */
    public void forget(Song song) {
        for (String name : order) {
            Playlist p = byName.get(name);
            if (p != null) p.remove(song);
        }
    }

    public void clear() {
        byName.clear();
        order.clear();
    }

    /** Rebuilds playlists from saved names and paths. Missing files are skipped. */
    public void restore(List<LibraryStore.PlaylistRecord> records, MusicLibrary library) {
        clear();
        if (records == null) return;
        for (LibraryStore.PlaylistRecord rec : records) {
            if (rec.name == null || rec.name.isBlank()) continue;
            String name = unique(clean(rec.name));
            Playlist p = new Playlist();
            if (rec.paths != null) {
                for (String path : rec.paths) {
                    Song s = library.byPath(path);
                    if (s != null && !p.contains(s)) p.add(s);
                }
            }
            byName.put(name, p);
            order.add(name);
        }
    }

    public List<LibraryStore.PlaylistRecord> snapshot() {
        List<LibraryStore.PlaylistRecord> out = new ArrayList<>();
        for (String name : order) {
            Playlist p = byName.get(name);
            List<String> paths = new ArrayList<>();
            if (p != null) for (Song s : p.toList()) paths.add(s.getPath());
            out.add(new LibraryStore.PlaylistRecord(name, paths));
        }
        return out;
    }

    private String clean(String requested) {
        String name = requested == null ? "" : requested.trim();
        return name.isEmpty() ? "New playlist" : name;
    }

    private String unique(String name) {
        return unique(name, null);
    }

    private String unique(String name, String ignore) {
        if (!taken(name, ignore)) return name;
        int n = 2;
        while (taken(name + " " + n, ignore)) n++;
        return name + " " + n;
    }

    private boolean taken(String name, String ignore) {
        if (name.equals(ignore)) return false;
        return byName.containsKey(name);
    }
}
