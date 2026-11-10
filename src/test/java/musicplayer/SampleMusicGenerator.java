package musicplayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Creates small demo WAV files in sample_music/ for instant project testing.
 */
public class SampleMusicGenerator {

    public static void main(String[] args) throws IOException {
        File dir = new File("sample_music");
        if (!dir.exists()) dir.mkdirs();

        makeTrack(new File(dir, "Lata Mangeshkar - Lag Ja Gale (Demo).wav"),
                new double[]{261.63, 293.66, 329.63, 392.00});
        makeTrack(new File(dir, "A.R. Rahman - Jai Ho (Demo).wav"),
                new double[]{329.63, 392.00, 440.00, 523.25});
        makeTrack(new File(dir, "Kishore Kumar - Pal Pal Dil Ke Paas (Demo).wav"),
                new double[]{220.00, 261.63, 329.63, 440.00});
        makeTrack(new File(dir, "Ludwig van Beethoven - Moonlight Sonata (Demo).wav"),
                new double[]{277.18, 329.63, 415.30, 554.37});

        System.out.println("Generated 4 sample tracks in sample_music/ successfully!");
    }

    private static void makeTrack(File out, double[] chords) throws IOException {
        int sampleRate = 44100;
        double duration = 5.0; // 5 seconds
        int totalSamples = (int) (sampleRate * duration);
        int dataSize = totalSamples * 2; // 16-bit mono

        ByteBuffer buf = ByteBuffer.allocate(44 + dataSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // RIFF header
        buf.put("RIFF".getBytes());
        buf.putInt(36 + dataSize);
        buf.put("WAVE".getBytes());
        buf.put("fmt ".getBytes());
        buf.putInt(16);             // Subchunk1Size
        buf.putShort((short) 1);     // AudioFormat = PCM
        buf.putShort((short) 1);     // NumChannels = 1
        buf.putInt(sampleRate);
        buf.putInt(sampleRate * 2);  // ByteRate
        buf.putShort((short) 2);     // BlockAlign
        buf.putShort((short) 16);    // BitsPerSample
        buf.put("data".getBytes());
        buf.putInt(dataSize);

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            int idx = Math.min(chords.length - 1, (int) (t / 1.25));
            double f = chords[idx];
            double env = Math.sin(Math.PI * i / totalSamples); // smooth fade-in and fade-out
            double sampleVal = (Math.sin(2 * Math.PI * f * t) * 0.6
                    + Math.sin(2 * Math.PI * f * 1.5 * t) * 0.25
                    + Math.sin(2 * Math.PI * f * 2.0 * t) * 0.15) * env;
            short s = (short) Math.max(-32767, Math.min(32767, (int) (sampleVal * 24000)));
            buf.putShort(s);
        }

        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(buf.array());
        }
    }
}
