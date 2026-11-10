package musicplayer.algo;

import musicplayer.ds.MinHeap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Top-K selection with a size-K min-heap: O(n log k) time, O(k) space. */
public class TopK {

    /** cmp orders items so that "bigger" = better. Result is best-first. */
    public static <T> List<T> topK(Iterable<T> items, int k, Comparator<? super T> cmp) {
        MinHeap<T> heap = new MinHeap<>(cmp);
        for (T x : items) {
            if (heap.size() < k) {
                heap.add(x);
            } else if (cmp.compare(x, heap.peek()) > 0) {
                heap.poll();
                heap.add(x);
            }
        }
        List<T> out = new ArrayList<>();
        while (!heap.isEmpty()) out.add(heap.poll());
        Collections.reverse(out);
        return out;
    }
}
