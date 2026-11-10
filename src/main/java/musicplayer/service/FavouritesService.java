package musicplayer.service;

import musicplayer.db.DatabaseException;
import musicplayer.db.FavouriteRepository;
import musicplayer.model.Song;
import musicplayer.model.User;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Responsibility: the favourite songs of ONE logged-in user. The set of favourite paths is kept in
 * memory (fast screen updates) and every change is written to MySQL in the background, in order.
 */
public class FavouritesService {

    private final FavouriteRepository repository;
    private final User user;
    private final Set<String> paths = ConcurrentHashMap.newKeySet();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "favourites-writer");
        t.setDaemon(true);
        return t;
    });
    private volatile Consumer<String> errorHandler = message -> { };

    public FavouritesService(FavouriteRepository repository, User user) {
        this.repository = repository;
        this.user = user;
    }

    /** Called with a readable message when saving to the database fails. */
    public void setErrorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
    }

    /** Reads this user's favourites from the database (blocking - call it off the UI thread). */
    public void load() {
        Set<String> fromDb = repository.findPaths(user.id());
        paths.clear();
        paths.addAll(fromDb);
    }

    /** Marks the songs that are in this user's favourites. */
    public void applyTo(Collection<Song> songs) {
        for (Song s : songs) s.setLiked(paths.contains(s.getPath()));
    }

    public int count() {
        return paths.size();
    }

    public void setFavourite(Song song, boolean favourite) {
        song.setLiked(favourite);
        if (favourite) paths.add(song.getPath());
        else paths.remove(song.getPath());
        String path = song.getPath(), title = song.getTitle(), artist = song.getArtist();
        writer.submit(() -> {
            try {
                if (favourite) repository.add(user.id(), path, title, artist);
                else repository.remove(user.id(), path);
            } catch (DatabaseException e) {
                errorHandler.accept("Could not save your favourite: " + e.getMessage());
            }
        });
    }

    /** Waits for pending writes, then stops the writer (call on logout / exit). */
    public void close() {
        writer.shutdown();
        try {
            writer.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
