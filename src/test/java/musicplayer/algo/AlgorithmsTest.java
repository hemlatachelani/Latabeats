package musicplayer.algo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class AlgorithmsTest {

    static class Track {
        final String title;
        final int plays;
        final int originalOrder;

        Track(String title, int plays, int originalOrder) {
            this.title = title;
            this.plays = plays;
            this.originalOrder = originalOrder;
        }
    }

    @Test
    @DisplayName("MergeSort: Correct sorting & strict STABILITY on ties")
    public void testMergeSortStability() {
        List<Track> list = new ArrayList<>();
        list.add(new Track("Track A", 10, 1));
        list.add(new Track("Track B", 50, 2));
        list.add(new Track("Track C", 10, 3));
        list.add(new Track("Track D", 30, 4));
        list.add(new Track("Track E", 10, 5));

        MergeSort.sort(list, Comparator.comparingInt(t -> t.plays));

        assertEquals(10, list.get(0).plays);
        assertEquals(10, list.get(1).plays);
        assertEquals(10, list.get(2).plays);
        assertEquals(30, list.get(3).plays);
        assertEquals(50, list.get(4).plays);

        assertEquals("Track A", list.get(0).title);
        assertEquals("Track C", list.get(1).title);
        assertEquals("Track E", list.get(2).title);
    }

    @Test
    @DisplayName("TopK: Extracts exact K highest elements in descending order")
    public void testTopK() {
        List<Integer> numbers = List.of(12, 95, 3, 44, 76, 21, 88, 5, 60, 100, 15);
        List<Integer> top4 = TopK.topK(numbers, 4, Integer::compareTo);

        assertEquals(4, top4.size());
        assertEquals(List.of(100, 95, 88, 76), top4);
    }

    @Test
    @DisplayName("Shuffle: Fisher-Yates preserves all elements with randomized permutation")
    public void testShuffle() {
        List<Integer> original = new ArrayList<>();
        for (int i = 0; i < 50; i++) original.add(i);

        List<Integer> shuffled = new ArrayList<>(original);
        Shuffle.fisherYates(shuffled, new Random());

        assertEquals(original.size(), shuffled.size());
        assertTrue(shuffled.containsAll(original));
        assertNotEquals(original, shuffled, "Shuffled list should differ from original list");
    }
}
