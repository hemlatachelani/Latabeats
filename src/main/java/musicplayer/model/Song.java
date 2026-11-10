package musicplayer.model;

public class Song {
    private String title;
    private String artist;
    private String album = "";
    private final String path;
    private String mood = "Calm";
    private boolean liked;
    private double durationSeconds;
    private int playCount;

    public Song(String title, String artist, String path) {
        this.title = title;
        this.artist = artist;
        this.path = path;
    }

    public String getTitle()  { return title; }
    public String getArtist() { return artist; }
    public String getAlbum()  { return album; }
    public String getPath()   { return path; }

    public void setTitle(String title)   { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setAlbum(String album)   { this.album = album; }

    public String getMood()           { return mood; }
    public void setMood(String mood)  { this.mood = mood; }

    public boolean isLiked()          { return liked; }
    public void setLiked(boolean l)   { this.liked = l; }

    public double getDurationSeconds()        { return durationSeconds; }
    public void setDurationSeconds(double s)  { this.durationSeconds = s; }

    public int getPlayCount()         { return playCount; }
    public void setPlayCount(int n)   { playCount = Math.max(0, n); }
    public void incrementPlayCount()  { playCount++; }

    public String getDurationText() {
        if (durationSeconds <= 0) return "--:--";
        int s = (int) durationSeconds;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override
    public String toString() {
        return artist.isEmpty() ? title : title + " - " + artist;
    }
}
