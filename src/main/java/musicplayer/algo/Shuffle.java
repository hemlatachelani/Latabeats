package musicplayer.algo;

import java.util.List;
import java.util.Random;

/** Fisher-Yates shuffle: unbiased, O(n). */
public class Shuffle {

    public static <T> void fisherYates(List<T> list, Random rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
