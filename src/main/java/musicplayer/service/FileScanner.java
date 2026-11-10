package musicplayer.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Responsibility: finding audio files on disk. */
public final class FileScanner {
    private static final int MAX_DEPTH = 3;

    private FileScanner() { }

    public static boolean isAudio(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a")
                || n.endsWith(".mp4") || n.endsWith(".m4v");
    }

    /** All audio files in a folder and its sub-folders (3 levels deep), sorted by name. */
    public static List<File> audioFilesIn(File dir) {
        return audioFilesIn(dir, 0);
    }

    private static List<File> audioFilesIn(File dir, int depth) {
        List<File> out = new ArrayList<>();
        File[] list = dir.listFiles();
        if (list == null) return out;
        Arrays.sort(list);
        for (File f : list) {
            if (f.isDirectory() && depth < MAX_DEPTH) out.addAll(audioFilesIn(f, depth + 1));
            else if (f.isFile() && isAudio(f)) out.add(f);
        }
        return out;
    }

    /** Turns command-line arguments (files and/or folders) into a list of audio files. */
    public static List<File> collect(List<String> paths) {
        List<File> out = new ArrayList<>();
        for (String p : paths) {
            File f = new File(p);
            if (f.isDirectory()) out.addAll(audioFilesIn(f));
            else if (f.isFile()) out.add(f);
        }
        return out;
    }
}
