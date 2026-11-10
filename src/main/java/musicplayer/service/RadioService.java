package musicplayer.service;

import musicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;

/** Responsibility: deciding WHICH stations exist and which songs each one draws from. */
public class RadioService {
    private final MusicLibrary library;
    private final PlaybackService playback;
    private final SongGraph graph;

    public RadioService(MusicLibrary library, PlaybackService playback, SongGraph graph) {
        this.library = library;
        this.playback = playback;
        this.graph = graph;
    }

    public List<Station> stations() {
        List<Station> l = new ArrayList<>();
        l.add(new Station("All songs", "Shuffle your whole library", "all", () -> new ArrayList<>(library.songs())));
        l.add(new Station("Favourites mix", "Songs you've hearted", "favourites", library::liked));
        l.add(new Station("Discover", "Songs you've played the least", "discover", () -> library.leastPlayed(15)));
        l.add(new Station("More like this", "BFS from the song that is playing", "related", this::relatedPool));
        for (String mood : MusicLibrary.MOODS) {
            l.add(new Station(mood + " radio", "Songs that feel " + mood.toLowerCase(), mood, () -> library.withMood(mood)));
        }
        return l;
    }

    private List<Song> relatedPool() {
        Song seed = playback.nowPlaying();
        if (seed == null) seed = playback.currentSong();
        if (seed == null) return List.of();
        List<Song> pool = new ArrayList<>();
        pool.add(seed);
        pool.addAll(graph.related(seed, 24));
        return pool;
    }
}
