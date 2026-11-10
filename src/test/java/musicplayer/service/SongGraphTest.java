package musicplayer.service;

import musicplayer.model.Song;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SongGraphTest {

    @Test
    @DisplayName("SongGraph: Graph building and BFS nearest-neighbor recommendations")
    public void testGraphBFSRecommendations() {
        SongGraph graph = new SongGraph();

        Song s1 = new Song("Lata Song 1", "Lata Mangeshkar", "/path/1");
        s1.setAlbum("Golden Melodies");
        s1.setMood("Calm");

        Song s2 = new Song("Lata Song 2", "Lata Mangeshkar", "/path/2");
        s2.setAlbum("Golden Melodies");
        s2.setMood("Calm");

        Song s3 = new Song("Kishore Song 1", "Kishore Kumar", "/path/3");
        s3.setAlbum("Duets");
        s3.setMood("Calm"); // connected via same mood

        Song s4 = new Song("Rock Song 1", "Rock Band", "/path/4");
        s4.setAlbum("Heavy Hits");
        s4.setMood("Energised"); // completely disconnected

        List<Song> library = List.of(s1, s2, s3, s4);
        graph.rebuild(library);

        // BFS from s1:
        // s2 shares Artist ("Lata Mangeshkar"), Album ("Golden Melodies"), and Mood ("Calm") -> Direct neighbor
        // s3 shares Mood ("Calm") -> Neighbor
        // s4 has nothing in common -> Disconnected
        List<Song> related = graph.related(s1, 10);

        assertEquals(2, related.size());
        assertTrue(related.contains(s2));
        assertTrue(related.contains(s3));
        assertFalse(related.contains(s4), "s4 has no matching artist, album, or mood and should not be recommended");
        assertFalse(related.contains(s1), "Seed song should not recommend itself");
    }
}
