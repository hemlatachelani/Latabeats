package musicplayer.ds;

/** Linked-list based stack. Used for play history ("Back" button) and undo. */
public class MyStack<T> {
    private static class Node<T> {
        T data; Node<T> below;
        Node(T d, Node<T> b) { data = d; below = b; }
    }

    private Node<T> top;
    private int size;

    public void push(T v) { top = new Node<>(v, top); size++; }

    public T pop() {
        if (top == null) return null;
        T v = top.data; top = top.below; size--; return v;
    }

    public T peek()          { return top == null ? null : top.data; }
    public boolean isEmpty() { return top == null; }
    public int size()        { return size; }
    public void clear()      { top = null; size = 0; }
}
