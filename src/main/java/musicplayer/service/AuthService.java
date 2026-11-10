package musicplayer.service;

import musicplayer.db.DatabaseException;
import musicplayer.db.UserRepository;
import musicplayer.model.User;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Responsibility: the rules of registration and login - validating input,
 * refusing duplicates and slowing down password guessing. It talks to the database only through
 * UserRepository and knows nothing about the screen.
 */
public class AuthService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,30}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 30_000;
    private static final String BAD_LOGIN = "Wrong user name / e-mail or password.";   // same text for both cases on purpose

    private static final class Attempts {
        int failures;
        long lockedUntil;
    }

    private final UserRepository users;
    private final Map<String, Attempts> attempts = new HashMap<>();

    public AuthService(UserRepository users) {
        this.users = users;
    }

    // ---- validation (public + static so it can be tested and reused by the screen) ----
    /** Returns an error message, or null when the value is fine. */
    public static String checkUsername(String u) {
        if (u == null || !USERNAME.matcher(u).matches()) return "User name: 3-30 letters, digits or _ (no spaces).";
        return null;
    }

    public static String checkEmail(String e) {
        if (e == null || e.length() > 120 || !EMAIL.matcher(e).matches()) return "Please enter a valid e-mail address.";
        return null;
    }

    public static String checkPassword(String p) {
        if (p == null || p.length() < 8) return "Password: at least 8 characters.";
        if (p.length() > 128) return "Password: at most 128 characters.";
        boolean letter = false, digit = false;
        for (char c : p.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) return "Password: use at least one letter and one digit.";
        return null;
    }

    // ---- registration ----
    public User register(String username, String email, String password, String confirm) throws AuthException {
        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim();
        String problem = firstNonNull(checkUsername(username), checkEmail(email), checkPassword(password));
        if (problem != null) throw new AuthException(problem);
        if (!password.equals(confirm)) throw new AuthException("The two passwords are not the same.");
        try {
            if (users.usernameExists(username)) throw new AuthException("That user name is already taken.");
            if (users.emailExists(email.toLowerCase(Locale.ROOT))) throw new AuthException("That e-mail is already registered.");
            User u = users.insert(username, email.toLowerCase(Locale.ROOT), password, username);
            users.touchLastLogin(u.id());
            return u;
        } catch (DatabaseException e) {
            if (e.isDuplicate()) throw new AuthException("That user name or e-mail is already registered.", e);
            throw new AuthException(e.getMessage(), e);
        }
    }

    // ---- login ----
    public User login(String login, String password) throws AuthException {
        login = login == null ? "" : login.trim();
        if (login.isEmpty() || password == null || password.isEmpty()) throw new AuthException("Enter your user name (or e-mail) and password.");
        String key = login.toLowerCase(Locale.ROOT);
        checkNotLocked(key);
        try {
            Optional<UserRepository.Stored> found = users.findByLogin(login);
            if (found.isEmpty() || !password.equals(found.get().password())) {
                registerFailure(key);
                throw new AuthException(BAD_LOGIN);
            }
            synchronized (attempts) {
                attempts.remove(key);
            }
            users.touchLastLogin(found.get().user().id());
            return found.get().user();
        } catch (DatabaseException e) {
            throw new AuthException(e.getMessage(), e);
        }
    }

    private void checkNotLocked(String key) throws AuthException {
        synchronized (attempts) {
            Attempts a = attempts.get(key);
            if (a != null && a.lockedUntil > System.currentTimeMillis()) {
                long secs = (a.lockedUntil - System.currentTimeMillis() + 999) / 1000;
                throw new AuthException("Too many wrong attempts. Try again in " + secs + " seconds.");
            }
        }
    }

    private void registerFailure(String key) {
        synchronized (attempts) {
            Attempts a = attempts.computeIfAbsent(key, k -> new Attempts());
            if (++a.failures >= MAX_FAILURES) {
                a.failures = 0;
                a.lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
            }
        }
    }

    // ---- account details ----
    public User changeDisplayName(User user, String name) throws AuthException {
        name = name == null ? "" : name.trim();
        if (name.isEmpty() || name.length() > 60) throw new AuthException("The name must be 1-60 characters.");
        try {
            users.updateDisplayName(user.id(), name);
        } catch (DatabaseException e) {
            throw new AuthException(e.getMessage(), e);
        }
        return user.withDisplayName(name);
    }

    /** Remembers the user's music folder so it can be loaded automatically next time. */
    public User rememberMusicFolder(User user, File folder) throws AuthException {
        try {
            users.updateMusicFolder(user.id(), folder.getAbsolutePath());
        } catch (DatabaseException e) {
            throw new AuthException(e.getMessage(), e);
        }
        return user.withMusicFolder(folder.getAbsolutePath());
    }

    private static String firstNonNull(String... values) {
        for (String v : values) if (v != null) return v;
        return null;
    }
}
