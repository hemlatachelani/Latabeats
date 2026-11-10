package musicplayer.ds;

import musicplayer.model.Song;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataStructuresTest {

    @Test
    @DisplayName("DoublyLinkedList: Insertion, removal, and sequential order")
    public void testDoublyLinkedList() {
        DoublyLinkedList<String> list = new DoublyLinkedList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());

        DoublyLinkedList.Node<String> n1 = list.addLast("Song A");
        DoublyLinkedList.Node<String> n2 = list.addLast("Song B");
        DoublyLinkedList.Node<String> n3 = list.addLast("Song C");

        assertEquals(3, list.size());
        assertFalse(list.isEmpty());
        assertNotNull(list.find("Song B"));

        List<String> collected = new ArrayList<>();
        for (String s : list) collected.add(s);
        assertEquals(List.of("Song A", "Song B", "Song C"), collected);

        list.remove(n2);
        assertEquals(2, list.size());
        assertNull(list.find("Song B"));

        collected.clear();
        for (String s : list) collected.add(s);
        assertEquals(List.of("Song A", "Song C"), collected);
    }

    @Test
    @DisplayName("HashTable: Separate chaining, updates, collision handling, and dynamic resizing")
    public void testHashTable() {
        HashTable<String, Integer> table = new HashTable<>(4);

        for (int i = 0; i < 20; i++) {
            table.put("key_" + i, i * 10);
        }

        assertEquals(20, table.size());
        for (int i = 0; i < 20; i++) {
            assertEquals(i * 10, table.get("key_" + i));
            assertTrue(table.containsKey("key_" + i));
        }

        Integer oldVal = table.put("key_5", 999);
        assertEquals(50, oldVal);
        assertEquals(999, table.get("key_5"));

        Integer removed = table.remove("key_5");
        assertEquals(999, removed);
        assertNull(table.get("key_5"));
        assertFalse(table.containsKey("key_5"));
        assertEquals(19, table.size());
    }

    @Test
    @DisplayName("LRUCache: O(1) access and strict capacity eviction (max 8)")
    public void testLRUCache() {
        LRUCache<String, String> cache = new LRUCache<>(8);

        for (int i = 1; i <= 8; i++) {
            cache.put("id_" + i, "Track " + i);
        }
        assertEquals(8, cache.size());

        assertEquals("Track 1", cache.get("id_1"));

        cache.put("id_9", "Track 9");
        assertEquals(8, cache.size());
        assertNull(cache.get("id_2"), "id_2 should have been evicted as LRU");
        assertNotNull(cache.get("id_1"), "id_1 was accessed so it must remain in cache");
        assertNotNull(cache.get("id_9"), "id_9 is newly added and must be in cache");
    }

    @Test
    @DisplayName("MinHeap: Invariant preservation, peek, and poll in ascending order")
    public void testMinHeap() {
        MinHeap<Integer> heap = new MinHeap<>(Integer::compareTo);
        assertTrue(heap.isEmpty());

        int[] values = {45, 12, 85, 32, 8, 99, 1};
        for (int v : values) heap.add(v);

        assertEquals(7, heap.size());
        assertEquals(1, heap.peek());

        int prev = heap.poll();
        while (!heap.isEmpty()) {
            int current = heap.poll();
            assertTrue(current >= prev, "Heap poll order violated: " + current + " < " + prev);
            prev = current;
        }
        assertEquals(0, heap.size());
    }

    @Test
    @DisplayName("Trie: Prefix insertion, case insensitivity, and autocomplete retrieval")
    public void testTrie() {
        Trie<Song> trie = new Trie<>();
        Song s1 = new Song("Shape of You", "Ed Sheeran", "/path/1");
        Song s2 = new Song("Perfect", "Ed Sheeran", "/path/2");
        Song s3 = new Song("Castle on the Hill", "Ed Sheeran", "/path/3");
        Song s4 = new Song("Yellow", "Coldplay", "/path/4");

        indexSong(trie, s1);
        indexSong(trie, s2);
        indexSong(trie, s3);
        indexSong(trie, s4);

        List<Song> res1 = trie.startsWith("sha", 10);
        assertEquals(1, res1.size());
        assertTrue(res1.contains(s1));

        List<Song> res2 = trie.startsWith("ed", 10);
        assertEquals(3, res2.size());
        assertTrue(res2.contains(s1));
        assertTrue(res2.contains(s2));
        assertTrue(res2.contains(s3));

        List<Song> res3 = trie.startsWith("cold", 10);
        assertEquals(1, res3.size());
        assertTrue(res3.contains(s4));

        List<Song> res4 = trie.startsWith("xyz", 10);
        assertTrue(res4.isEmpty());
    }

    private void indexSong(Trie<Song> trie, Song s) {
        String full = (s.getTitle() + " " + s.getArtist()).toLowerCase();
        for (String word : full.split("\\s+")) {
            if (!word.isBlank()) trie.insert(word, s);
        }
    }

    @Test
    @DisplayName("MyStack (LIFO) & MyQueue (FIFO)")
    public void testStackAndQueue() {
        MyStack<String> stack = new MyStack<>();
        stack.push("First");
        stack.push("Second");
        assertEquals("Second", stack.peek());
        assertEquals("Second", stack.pop());
        assertEquals("First", stack.pop());
        assertTrue(stack.isEmpty());

        MyQueue<String> queue = new MyQueue<>();
        queue.enqueue("First");
        queue.enqueue("Second");
        assertEquals("First", queue.peek());
        assertEquals("First", queue.dequeue());
        assertEquals("Second", queue.dequeue());
        assertTrue(queue.isEmpty());
    }
}
