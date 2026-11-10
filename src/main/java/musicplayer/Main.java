package musicplayer;

import javafx.application.Application;
import musicplayer.ui.MainUI;

/**
 * The main method - and nothing else.
 * All the UI code is in {@link MainUI}; all the rules are in the service package.
 *
 * (This class does not extend Application on purpose: starting from a plain class avoids the
 * "JavaFX runtime components are missing" error when running from an IDE or a jar.)
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        Application.launch(MainUI.class, args);
    }
}
