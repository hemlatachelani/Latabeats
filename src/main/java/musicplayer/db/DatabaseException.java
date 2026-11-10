package musicplayer.db;

/** A database problem with a message that is safe and clear enough to show to the user. */
public class DatabaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final boolean duplicate;

    public DatabaseException(String message, Throwable cause) {
        this(message, cause, false);
    }

    public DatabaseException(String message, Throwable cause, boolean duplicate) {
        super(message, cause);
        this.duplicate = duplicate;
    }

    /** True when a UNIQUE key was violated (for example: that username already exists). */
    public boolean isDuplicate() { return duplicate; }
}
