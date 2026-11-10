package musicplayer.model;

/** A registered user. Never contains the password (only the database layer ever sees it). */
public record User(int id, String username, String email, String displayName, String musicFolder) {

    public User withDisplayName(String name)   { return new User(id, username, email, name, musicFolder); }
    public User withMusicFolder(String folder) { return new User(id, username, email, displayName, folder); }
}
