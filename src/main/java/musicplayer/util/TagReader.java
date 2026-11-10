package musicplayer.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Reads the tags that are stored INSIDE an audio file - written from scratch, no libraries.
 *
 *   MP3 : ID3v2.2 / v2.3 / v2.4 (title, artist, album, embedded picture)  + ID3v1 as a fallback
 *   M4A : MP4 "ilst" atoms (title, artist, album, cover)
 *
 * Also offers findFolderArt() which looks for cover.jpg / folder.jpg ... next to the song.
 */
public final class TagReader {

    /** What we found in the file. Fields stay null when the file does not contain them. */
    public static final class Tags {
        public String title, artist, album, albumArtist;
        public byte[] cover;
        private int coverType = -1;

        public boolean any() { return title != null || artist != null || album != null || cover != null; }

        @Override
        public String toString() {
            return "title=" + title + ", artist=" + artist + ", album=" + album
                    + ", cover=" + (cover == null ? "none" : cover.length + " bytes");
        }
    }

    private static final int MAX_TAG = 64 * 1024 * 1024;     // sanity limits so a broken file can't eat memory
    private static final int MAX_PICTURE = 25 * 1024 * 1024;

    private TagReader() { }

    /** Never throws: on any problem you simply get fewer fields. */
    public static Tags read(File f) {
        Tags t = new Tags();
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            byte[] head = new byte[12];
            int n = 0;
            while (n < head.length) {
                int r = raf.read(head, n, head.length - n);
                if (r < 0) break;
                n += r;
            }
            if (n >= 10 && head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
                readId3v2(raf, head, t);
            } else if (n >= 12 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') {
                readMp4(raf, t);
            }
            if (t.title == null && t.artist == null && t.album == null) readId3v1(raf, t);
        } catch (Exception e) {
            System.err.println("[tags] could not fully read " + f.getName() + ": " + e);
        }
        if (t.artist == null) t.artist = t.albumArtist;
        return t;
    }

    // ------------------------------------------------------------------
    //  ID3v2
    // ------------------------------------------------------------------
    private static void readId3v2(RandomAccessFile raf, byte[] head, Tags t) throws IOException {
        int major = head[3] & 0xff;                 // 2, 3 or 4
        int flags = head[5] & 0xff;
        int size = syncsafe(head, 6);
        if (major < 2 || major > 4 || size <= 0 || size > MAX_TAG) return;
        size = (int) Math.min(size, Math.max(0, raf.length() - 10));
        byte[] tag = new byte[size];
        raf.seek(10);
        raf.readFully(tag);

        if ((flags & 0x80) != 0 && major < 4) tag = unsync(tag);       // tag-wide unsynchronisation

        int pos = 0;
        if ((flags & 0x40) != 0 && major >= 3 && tag.length >= 4) {    // skip extended header
            pos = major == 3 ? 4 + be32(tag, 0) : syncsafe(tag, 0);
        }
        int idLen = major == 2 ? 3 : 4;
        int hdr = major == 2 ? 6 : 10;

        while (pos >= 0 && pos + hdr <= tag.length) {
            if (tag[pos] == 0) break;                                  // padding reached
            String id = new String(tag, pos, idLen, StandardCharsets.ISO_8859_1);
            int fsize, fflags = 0;
            if (major == 2) {
                fsize = be24(tag, pos + 3);
            } else if (major == 3) {
                fsize = be32(tag, pos + 4);
                fflags = be16(tag, pos + 8);
            } else {
                fsize = syncsafe(tag, pos + 4);
                fflags = be16(tag, pos + 8);
            }
            int start = pos + hdr;
            if (fsize < 0 || start + fsize > tag.length) break;
            pos = start + fsize;
            if (fsize == 0) continue;

            boolean skip = false;
            int dataStart = start;
            int dataEnd = start + fsize;
            if (major == 3) {
                if ((fflags & 0x00C0) != 0) skip = true;               // compressed / encrypted
                if ((fflags & 0x0020) != 0) dataStart++;               // grouping byte
            } else if (major == 4) {
                if ((fflags & 0x000C) != 0) skip = true;               // compressed / encrypted
                if ((fflags & 0x0040) != 0) dataStart++;               // grouping byte
                if ((fflags & 0x0001) != 0) dataStart += 4;            // data length indicator
            }
            if (skip || dataStart >= dataEnd) continue;
            byte[] data = Arrays.copyOfRange(tag, dataStart, dataEnd);
            if (major == 4 && (fflags & 0x0002) != 0) data = unsync(data);
            handleFrame(id, data, t);
        }
    }

    private static void handleFrame(String id, byte[] d, Tags t) {
        switch (id) {
            case "TIT2": case "TT2":
                if (t.title == null) t.title = text(d);
                break;
            case "TPE1": case "TP1":
                if (t.artist == null) t.artist = text(d);
                break;
            case "TPE2": case "TP2":
                if (t.albumArtist == null) t.albumArtist = text(d);
                break;
            case "TALB": case "TAL":
                if (t.album == null) t.album = text(d);
                break;
            case "APIC": case "PIC":
                readPicture(id, d, t);
                break;
            default:
                break;
        }
    }

    private static void readPicture(String id, byte[] d, Tags t) {
        if (d.length < 8 || d.length > MAX_PICTURE) return;
        int enc = d[0] & 0xff;
        int p = 1;
        if (id.equals("PIC")) {
            p += 3;                                                    // 3-letter image format ("JPG")
        } else {
            int end = indexOfZero(d, p);                               // MIME type, zero terminated
            if (end < 0) return;
            p = end + 1;
        }
        if (p >= d.length) return;
        int picType = d[p++] & 0xff;                                   // 3 = front cover
        if (enc == 1 || enc == 2) {                                    // UTF-16 description: double zero
            while (p + 1 < d.length && !(d[p] == 0 && d[p + 1] == 0)) p += 2;
            p += 2;
        } else {
            int e = indexOfZero(d, p);
            if (e < 0) return;
            p = e + 1;
        }
        if (p >= d.length) return;
        byte[] img = Arrays.copyOfRange(d, p, d.length);
        if (!looksLikeImage(img)) return;
        if (t.cover == null || (picType == 3 && t.coverType != 3)) {   // prefer the front cover
            t.cover = img;
            t.coverType = picType;
        }
    }

    // ------------------------------------------------------------------
    //  ID3v1 (last 128 bytes of an MP3)
    // ------------------------------------------------------------------
    private static void readId3v1(RandomAccessFile raf, Tags t) throws IOException {
        long len = raf.length();
        if (len < 128) return;
        byte[] b = new byte[128];
        raf.seek(len - 128);
        raf.readFully(b);
        if (b[0] != 'T' || b[1] != 'A' || b[2] != 'G') return;
        if (t.title == null) t.title = clean(new String(b, 3, 30, StandardCharsets.ISO_8859_1));
        if (t.artist == null) t.artist = clean(new String(b, 33, 30, StandardCharsets.ISO_8859_1));
        if (t.album == null) t.album = clean(new String(b, 63, 30, StandardCharsets.ISO_8859_1));
    }

    // ------------------------------------------------------------------
    //  MP4 / M4A atoms
    // ------------------------------------------------------------------
    private static void readMp4(RandomAccessFile raf, Tags t) throws IOException {
        parseAtoms(raf, 0, raf.length(), 0, t);
    }

    private static void parseAtoms(RandomAccessFile raf, long start, long end, int depth, Tags t) throws IOException {
        if (depth > 8) return;
        long pos = start;
        while (pos + 8 <= end) {
            raf.seek(pos);
            long size = u32(raf);
            String type = fourCC(raf);
            long hdr = 8;
            if (size == 1) {
                size = raf.readLong();
                hdr = 16;
            } else if (size == 0) {
                size = end - pos;
            }
            if (size < hdr) return;
            if (pos + size > end) size = end - pos;
            long body = pos + hdr, bodyEnd = pos + size;

            switch (type) {
                case "moov": case "udta": case "ilst":
                    parseAtoms(raf, body, bodyEnd, depth + 1, t);
                    break;
                case "meta":                                            // has 4 bytes of version/flags first
                    parseAtoms(raf, body + 4, bodyEnd, depth + 1, t);
                    break;
                case "\u00a9nam":
                    if (t.title == null) t.title = utf8(dataPayload(raf, body, bodyEnd));
                    break;
                case "\u00a9ART":
                    if (t.artist == null) t.artist = utf8(dataPayload(raf, body, bodyEnd));
                    break;
                case "aART":
                    if (t.albumArtist == null) t.albumArtist = utf8(dataPayload(raf, body, bodyEnd));
                    break;
                case "\u00a9alb":
                    if (t.album == null) t.album = utf8(dataPayload(raf, body, bodyEnd));
                    break;
                case "covr": {
                    byte[] img = dataPayload(raf, body, bodyEnd);
                    if (t.cover == null && img != null && looksLikeImage(img)) t.cover = img;
                    break;
                }
                default:
                    break;
            }
            pos += size;
        }
    }

    /** Content of the 'data' child atom: 8 bytes (version/flags/locale) then the payload. */
    private static byte[] dataPayload(RandomAccessFile raf, long start, long end) throws IOException {
        long pos = start;
        while (pos + 16 <= end) {
            raf.seek(pos);
            long size = u32(raf);
            String type = fourCC(raf);
            if (size < 8) return null;
            if (type.equals("data")) {
                long len = Math.min(size, end - pos) - 16;
                if (len <= 0 || len > MAX_PICTURE) return null;
                raf.seek(pos + 16);
                byte[] b = new byte[(int) len];
                raf.readFully(b);
                return b;
            }
            pos += size;
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Cover image sitting next to the song (cover.jpg, folder.png ...)
    // ------------------------------------------------------------------
    public static File findFolderArt(File dir) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        String[] names = {"cover", "folder", "front", "albumart", "album", "artwork", "thumb"};
        for (String wanted : names) {
            for (File f : files) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                int dot = n.lastIndexOf('.');
                if (dot <= 0) continue;
                String base = n.substring(0, dot), ext = n.substring(dot + 1);
                if (base.equals(wanted) && (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png"))) return f;
            }
        }
        return null;
    }

    public static boolean looksLikeImage(byte[] b) {
        if (b == null || b.length < 4) return false;
        if ((b[0] & 0xff) == 0xFF && (b[1] & 0xff) == 0xD8) return true;                              // JPEG
        if ((b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return true;           // PNG
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F') return true;                                    // GIF
        return b[0] == 'B' && b[1] == 'M';                                                             // BMP
    }

    // ------------------------------------------------------------------
    //  Duration (so we do not depend on the media player to know a song's length)
    // ------------------------------------------------------------------
    private static final int[][] MPEG1_BR = {
            {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448},   // layer I
            {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384},      // layer II
            {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320}};      // layer III
    private static final int[][] MPEG2_BR = {
            {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256},      // layer I
            {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160},           // layer II
            {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}};          // layer III
    private static final int[] SR_MPEG1 = {44100, 48000, 32000};
    private static final int[] SR_MPEG2 = {22050, 24000, 16000};
    private static final int[] SR_MPEG25 = {11025, 12000, 8000};

    /** Length in seconds (MP3, M4A/MP4, WAV). Returns 0 when it cannot be worked out. */
    public static double durationSeconds(File f) {
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            byte[] head = new byte[12];
            int n = 0;
            while (n < head.length) {
                int r = raf.read(head, n, head.length - n);
                if (r < 0) break;
                n += r;
            }
            if (n >= 12 && head[4] == 'f' && head[5] == 't' && head[6] == 'y' && head[7] == 'p') return mp4Duration(raf);
            if (n >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F') return wavDuration(raf);
            return mp3Duration(raf, head, n);
        } catch (Exception e) {
            return 0;
        }
    }

    private static double mp3Duration(RandomAccessFile raf, byte[] head, int headLen) throws IOException {
        long len = raf.length();
        long pos = 0;
        if (headLen >= 10 && head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
            pos = 10L + syncsafe(head, 6);
            if ((head[5] & 0x10) != 0) pos += 10;                       // footer present
        }
        if (pos >= len) return 0;
        int scan = (int) Math.min(131072, len - pos);
        byte[] b = new byte[scan];
        raf.seek(pos);
        raf.readFully(b);

        for (int i = 0; i + 4 <= b.length; i++) {
            if ((b[i] & 0xFF) != 0xFF || (b[i + 1] & 0xE0) != 0xE0) continue;
            int ver = (b[i + 1] >> 3) & 3;                              // 3 = MPEG1, 2 = MPEG2, 0 = MPEG2.5
            int layerBits = (b[i + 1] >> 1) & 3;                        // 1 = III, 2 = II, 3 = I
            int brIdx = (b[i + 2] >> 4) & 0xF;
            int srIdx = (b[i + 2] >> 2) & 3;
            if (ver == 1 || layerBits == 0 || brIdx == 0 || brIdx == 15 || srIdx == 3) continue;
            int layer = 3 - layerBits;                                  // 0 = I, 1 = II, 2 = III
            boolean mpeg1 = ver == 3;
            int bitrate = (mpeg1 ? MPEG1_BR : MPEG2_BR)[layer][brIdx] * 1000;
            int sampleRate = (ver == 3 ? SR_MPEG1 : ver == 2 ? SR_MPEG2 : SR_MPEG25)[srIdx];
            int samplesPerFrame = layer == 0 ? 384 : (layer == 1 || mpeg1) ? 1152 : 576;
            int channelMode = (b[i + 3] >> 6) & 3;                      // 3 = mono

            // VBR/Xing header = exact number of frames
            int sideInfo = mpeg1 ? (channelMode == 3 ? 17 : 32) : (channelMode == 3 ? 9 : 17);
            int xo = i + 4 + sideInfo;
            if (xo + 12 <= b.length) {
                String id = new String(b, xo, 4, StandardCharsets.ISO_8859_1);
                if ((id.equals("Xing") || id.equals("Info")) && (be32(b, xo + 4) & 1) != 0) {
                    long frames = be32(b, xo + 8) & 0xFFFFFFFFL;
                    if (frames > 0) return frames * (double) samplesPerFrame / sampleRate;
                }
            }
            int vo = i + 36;                                            // VBRI header
            if (vo + 18 <= b.length && b[vo] == 'V' && b[vo + 1] == 'B' && b[vo + 2] == 'R' && b[vo + 3] == 'I') {
                long frames = be32(b, vo + 14) & 0xFFFFFFFFL;
                if (frames > 0) return frames * (double) samplesPerFrame / sampleRate;
            }
            // plain constant bitrate: size / bitrate
            long audioBytes = len - (pos + i);
            byte[] tail = new byte[3];
            if (len > 128) {
                raf.seek(len - 128);
                raf.readFully(tail);
                if (tail[0] == 'T' && tail[1] == 'A' && tail[2] == 'G') audioBytes -= 128;
            }
            return audioBytes * 8.0 / bitrate;
        }
        return 0;
    }

    private static double mp4Duration(RandomAccessFile raf) throws IOException {
        long pos = 0, end = raf.length();
        while (pos + 8 <= end) {
            raf.seek(pos);
            long size = u32(raf);
            String type = fourCC(raf);
            long hdr = 8;
            if (size == 1) {
                size = raf.readLong();
                hdr = 16;
            } else if (size == 0) {
                size = end - pos;
            }
            if (size < hdr) break;
            if (type.equals("moov")) return mvhdDuration(raf, pos + hdr, Math.min(end, pos + size));
            pos += size;
        }
        return 0;
    }

    private static double mvhdDuration(RandomAccessFile raf, long start, long end) throws IOException {
        long pos = start;
        while (pos + 8 <= end) {
            raf.seek(pos);
            long size = u32(raf);
            String type = fourCC(raf);
            if (size < 8) break;
            if (type.equals("mvhd")) {
                int version = raf.readUnsignedByte();
                raf.skipBytes(3);
                long timescale, duration;
                if (version == 1) {
                    raf.skipBytes(16);
                    timescale = u32(raf);
                    duration = raf.readLong();
                } else {
                    raf.skipBytes(8);
                    timescale = u32(raf);
                    duration = u32(raf);
                }
                return timescale > 0 ? (double) duration / timescale : 0;
            }
            pos += size;
        }
        return 0;
    }

    private static double wavDuration(RandomAccessFile raf) throws IOException {
        long len = raf.length();
        long pos = 12;
        long byteRate = 0;
        while (pos + 8 <= len) {
            raf.seek(pos);
            byte[] ck = new byte[8];
            raf.readFully(ck);
            String id = new String(ck, 0, 4, StandardCharsets.ISO_8859_1);
            long size = le32(ck, 4);
            if (id.equals("fmt ") && size >= 16) {
                byte[] fmt = new byte[16];
                raf.readFully(fmt);
                byteRate = le32(fmt, 8);
            } else if (id.equals("data")) {
                long dataLen = Math.min(size, len - (pos + 8));
                return byteRate > 0 ? (double) dataLen / byteRate : 0;
            }
            pos += 8 + size + (size & 1);
        }
        return 0;
    }

    private static long le32(byte[] b, int o) {
        return (b[o] & 0xffL) | ((b[o + 1] & 0xffL) << 8) | ((b[o + 2] & 0xffL) << 16) | ((b[o + 3] & 0xffL) << 24);
    }

    // ------------------------------------------------------------------
    //  small helpers
    // ------------------------------------------------------------------
    /** First string of an ID3 text frame (first byte = text encoding). */
    private static String text(byte[] d) {
        if (d.length < 2) return null;
        int enc = d[0] & 0xff;
        String s;
        switch (enc) {
            case 1:  s = new String(d, 1, d.length - 1, StandardCharsets.UTF_16); break;   // BOM decides byte order
            case 2:  s = new String(d, 1, d.length - 1, StandardCharsets.UTF_16BE); break;
            case 3:  s = new String(d, 1, d.length - 1, StandardCharsets.UTF_8); break;
            default: s = latin1OrUtf8(d, 1, d.length - 1); break;
        }
        return clean(s);
    }

    /** "ISO-8859-1" frames are often really UTF-8; use UTF-8 when the bytes are valid UTF-8. */
    private static String latin1OrUtf8(byte[] d, int off, int len) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(d, off, len)).toString();
        } catch (CharacterCodingException e) {
            return new String(d, off, len, StandardCharsets.ISO_8859_1);
        }
    }

    private static String utf8(byte[] b) {
        return b == null ? null : clean(new String(b, StandardCharsets.UTF_8));
    }

    /** Cuts at the first NUL, trims, returns null for empty text. */
    private static String clean(String s) {
        if (s == null) return null;
        int z = s.indexOf('\u0000');
        if (z >= 0) s = s.substring(0, z);
        s = s.replace("\uFEFF", "").trim();
        return s.isEmpty() ? null : s;
    }

    private static int indexOfZero(byte[] b, int from) {
        for (int i = from; i < b.length; i++) if (b[i] == 0) return i;
        return -1;
    }

    /** Undo ID3 unsynchronisation: every 0xFF 0x00 becomes 0xFF. */
    private static byte[] unsync(byte[] in) {
        byte[] out = new byte[in.length];
        int o = 0;
        for (int i = 0; i < in.length; i++) {
            out[o++] = in[i];
            if ((in[i] & 0xff) == 0xFF && i + 1 < in.length && in[i + 1] == 0) i++;
        }
        return Arrays.copyOf(out, o);
    }

    private static int syncsafe(byte[] b, int o) {
        return ((b[o] & 0x7f) << 21) | ((b[o + 1] & 0x7f) << 14) | ((b[o + 2] & 0x7f) << 7) | (b[o + 3] & 0x7f);
    }

    private static int be32(byte[] b, int o) {
        return ((b[o] & 0xff) << 24) | ((b[o + 1] & 0xff) << 16) | ((b[o + 2] & 0xff) << 8) | (b[o + 3] & 0xff);
    }

    private static int be24(byte[] b, int o) {
        return ((b[o] & 0xff) << 16) | ((b[o + 1] & 0xff) << 8) | (b[o + 2] & 0xff);
    }

    private static int be16(byte[] b, int o) {
        return ((b[o] & 0xff) << 8) | (b[o + 1] & 0xff);
    }

    private static long u32(RandomAccessFile raf) throws IOException {
        return raf.readInt() & 0xFFFFFFFFL;
    }

    private static String fourCC(RandomAccessFile raf) throws IOException {
        byte[] b = new byte[4];
        raf.readFully(b);
        return new String(b, StandardCharsets.ISO_8859_1);
    }
}
