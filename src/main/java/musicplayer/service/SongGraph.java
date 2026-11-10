package musicplayer.service;

import musicplayer.ds.HashTable;
import musicplayer.ds.MyQueue;
import musicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * Undirected graph of songs. An edge means "related": same artist, same album, or same mood.
 * "More like this" is a breadth-first walk from the chosen song.
 */
public class SongGraph {

    private final HashTable<String, List<Song>> adj = new HashTable<>();
    private final HashTable<String, Song> nodes = new HashTable<>();

    public void rebuild(List<Song> songs) {
        adj.clear();
        nodes.clear();
        if (songs == null || songs.isEmpty()) return;

        HashTable<String, List<Song>> byArtist = new HashTable<>();
        HashTable<String, List<Song>> byAlbum = new HashTable<>();
        HashTable<String, List<Song>> byMood = new HashTable<>();

        for (Song s : songs) {
            nodes.put(s.getPath(), s);
            adj.put(s.getPath(), new ArrayList<>());
            index(byArtist, norm(s.getArtist()), s);
            if (!s.getAlbum().isBlank()) index(byAlbum, norm(s.getAlbum()), s);
            index(byMood, s.getMood(), s);
        }

        connectGroups(byArtist);
        connectGroups(byAlbum);
        connectGroups(byMood);
    }

    /** Songs closest to {@code start} in BFS order, not including start itself. */
    public List<Song> related(Song start, int max) {
        List<Song> out = new ArrayList<>();
        if (start == null || max <= 0 || adj.get(start.getPath()) == null) return out;

        HashTable<String, Boolean> seen = new HashTable<>();
        MyQueue<Song> q = new MyQueue<>();
        seen.put(start.getPath(), Boolean.TRUE);
        q.enqueue(start);

        while (!q.isEmpty() && out.size() < max) {
            Song u = q.dequeue();
            List<Song> nbrs = adj.get(u.getPath());
            if (nbrs == null) continue;
            for (Song v : nbrs) {
                if (seen.containsKey(v.getPath())) continue;
                seen.put(v.getPath(), Boolean.TRUE);
                out.add(v);
                if (out.size() >= max) break;
                q.enqueue(v);
            }
        }
        return out;
    }

    private void connectGroups(HashTable<String, List<Song>> groups) {
        for (List<Song> group : groups.values()) {
            if (group.size() < 2) continue;
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    link(group.get(i), group.get(j));
                }
            }
        }
    }

    private void link(Song a, Song b) {
        if (a == b) return;
        List<Song> na = adj.get(a.getPath());
        List<Song> nb = adj.get(b.getPath());
        if (na == null || nb == null) return;
        if (!na.contains(b)) na.add(b);
        if (!nb.contains(a)) nb.add(a);
    }

    private static void index(HashTable<String, List<Song>> table, String key, Song s) {
        if (key == null || key.isEmpty()) return;
        List<Song> list = table.get(key);
        if (list == null) {
            list = new ArrayList<>();
            table.put(key, list);
        }
        list.add(s);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}
