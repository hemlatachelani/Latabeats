package musicplayer.algo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Generic top-down merge sort. Stable, O(n log n) time, O(n) extra space. */
public class MergeSort {

    public static <T> void sort(List<T> list, Comparator<? super T> cmp) {
        if (list.size() < 2) return;
        List<T> tmp = new ArrayList<>(list);
        sort(list, tmp, 0, list.size() - 1, cmp);
    }

    private static <T> void sort(List<T> a, List<T> tmp, int lo, int hi, Comparator<? super T> c) {
        if (lo >= hi) return;
        int mid = (lo + hi) >>> 1;
        sort(a, tmp, lo, mid, c);
        sort(a, tmp, mid + 1, hi, c);
        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) {
            if (c.compare(a.get(i), a.get(j)) <= 0) tmp.set(k++, a.get(i++));
            else tmp.set(k++, a.get(j++));
        }
        while (i <= mid) tmp.set(k++, a.get(i++));
        while (j <= hi)  tmp.set(k++, a.get(j++));
        for (k = lo; k <= hi; k++) a.set(k, tmp.get(k));
    }
}
