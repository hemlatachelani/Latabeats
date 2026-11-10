package musicplayer.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Least-Recently-Used cache = HashMap (O(1) lookup) + DoublyLinkedList (O(1) reorder/evict).
 * Front of the list = most recently used. Used for the "Recently played" shelf.
 */
public class LRUCache<K, V> {

    private static class Entry<K, V> {
        final K key;
        V value;
        Entry(K key, V value) { this.key = key; this.value = value; }
    }

    private final int capacity;
    private final Map<K, DoublyLinkedList.Node<Entry<K, V>>> map = new HashMap<>();
    private final DoublyLinkedList<Entry<K, V>> list = new DoublyLinkedList<>();

    public LRUCache(int capacity) { this.capacity = capacity; }

    /** O(1) - also marks the entry as most recently used. */
    public V get(K key) {
        DoublyLinkedList.Node<Entry<K, V>> node = map.get(key);
        if (node == null) return null;
        Entry<K, V> e = node.getData();
        list.remove(node);
        map.put(key, list.addFirst(e));
        return e.value;
    }

    /** O(1) - inserts or refreshes; evicts the least recently used entry when full. */
    public void put(K key, V value) {
        DoublyLinkedList.Node<Entry<K, V>> node = map.get(key);
        Entry<K, V> e;
        if (node != null) {
            e = node.getData();
            list.remove(node);
        } else {
            if (map.size() >= capacity) {
                DoublyLinkedList.Node<Entry<K, V>> tail = list.getTail();
                map.remove(tail.getData().key);
                list.remove(tail);
            }
            e = new Entry<>(key, value);
        }
        e.value = value;
        map.put(key, list.addFirst(e));
    }

    public void remove(K key) {
        DoublyLinkedList.Node<Entry<K, V>> node = map.remove(key);
        if (node != null) list.remove(node);
    }

    public int size() { return map.size(); }

    /** Most recently used first. */
    public List<V> toList() {
        List<V> out = new ArrayList<>();
        for (Entry<K, V> e : list) out.add(e.value);
        return out;
    }
}
