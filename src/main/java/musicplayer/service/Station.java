package musicplayer.service;

import musicplayer.model.Song;

import java.util.List;
import java.util.function.Supplier;

/** A radio station = a name + a rule that picks songs from the library. No UI types in here. */
public final class Station {
    private final String name;
    private final String description;
    private final String accent;                 // "all", "favourites", "discover" or a mood name (the UI maps it to a colour)
    private final Supplier<List<Song>> source;

    public Station(String name, String description, String accent, Supplier<List<Song>> source) {
        this.name = name;
        this.description = description;
        this.accent = accent;
        this.source = source;
    }

    public String name()         { return name; }
    public String description()  { return description; }
    public String accent()       { return accent; }
    public List<Song> songs()    { return source.get(); }
}
