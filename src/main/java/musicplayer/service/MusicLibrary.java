package musicplayer.service;

import musicplayer.algo.MergeSort;
import musicplayer.algo.TopK;
import musicplayer.ds.HashTable;
import musicplayer.ds.Trie;
import musicplayer.model.Song;
import musicplayer.util.TagReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Responsibility: the collection of songs - adding/removing, ordering (merge sort), searching (trie)
 * and simple queries (favourites, mood, least/most played). Knows nothing about JavaFX or playback.
 */
public class MusicLibrary {

    public static final String[] MOODS = {"Late night", "Energised", "Calm", "Dreamy", "Melancholy", "Soulful"};
    private static final String TOKEN_SPLIT = "[\\s\\-_.,()]+";

    private final List<Song> addedOrder = new ArrayList<>();      // "Date added" order
    private final List<Song> sorted = new ArrayList<>();          // current order, shown to everybody
    private final Playlist playlist = new Playlist();             // linked list kept in the same order
    private final HashTable<String, Song> byPath = new HashTable<>(); // O(1) lookup by file path
    private final Trie<Song> searchIndex = new Trie<>();
    private SortOrder sortOrder = SortOrder.DATE_ADDED;
    private int addedCount = 0;

    // ---- the collection ----
    public List<Song> songs()      { return Collections.unmodifiableList(sorted); }
    public Playlist playlist()     { return playlist; }
    public int size()              { return sorted.size(); }
    public boolean isEmpty()       { return sorted.isEmpty(); }

    public boolean contains(File f) {
        return byPath.containsKey(f.getAbsolutePath());
    }

    public Song byPath(String path) {
        return path == null ? null : byPath.get(path);
    }

    /** Adds one file ("Artist - Title.mp3" is split into artist/title). Returns null if it was already there. */
    public Song add(File f) {
        if (contains(f)) return null;
        String name = f.getName().replaceFirst("\\.[^.]+$", "");
        String title = name, artist = "";
        int dash = name.indexOf(" - ");
        if (dash > 0) {
            artist = name.substring(0, dash).trim();
            title = name.substring(dash + 3).trim();
        }
        Song s = new Song(title, artist, f.getAbsolutePath());
        s.setMood(MOODS[addedCount++ % MOODS.length]);            // default mood, the user can change it
        addedOrder.add(s);
        byPath.put(s.getPath(), s);
        index(s);
        return s;
    }

    /** Adds many files, re-sorts once, returns only the songs that were really new. */
    public List<Song> addAll(List<File> files) {
        List<Song> added = new ArrayList<>();
        for (File f : files) {
            Song s = add(f);
            if (s != null) added.add(s);
        }
        resort();
        return added;
    }

    public void remove(Song s) {
        addedOrder.remove(s);
        sorted.remove(s);
        playlist.remove(s);
        byPath.remove(s.getPath());
        reindex();
    }

    /** Restores moods, play counts and tags saved from the previous session. */
    public void applySaved(LibraryStore.Snapshot snap) {
        if (snap == null) return;
        for (LibraryStore.SongRecord r : snap.songs) {
            Song s = byPath.get(r.path);
            if (s == null) continue;
            if (r.mood != null && !r.mood.isBlank()) s.setMood(r.mood);
            if (r.playCount > s.getPlayCount()) s.setPlayCount(r.playCount);
            if (r.duration > 0) s.setDurationSeconds(r.duration);
            if (r.title != null && !r.title.isBlank()) s.setTitle(r.title);
            if (r.artist != null) s.setArtist(r.artist);
            if (r.album != null) s.setAlbum(r.album);
        }
        reindex();
        resort();
    }

    // ---- ordering ----
    public SortOrder sortOrder() { return sortOrder; }

    public void setSortOrder(SortOrder order) {
        this.sortOrder = order;
        resort();
    }

    /** Merge sort is stable, so ties keep their "date added" order. The playlist's linked list is rebuilt too. */
    public void resort() {
        List<Song> list = new ArrayList<>(addedOrder);
        if (sortOrder.comparator() != null) MergeSort.sort(list, sortOrder.comparator());
        sorted.clear();
        sorted.addAll(list);
        playlist.replaceAll(sorted);
    }

    // ---- tags read from the file ----
    /** Copies title/artist/album found in the file into the song. Returns true if anything changed. */
    public boolean applyTags(Song s, TagReader.Tags tags) {
        boolean changed = false;
        if (tags.title != null && !tags.title.equals(s.getTitle())) {
            s.setTitle(tags.title);
            changed = true;
        }
        if (tags.artist != null && !tags.artist.equals(s.getArtist())) {
            s.setArtist(tags.artist);
            changed = true;
        }
        if (tags.album != null && !tags.album.equals(s.getAlbum())) {
            s.setAlbum(tags.album);
            changed = true;
        }
        if (changed) {
            reindex();          // search must know the real title/artist/album
            resort();           // keep the chosen order
        }
        return changed;
    }

    // ---- search ----
    /** Prefix search over every word of title/artist/album. Several words = set intersection. */
    public Set<Song> search(String query) {
        Set<Song> result = null;
        for (String token : query.toLowerCase().split(TOKEN_SPLIT)) {
            if (token.isEmpty()) continue;
            Set<Song> set = new HashSet<>(searchIndex.startsWith(token, Integer.MAX_VALUE));
            if (result == null) result = set;
            else result.retainAll(set);
        }
        return result == null ? new HashSet<>() : result;
    }

    private void index(Song s) {
        String text = (s.getTitle() + " " + s.getArtist() + " " + s.getAlbum()).toLowerCase();
        for (String token : text.split(TOKEN_SPLIT)) {
            if (!token.isEmpty()) searchIndex.insert(token, s);
        }
    }

    private void reindex() {
        searchIndex.clear();
        for (Song s : addedOrder) index(s);
    }

    // ---- queries ----
    public List<Song> liked() {
        List<Song> l = new ArrayList<>();
        for (Song s : sorted) if (s.isLiked()) l.add(s);
        return l;
    }

    public int likedCount() { return liked().size(); }

    public List<Song> withMood(String mood) {
        List<Song> l = new ArrayList<>();
        for (Song s : sorted) if (s.getMood().equals(mood)) l.add(s);
        return l;
    }

    public List<Song> leastPlayed(int max) {
        List<Song> l = new ArrayList<>(sorted);
        MergeSort.sort(l, (a, b) -> Integer.compare(a.getPlayCount(), b.getPlayCount()));
        return l.size() > max ? new ArrayList<>(l.subList(0, max)) : l;
    }

    /** Top-K with a min-heap. Only songs that were played at least once. */
    public List<Song> mostPlayed(int k) {
        List<Song> played = new ArrayList<>();
        for (Song s : sorted) if (s.getPlayCount() > 0) played.add(s);
        return TopK.topK(played, k, (a, b) -> Integer.compare(a.getPlayCount(), b.getPlayCount()));
    }
}
