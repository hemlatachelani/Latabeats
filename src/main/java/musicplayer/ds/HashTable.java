package musicplayer.ds;

import java.util.ArrayList;
import java.util.List;

/**
 * Separate-chaining hash table. put/get/remove are expected O(1).
 * Used as the song-by-path index and to hold named playlists.
 */
public class HashTable<K, V> {

    private static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;
        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private static final double LOAD = 0.75;

    private Node<K, V>[] buckets;
    private int size;

    public HashTable() {
        this(16);
    }

    @SuppressWarnings("unchecked")
    public HashTable(int capacity) {
        int cap = 8;
        while (cap < capacity) cap *= 2;
        buckets = (Node<K, V>[]) new Node[cap];
    }

    public V put(K key, V value) {
        requireKey(key);
        if (value == null) throw new IllegalArgumentException("value");
        if (size + 1 > buckets.length * LOAD) resize(buckets.length * 2);
        int i = index(key);
        for (Node<K, V> n = buckets[i]; n != null; n = n.next) {
            if (n.key.equals(key)) {
                V old = n.value;
                n.value = value;
                return old;
            }
        }
        buckets[i] = new Node<>(key, value, buckets[i]);
        size++;
        return null;
    }

    public V get(K key) {
        if (key == null) return null;
        for (Node<K, V> n = buckets[index(key)]; n != null; n = n.next) {
            if (n.key.equals(key)) return n.value;
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public V remove(K key) {
        if (key == null) return null;
        int i = index(key);
        Node<K, V> prev = null;
        for (Node<K, V> n = buckets[i]; n != null; prev = n, n = n.next) {
            if (!n.key.equals(key)) continue;
            if (prev == null) buckets[i] = n.next;
            else prev.next = n.next;
            size--;
            return n.value;
        }
        return null;
    }

    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    @SuppressWarnings("unchecked")
    public void clear() {
        buckets = (Node<K, V>[]) new Node[16];
        size = 0;
    }

    public List<K> keys() {
        List<K> out = new ArrayList<>();
        for (Node<K, V> head : buckets) {
            for (Node<K, V> n = head; n != null; n = n.next) out.add(n.key);
        }
        return out;
    }

    public List<V> values() {
        List<V> out = new ArrayList<>();
        for (Node<K, V> head : buckets) {
            for (Node<K, V> n = head; n != null; n = n.next) out.add(n.value);
        }
        return out;
    }

    public void forEach(Visitor<K, V> visitor) {
        for (Node<K, V> head : buckets) {
            for (Node<K, V> n = head; n != null; n = n.next) visitor.visit(n.key, n.value);
        }
    }

    public interface Visitor<K, V> {
        void visit(K key, V value);
    }

    private int index(K key) {
        return (key.hashCode() & 0x7fffffff) % buckets.length;
    }

    @SuppressWarnings("unchecked")
    private void resize(int cap) {
        Node<K, V>[] old = buckets;
        buckets = (Node<K, V>[]) new Node[cap];
        size = 0;
        for (Node<K, V> head : old) {
            for (Node<K, V> n = head; n != null; n = n.next) put(n.key, n.value);
        }
    }

    private static void requireKey(Object key) {
        if (key == null) throw new IllegalArgumentException("key");
    }
}
