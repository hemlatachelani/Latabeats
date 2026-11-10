package musicplayer.ds;

import java.util.ArrayList;
import java.util.List;

/** Prefix tree for autocomplete. Stores values (e.g. Songs) at the node where their key ends. */
public class Trie<V> {

    private static class Node<V> {
        HashTable<Character, Node<V>> children = new HashTable<>();
        List<V> values = new ArrayList<>();
    }

    private final Node<V> root = new Node<>();

    /** O(L) where L = key length */
    public void insert(String key, V value) {
        Node<V> n = root;
        for (char c : key.toLowerCase().toCharArray()) {
            Node<V> child = n.children.get(c);
            if (child == null) { child = new Node<>(); n.children.put(c, child); }
            n = child;
        }
        n.values.add(value);
    }

    /** Returns up to `limit` values whose key starts with `prefix`. O(L + results) */
    public List<V> startsWith(String prefix, int limit) {
        List<V> out = new ArrayList<>();
        Node<V> n = root;
        for (char c : prefix.toLowerCase().toCharArray()) {
            n = n.children.get(c);
            if (n == null) return out;
        }
        collect(n, out, limit);
        return out;
    }

    private void collect(Node<V> n, List<V> out, int limit) {
        for (V v : n.values) {
            if (out.size() >= limit) return;
            if (!out.contains(v)) out.add(v);
        }
        for (Node<V> child : n.children.values()) {
            if (out.size() >= limit) return;
            collect(child, out, limit);
        }
    }

    public void clear() { root.children.clear(); root.values.clear(); }
}
