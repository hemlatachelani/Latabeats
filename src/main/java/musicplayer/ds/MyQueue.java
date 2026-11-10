package musicplayer.ds;

import java.util.ArrayList;
import java.util.List;

/** FIFO queue built on our own DoublyLinkedList. enqueueFront() lets us do "Play next". */
public class MyQueue<T> {
    private final DoublyLinkedList<T> list = new DoublyLinkedList<>();

    /** O(1) */
    public void enqueue(T v) { list.addLast(v); }

    /** O(1) - puts the item at the very front ("Play next"). */
    public void enqueueFront(T v) { list.addFirst(v); }

    /** O(1) - returns null when empty. */
    public T dequeue() {
        DoublyLinkedList.Node<T> h = list.getHead();
        if (h == null) return null;
        T v = h.getData();
        list.remove(h);
        return v;
    }

    public T peek() { return list.getHead() == null ? null : list.getHead().getData(); }

    /** O(n) - removes the first occurrence. */
    public boolean remove(T v) {
        DoublyLinkedList.Node<T> n = list.find(v);
        if (n == null) return false;
        list.remove(n);
        return true;
    }

    public boolean contains(T v) { return list.find(v) != null; }
    public int size()            { return list.size(); }
    public boolean isEmpty()     { return list.isEmpty(); }
    public void clear()          { while (!isEmpty()) dequeue(); }

    public List<T> toList() {
        List<T> l = new ArrayList<>();
        for (T t : list) l.add(t);
        return l;
    }
}
