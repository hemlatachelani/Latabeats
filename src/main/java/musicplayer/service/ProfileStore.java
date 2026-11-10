package musicplayer.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Responsibility: remembering one user's profile picture (a local file path) between runs. */
public class ProfileStore {

    private final File file;
    private final Properties props = new Properties();

    /** One file per user name (user names only contain letters, digits and _, so this is safe). */
    public static ProfileStore forUser(String username) {
        File primary = new File(System.getProperty("user.home"),
                ".latabeats" + File.separator + "profile_" + username + ".properties");
        if (primary.exists()) return new ProfileStore(primary);
        File legacy = new File(System.getProperty("user.home"),
                ".nocturne" + File.separator + "profile_" + username + ".properties");
        if (legacy.exists()) return new ProfileStore(legacy);
        return new ProfileStore(primary);
    }

    public ProfileStore(File file) {
        this.file = file;
    }

    public void load() {
        if (!file.exists()) return;
        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (IOException ignored) {
            // start with an empty profile
        }
    }

    public void save() {
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "LataBeats profile picture");
        } catch (IOException ignored) {
            // not critical: it just won't be remembered next time
        }
    }

    /** The picture file, or null when none was chosen or it no longer exists. */
    public File photo() {
        String p = props.getProperty("photo");
        if (p == null) return null;
        File f = new File(p);
        return f.exists() ? f : null;
    }

    public void setPhoto(File f) {
        props.setProperty("photo", f.getAbsolutePath());
    }

    public void clearPhoto() {
        props.remove("photo");
    }
}
