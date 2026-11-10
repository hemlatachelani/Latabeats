package musicplayer.service;

/** Registration / login failed. The message is written for the user and can be shown as it is. */
public class AuthException extends Exception {
    private static final long serialVersionUID = 1L;
    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
