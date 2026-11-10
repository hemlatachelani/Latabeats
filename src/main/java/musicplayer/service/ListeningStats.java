package musicplayer.service;

import musicplayer.algo.MergeSort;
import musicplayer.model.Song;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Responsibility: how much of each mood the user has listened to, and the ranked breakdown of it. */
public class ListeningStats {

    /** One mood and its share of the total (0..1). */
    public static final class Share {
        public final String mood;
        public final double fraction;
        Share(String mood, double fraction) {
            this.mood = mood;
            this.fraction = fraction;
        }
    }

    /** byTime = true when based on real listening time, false when based on the song count. */
    public static final class Breakdown {
        public final boolean byTime;
        public final List<Share> shares;
        Breakdown(boolean byTime, List<Share> shares) {
            this.byTime = byTime;
            this.shares = shares;
        }
    }

    private final Map<String, Double> moodSeconds = new HashMap<>();

    /** Call while a song plays. Jumps (seeking) of 1.5 s or more are ignored. */
    public void record(String mood, double previousSeconds, double nowSeconds) {
        double delta = nowSeconds - previousSeconds;
        if (delta > 0 && delta < 1.5) moodSeconds.merge(mood, delta, Double::sum);
    }

    public Map<String, Double> snapshot() {
        return new HashMap<>(moodSeconds);
    }

    public void restore(Map<String, Double> saved) {
        moodSeconds.clear();
        if (saved != null) moodSeconds.putAll(saved);
    }

    public Breakdown breakdown(Collection<Song> library) {
        double listened = 0;
        for (double v : moodSeconds.values()) listened += v;
        boolean byTime = listened > 1;

        Map<String, Double> data = new HashMap<>();
        if (byTime) data.putAll(moodSeconds);
        else for (Song s : library) data.merge(s.getMood(), 1.0, Double::sum);

        double total = 0;
        for (double v : data.values()) total += v;
        List<Share> shares = new ArrayList<>();
        if (total <= 0) return new Breakdown(byTime, shares);

        List<String> keys = new ArrayList<>(data.keySet());
        MergeSort.sort(keys, (a, b) -> Double.compare(data.get(b), data.get(a)));
        for (String k : keys) shares.add(new Share(k, data.get(k) / total));
        return new Breakdown(byTime, shares);
    }
}
