package musicplayer.ds;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Hand-written generic doubly linked list. Nodes are exposed so a playlist can keep a "current" pointer. */
public class DoublyLinkedList<T> implements Iterable<T> {

    public static class Node<T> {
        private final T data;
        private Node<T> prev, next;
        private Node(T data) { this.data = data; }
        public T getData()      { return data; }
        public Node<T> getPrev() { return prev; }
        public Node<T> getNext() { return next; }
    }

    private Node<T> head, tail;
    private int size;

    /** O(1) */
    public Node<T> addLast(T value) {
        Node<T> n = new Node<>(value);
        if (tail == null) head = tail = n;
        else { tail.next = n; n.prev = tail; tail = n; }
        size++;
        return n;
    }

    /** O(1) */
    public Node<T> addFirst(T value) {
        Node<T> n = new Node<>(value);
        if (head == null) head = tail = n;
        else { n.next = head; head.prev = n; head = n; }
        size++;
        return n;
    }

    /** O(1) when you already hold the node. */
    public void remove(Node<T> n) {
        if (n.prev == null) head = n.next; else n.prev.next = n.next;
        if (n.next == null) tail = n.prev; else n.next.prev = n.prev;
        n.prev = n.next = null;
        size--;
    }

    /** O(n) */
    public Node<T> find(T value) {
        for (Node<T> c = head; c != null; c = c.next)
            if (c.data.equals(value)) return c;
        return null;
    }

    public Node<T> getHead() { return head; }
    public Node<T> getTail() { return tail; }
    public int size()        { return size; }
    public boolean isEmpty() { return size == 0; }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> cur = head;
            public boolean hasNext() { return cur != null; }
            public T next() {
                if (cur == null) throw new NoSuchElementException();
                T d = cur.data; cur = cur.next; return d;
            }
        };
    }
}
