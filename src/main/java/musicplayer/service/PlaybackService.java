package musicplayer.service;

import musicplayer.algo.Shuffle;
import musicplayer.ds.LRUCache;
import musicplayer.ds.MyQueue;
import musicplayer.ds.MyStack;
import musicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Responsibility: deciding WHAT plays next - the Up Next queue, radio stations, shuffle, repeat,
 * the Back history and "recently played". It never touches an audio device or any UI class;
 * the window just asks "what's next?" and plays whatever song it gets back.
 */
public class PlaybackService {

    private final MusicLibrary library;
    private final Playlist playlist;
    private final MyStack<Song> history = new MyStack<>();              // Back button
    private final MyQueue<Song> queue = new MyQueue<>();                // Up Next + radio
    private final LRUCache<Song, Song> recent = new LRUCache<>(8);      // Recently played
    private final Random rng = new Random();

    private Station station;
    private boolean shuffle;
    private Song nowPlaying;

    public PlaybackService(MusicLibrary library) {
        this.library = library;
        this.playlist = library.playlist();
    }

    // ---- state ----
    public Song nowPlaying()         { return nowPlaying; }
    public Song currentSong()        { return playlist.current(); }
    public void select(Song s)       { playlist.select(s); }
    public void setShuffle(boolean on)    { shuffle = on; }
    public void setRepeatAll(boolean on)  { playlist.setRepeatAll(on); }

    /** Bookkeeping when a song really starts playing. */
    public void started(Song s, boolean recordHistory) {
        if (recordHistory && nowPlaying != null && nowPlaying != s) history.push(nowPlaying);
        nowPlaying = s;
        playlist.select(s);
        s.incrementPlayCount();
        recent.put(s, s);
    }

    /** A song was deleted from the library: forget it everywhere. */
    public void forget(Song s) {
        while (queue.remove(s)) { /* every occurrence */ }
        recent.remove(s);
        if (nowPlaying == s) nowPlaying = null;
    }

    // ---- what plays next ----
    /** Priority: queue (including radio) -> shuffle -> next song in the linked list. */
    public Song next() {
        if (queue.isEmpty() && station != null) refillFromStation();     // radio never runs dry
        if (!queue.isEmpty()) {
            Song s = queue.dequeue();
            playlist.select(s);
            return s;
        }
        List<Song> all = library.songs();
        if (shuffle && all.size() > 1) {
            Song s;
            do {
                s = all.get(rng.nextInt(all.size()));
            } while (s == nowPlaying);
            playlist.select(s);
            return s;
        }
        return playlist.next();
    }

    public Song previous() {
        return playlist.previous();
    }

    /** The song played before this one (pops the history stack, skipping deleted songs). */
    public Song back() {
        Song s = history.pop();
        while (s != null && !library.songs().contains(s)) s = history.pop();
        if (s != null) playlist.select(s);
        return s;
    }

    // ---- the queue ----
    public void enqueue(Song s)       { queue.enqueue(s); }
    public void enqueueFront(Song s)  { queue.enqueueFront(s); }
    public void removeFromQueue(Song s) { queue.remove(s); }
    public void clearQueue()          { queue.clear(); }
    public List<Song> queue()         { return queue.toList(); }
    public int queueSize()            { return queue.size(); }

    /** Takes the first queued song (used by the Queue view's Play button). */
    public Song takeFromQueue() {
        Song s = queue.dequeue();
        if (s != null) playlist.select(s);
        return s;
    }

    /** Play a list (a mood, the favourites ...): first song now, the rest through the queue. */
    public Song playContext(List<Song> songs) {
        if (songs.isEmpty()) return null;
        station = null;
        queue.clear();
        for (int i = 1; i < songs.size(); i++) queue.enqueue(songs.get(i));
        Song first = songs.get(0);
        playlist.select(first);
        return first;
    }

    // ---- radio ----
    public Station station() { return station; }

    /** Starts a station. Returns the first song, or null when the station has no songs. */
    public Song startStation(Station st) {
        if (st.songs().isEmpty()) return null;
        station = st;
        queue.clear();
        refillFromStation();
        Song first = queue.dequeue();
        playlist.select(first);
        return first;
    }

    public void stopStation() {
        station = null;
        queue.clear();
    }

    /** Shuffles the station's songs with Fisher-Yates and pushes them into the queue. */
    private void refillFromStation() {
        List<Song> pool = new ArrayList<>(station.songs());
        Shuffle.fisherYates(pool, rng);
        if (pool.size() > 1 && pool.get(0) == nowPlaying) {              // avoid an instant repeat
            Song t = pool.get(0);
            pool.set(0, pool.get(1));
            pool.set(1, t);
        }
        for (Song s : pool) queue.enqueue(s);
    }

    // ---- recently played ----
    public List<Song> recentlyPlayed() { return recent.toList(); }

    /** Rebuilds the LRU shelf from saved paths (most-recent first) without changing play counts. */
    public void restoreRecent(List<String> mruFirst, MusicLibrary library) {
        if (mruFirst == null || mruFirst.isEmpty()) return;
        for (int i = mruFirst.size() - 1; i >= 0; i--) {
            Song s = library.byPath(mruFirst.get(i));
            if (s != null) recent.put(s, s);
        }
    }
}
