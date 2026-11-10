package musicplayer.service;

import musicplayer.util.TagReader;

import java.io.File;

/**
 * Responsibility: finding out everything a file knows about itself (tags, cover picture, length).
 * It is plain blocking code with no UI types - call it from a background thread.
 */
public class MetadataService {

    /** Cover is either raw bytes from the tag, or a picture file found next to the song, or neither. */
    public static final class Result {
        public final TagReader.Tags tags;
        public final File coverFile;
        public final double seconds;
        private final String coverSource;

        Result(TagReader.Tags tags, File coverFile, double seconds, String coverSource) {
            this.tags = tags;
            this.coverFile = coverFile;
            this.seconds = seconds;
            this.coverSource = coverSource;
        }

        public byte[] coverBytes() { return tags.cover; }

        @Override
        public String toString() {
            return tags + "  (cover: " + coverSource + ", length: " + String.format("%.1f", seconds) + " s)";
        }
    }

    public Result load(File file) {
        TagReader.Tags tags = TagReader.read(file);
        File coverFile = null;
        String source = tags.cover != null ? "embedded picture" : "none";
        if (tags.cover == null) {
            File art = TagReader.findFolderArt(file.getParentFile());
            if (art != null) {
                coverFile = art;
                source = "folder image " + art.getName();
            }
        }
        double seconds = TagReader.durationSeconds(file);
        Result r = new Result(tags, coverFile, seconds, source);
        System.out.println("[tags] " + file.getName() + " -> " + r);
        return r;
    }
}
