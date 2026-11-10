package musicplayer.service;

import musicplayer.ds.DoublyLinkedList;
import musicplayer.ds.DoublyLinkedList.Node;
import musicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;

/** Playlist = doubly linked list + a pointer to the current node. */
public class Playlist {
    private DoublyLinkedList<Song> songs = new DoublyLinkedList<>();
    private Node<Song> current;
    private boolean repeatAll;

    public void add(Song s) {
        Node<Song> n = songs.addLast(s);
        if (current == null) current = n;
    }

    public void remove(Song s) {
        Node<Song> n = songs.find(s);
        if (n == null) return;
        if (n == current) current = n.getNext() != null ? n.getNext() : n.getPrev();
        songs.remove(n);
    }

    /** Rebuilds the linked list in the given order (used after sorting). Keeps the current song. */
    public void replaceAll(List<Song> ordered) {
        Song cur = current();
        songs = new DoublyLinkedList<>();
        current = null;
        for (Song s : ordered) {
            Node<Song> n = songs.addLast(s);
            if (s == cur) current = n;
        }
        if (current == null) current = songs.getHead();
    }

    public void select(Song s) {
        Node<Song> n = songs.find(s);
        if (n != null) current = n;
    }

    public Song current() { return current == null ? null : current.getData(); }

    /** Moves to next song. Wraps to the head if repeatAll, otherwise returns null at the end. */
    public Song next() {
        if (current == null) return null;
        if (current.getNext() != null) current = current.getNext();
        else if (repeatAll) current = songs.getHead();
        else return null;
        return current.getData();
    }

    public Song previous() {
        if (current == null) return null;
        if (current.getPrev() != null) current = current.getPrev();
        else if (repeatAll) current = songs.getTail();
        return current.getData();
    }

    public boolean contains(Song s) { return songs.find(s) != null; }

    public void setRepeatAll(boolean r) { repeatAll = r; }
    public boolean isRepeatAll()        { return repeatAll; }
    public int size()                   { return songs.size(); }

    public List<Song> toList() {
        List<Song> l = new ArrayList<>();
        for (Song s : songs) l.add(s);
        return l;
    }
}
