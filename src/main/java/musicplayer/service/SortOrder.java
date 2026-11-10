package musicplayer.service;

import musicplayer.model.Song;

import java.util.Comparator;

/** The ways the library can be ordered. DATE_ADDED has no comparator = the order songs were added. */
public enum SortOrder {
    DATE_ADDED("Date added", null),
    TITLE("Title A-Z", (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getTitle(), b.getTitle())),
    ARTIST("Artist A-Z", (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getArtist(), b.getArtist())),
    DURATION("Duration (shortest)", (a, b) -> Double.compare(a.getDurationSeconds(), b.getDurationSeconds())),
    MOST_PLAYED("Most played", (a, b) -> Integer.compare(b.getPlayCount(), a.getPlayCount())),
    MOOD("Mood", (a, b) -> a.getMood().compareTo(b.getMood()));

    private final String label;
    private final Comparator<Song> comparator;

    SortOrder(String label, Comparator<Song> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public String label()                 { return label; }
    public Comparator<Song> comparator()  { return comparator; }

    public static SortOrder fromLabel(String label) {
        for (SortOrder o : values()) if (o.label.equals(label)) return o;
        return DATE_ADDED;
    }

    public static String[] labels() {
        SortOrder[] all = values();
        String[] out = new String[all.length];
        for (int i = 0; i < all.length; i++) out[i] = all[i].label;
        return out;
    }
}
