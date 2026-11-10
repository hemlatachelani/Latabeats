package musicplayer.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Small per-computer settings: remembers the last user name so the login box can be pre-filled. */
public class LocalSettings {
    private final File file = resolveFile();
    private final Properties props = new Properties();

    private static File resolveFile() {
        File primary = new File(System.getProperty("user.home"), ".latabeats" + File.separator + "app.properties");
        if (primary.exists()) return primary;
        File legacy = new File(System.getProperty("user.home"), ".nocturne" + File.separator + "app.properties");
        if (legacy.exists()) return legacy;
        return primary;
    }

    public LocalSettings() {
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException ignored) {
                // start empty
            }
        }
    }

    public String lastUser() {
        return props.getProperty("lastUser", "");
    }

    public void setLastUser(String username) {
        props.setProperty("lastUser", username);
        file.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "LataBeats settings");
        } catch (IOException ignored) {
            // not critical
        }
    }
}
