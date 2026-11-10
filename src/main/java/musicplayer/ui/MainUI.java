package musicplayer.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import musicplayer.model.Song;
import musicplayer.db.Database;
import musicplayer.db.DatabaseConfig;
import musicplayer.db.DatabaseException;
import musicplayer.db.FavouriteRepository;
import musicplayer.db.UserRepository;
import musicplayer.model.User;
import musicplayer.service.AuthException;
import musicplayer.service.AuthService;
import musicplayer.service.FavouritesService;
import musicplayer.service.FileScanner;
import musicplayer.service.LibraryStore;
import musicplayer.service.LocalSettings;
import musicplayer.service.ListeningStats;
import musicplayer.service.MetadataService;
import musicplayer.service.MusicLibrary;
import musicplayer.service.PlaybackService;
import musicplayer.service.PlaylistService;
import musicplayer.service.ProfileStore;
import musicplayer.service.RadioService;
import musicplayer.service.SongGraph;
import musicplayer.service.SortOrder;
import musicplayer.service.Station;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MainUI extends Application {

    // ---------- branding / constants ----------
    private static final String APP_NAME = "LataBeats";
    private static final String PLAYLIST_TITLE = "My Library";
    private static final String[] MOODS = MusicLibrary.MOODS;
    private static final Map<String, Color> MOOD_COLORS = new LinkedHashMap<>();
    static {
        MOOD_COLORS.put("Late night", Color.web("#7dd3fc"));
        MOOD_COLORS.put("Energised", Color.web("#fdba74"));
        MOOD_COLORS.put("Calm", Color.web("#86efac"));
        MOOD_COLORS.put("Dreamy", Color.web("#f9a8d4"));
        MOOD_COLORS.put("Melancholy", Color.web("#a5b4fc"));
        MOOD_COLORS.put("Soulful", Color.web("#fcd34d"));
    }

    // SVG icon paths (24x24 viewBox)
    private static final String PLAY = "M8 5v14l11-7z";
    private static final String PAUSE = "M6 19h4V5H6v14zm8-14v14h4V5h-4z";
    private static final String PREV = "M6 6h2v12H6zm3.5 6l8.5 6V6z";
    private static final String NEXT = "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z";
    private static final String SHUFFLE = "M10.59 9.17L5.41 4 4 5.41l5.17 5.17 1.42-1.41zM14.5 4l2.04 2.04L4 18.59 5.41 20 17.96 7.46 20 9.5V4h-5.5zm.33 9.41l-1.41 1.41 3.13 3.13L14.5 20H20v-5.5l-2.04 2.04-3.13-3.13z";
    private static final String REPEAT = "M7 7h10v3l4-4-4-4v3H5v6h2V7zm10 10H7v-3l-4 4 4 4v-3h12v-6h-2v4z";
    private static final String UNDO = "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z";
    private static final String HEART = "M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z";
    private static final String HEART_FILLED = "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z";
    private static final String MORE = "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z";
    private static final String CLOCK = "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z";
    private static final String NOTE = "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z";
    private static final String VOLUME = "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z";

    private static final Color DARK = Color.web("#14122b");
    private static final PseudoClass PLAYING = PseudoClass.getPseudoClass("playing");
    private static final PseudoClass CHOSEN = PseudoClass.getPseudoClass("chosen");

    private static final double COL_NUM = 44, COL_MOOD = 140, COL_HEART = 48, COL_DUR = 64;
    private static final double COVER = 140;

    private enum Page { LISTEN, MOODS, LIBRARY, RADIO }
    private enum Mode { ALL, FAV, QUEUE, PLAYLIST }

    // ---------- everything the window needs; ALL rules live in these services ----------
    private Stage stage;
    private final MusicLibrary library = new MusicLibrary();
    private final PlaybackService playback = new PlaybackService(library);
    private final SongGraph graph = new SongGraph();
    private final PlaylistService playlists = new PlaylistService();
    private final RadioService radio = new RadioService(library, playback, graph);
    private final ListeningStats stats = new ListeningStats();
    private final MetadataService metadata = new MetadataService();

    // ---------- the logged-in session (set after a successful login) ----------
    private Database database;
    private AuthService auth;
    private LocalSettings settings;
    private volatile User user;
    private FavouritesService favourites;
    private ProfileStore profileStore;
    private LibraryStore libraryStore;
    private List<File> startupFiles = List.of();

    private final ObservableList<Song> displayed = FXCollections.observableArrayList();  // what the main list shows
    private final ListView<Song> listView = new ListView<>(displayed);

    // ---------- UI state ----------
    private Page page = Page.LIBRARY;
    private Mode mode = Mode.ALL;
    private String selectedMood = MOODS[0];

    private BorderPane root;
    private final Map<Page, ToggleButton> navButtons = new EnumMap<>(Page.class);
    private final StackPane pages = new StackPane();
    private VBox listPage, homePage, radioPage;

    // search
    private final TextField search = new TextField();
    private final Label clearSearch = new Label("✕");

    // list page
    private final HBox headerChips = new HBox(10);
    private final Label titleLabel = new Label(PLAYLIST_TITLE);
    private final Label playlistSub = new Label();
    private final Button headerPlay = new Button("Play");
    private final ToggleButton shuffleBtn = new ToggleButton();
    private final Button likeBtn = new Button();
    private final StackPane coverFallback = new StackPane();
    private final ImageView coverView = new ImageView();
    private final HBox modeBar = new HBox(8);
    private final ToggleButton allBtn = new ToggleButton("All songs");
    private final ToggleButton favBtn = new ToggleButton("Favourites");
    private final ToggleButton queueBtn = new ToggleButton("Queue");
    private final ToggleButton playlistsBtn = new ToggleButton("Playlists");
    private final ComboBox<String> playlistBox = new ComboBox<>();
    private String selectedPlaylist;
    private boolean updatingPlaylistBox = false;
    private Song relatedSeed;
    private final ComboBox<String> sortBox = new ComboBox<>();
    private final FlowPane moodChips = new FlowPane(8, 8);
    private final Label placeholderText = new Label();
    private final Button placeholderAdd = new Button("Add songs");

    // home page (Listen now)
    private final ObservableList<Song> recentItems = FXCollections.observableArrayList();
    private final ObservableList<Song> topItems = FXCollections.observableArrayList();
    private final ListView<Song> recentList = new ListView<>(recentItems);
    private final ListView<Song> topList = new ListView<>(topItems);

    // radio page
    private final FlowPane stationPane = new FlowPane(14, 14);
    private final Label stationStatus = new Label();
    private final Button stopRadioBtn = new Button("Stop radio");

    // side column
    private final GridPane moodBar = new GridPane();
    private final VBox moodLegend = new VBox(10);
    private final Label moodSubtitle = new Label("By song count");

    // player bar
    private final Label nowTitle = new Label("Nothing playing");
    private final Label nowArtist = new Label("");
    private final Label elapsedLabel = new Label("0:00");
    private final Label totalLabel = new Label("0:00");
    private final Slider seek = new Slider(0, 1, 0);
    private final Slider volume = new Slider(0, 1, 0.7);
    private final Button playBtn = new Button();

    // playback
    private MediaPlayer player;
    private boolean seeking = false;
    private boolean moodSync = true;
    private double lastTime = 0;
    private int lastShownSecond = -1;

    // metadata + pictures
    private final Map<Song, Image> covers = new HashMap<>();            // album art read from inside the files
    private final ImageView nowThumbImg = new ImageView();
    private final Node nowNote = icon(NOTE, 22, Color.web("#ffffff99"));
    private final StackPane nowThumb = new StackPane(nowNote, nowThumbImg);

    // visualizer & video
    private final SpectrumView spectrum = new SpectrumView();
    private final MediaView mediaView = new MediaView();
    private final Label visStyle = new Label();
    private final Label visNow = new Label("Play a song to see it come alive");
    private boolean hasVideoTrack = false;
    private boolean showVideo = true;
    private Stage videoPopoutStage;

    // profile (picture + name chosen by the user)
    private final StackPane avatar = new StackPane();
    private final Label avatarText = new Label("ME");
    private final ImageView avatarImg = new ImageView();
    private Image profileImage;

    // =====================================================================
    //  Start
    // =====================================================================
    /** JavaFX calls this after Main.main() has launched the application. The user first sees the login screen. */
    @Override
    public void start(Stage primaryStage) {
        begin(primaryStage, FileScanner.collect(getParameters().getRaw()));      // optional command-line files/folders
    }

    /** Shows the login / registration window. A new MainUI object always means a completely clean session. */
    private void begin(Stage loginStage, List<File> startup) {
        startupFiles = startup;
        settings = new LocalSettings();
        database = new Database(DatabaseConfig.load());
        auth = new AuthService(new UserRepository(database));
        new AuthView(loginStage, database, auth, settings, this::openPlayer).show();
    }

    /** Called after a successful login or registration: builds the music player window for that user. */
    private void openPlayer(User loggedIn) {
        user = loggedIn;
        profileStore = ProfileStore.forUser(loggedIn.username());
        profileStore.load();
        libraryStore = LibraryStore.forUser(loggedIn.username());
        loadProfileImage();
        favourites = new FavouritesService(new FavouriteRepository(database), loggedIn);
        favourites.setErrorHandler(message -> Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, message).show()));

        stage = new Stage();
        root = new BorderPane();
        root.getStyleClass().add("root-bg");
        root.setPadding(new Insets(20, 28, 20, 28));
        root.setTop(buildTopBar());

        HBox main = new HBox(20, buildLibraryCard(), buildSideColumn());
        BorderPane.setMargin(main, new Insets(18, 0, 16, 0));
        root.setCenter(main);
        root.setBottom(buildPlayerBar());

        Scene scene = new Scene(root, 1200, 830);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle(APP_NAME + " - " + loggedIn.displayName());
        stage.setMinWidth(1020);
        stage.setMinHeight(740);
        stage.setScene(scene);
        setupGlobalShortcuts(scene);
        stage.setOnHidden(e -> {
            if (videoPopoutStage != null) videoPopoutStage.close();
            persistLibrary();
            favourites.close();
        });
        stage.show();

        applyPageVisibility();
        refreshAll();
        startSession();
    }

    private void setupGlobalShortcuts(Scene scene) {
        scene.setOnKeyPressed(e -> {
            Node focus = scene.getFocusOwner();
            boolean inTextField = (focus instanceof TextInputControl);

            if (e.isControlDown() && e.getCode() == KeyCode.F) {
                search.requestFocus();
                search.selectAll();
                e.consume();
                return;
            }

            if (!inTextField) {
                if (e.getCode() == KeyCode.SPACE) {
                    togglePlay();
                    e.consume();
                } else if (e.getCode() == KeyCode.RIGHT) {
                    if (player != null) {
                        double t = Math.min(seek.getMax(), seek.getValue() + 5);
                        player.seek(Duration.seconds(t));
                    }
                    e.consume();
                } else if (e.getCode() == KeyCode.LEFT) {
                    if (player != null) {
                        double t = Math.max(0, seek.getValue() - 5);
                        player.seek(Duration.seconds(t));
                    }
                    e.consume();
                } else if (e.getCode() == KeyCode.J) {
                    goBack();
                    e.consume();
                } else if (e.getCode() == KeyCode.L) {
                    Song n = playback.next();
                    if (n != null) play(n, true);
                    e.consume();
                } else if (e.getCode() == KeyCode.M) {
                    volume.setValue(volume.getValue() > 0 ? 0 : 0.7);
                    if (player != null) player.setVolume(volume.getValue());
                    e.consume();
                } else if (e.getCode() == KeyCode.H) {
                    Song s = currentOrSelected();
                    if (s != null) toggleFavourite(s);
                    e.consume();
                }
            }
        });
    }

    // =====================================================================
    //  Top bar (logo, tabs, mood sync, search, avatar)
    // =====================================================================
    private Node buildTopBar() {
        HBox bars = new HBox(2);
        bars.setAlignment(Pos.BOTTOM_CENTER);
        double[] hs = {10, 20, 14, 24, 12};
        for (double h : hs) {
            Rectangle r = new Rectangle(3, h);
            r.setArcWidth(3);
            r.setArcHeight(3);
            r.setFill(Color.WHITE);
            bars.getChildren().add(r);
        }
        Label logoText = new Label(APP_NAME.toLowerCase());
        logoText.getStyleClass().add("logo-text");
        HBox logo = new HBox(10, bars, logoText);
        logo.setAlignment(Pos.CENTER_LEFT);

        // navigation tabs
        HBox nav = new HBox(4);
        nav.getStyleClass().add("nav-pill");
        nav.setAlignment(Pos.CENTER);
        ToggleGroup group = new ToggleGroup();
        Page[] order = {Page.LISTEN, Page.MOODS, Page.LIBRARY, Page.RADIO};
        String[] names = {"Listen now", "Moods", "Library", "Radio"};
        for (int i = 0; i < order.length; i++) {
            Page p = order[i];
            ToggleButton tb = new ToggleButton(names[i]);
            tb.getStyleClass().add("nav-tab");
            tb.setToggleGroup(group);
            tb.setSelected(p == page);
            tb.setOnAction(e -> showPage(p));
            navButtons.put(p, tb);
            nav.getChildren().add(tb);
        }
        group.selectedToggleProperty().addListener((o, old, now) -> { if (now == null && old != null) old.setSelected(true); });

        // mood sync
        Label syncLabel = new Label("Mood sync");
        syncLabel.setStyle("-fx-font-weight: bold;");
        HBox sync = new HBox(10, buildSwitch(), syncLabel);
        sync.getStyleClass().add("pill-box");
        sync.setAlignment(Pos.CENTER);

        // search
        search.setPromptText("Search songs, artists, albums");
        search.getStyleClass().add("search-field");
        search.setPrefWidth(230);
        search.textProperty().addListener((o, old, txt) -> onSearchChanged());
        search.setOnAction(e -> {                       // Enter = play first result
            if (!displayed.isEmpty()) {
                Song s = displayed.get(0);
                playback.select(s);
                play(s, true);
            }
        });
        search.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) search.clear(); });
        clearSearch.getStyleClass().add("muted");
        clearSearch.setCursor(Cursor.HAND);
        clearSearch.setVisible(false);
        clearSearch.setOnMouseClicked(e -> { search.clear(); search.requestFocus(); });
        StackPane searchBox = new StackPane(search, clearSearch);
        StackPane.setAlignment(clearSearch, Pos.CENTER_RIGHT);
        StackPane.setMargin(clearSearch, new Insets(0, 14, 0, 0));

        buildProfile();

        HBox right = new HBox(12, sync, searchBox, avatar);
        right.setAlignment(Pos.CENTER_RIGHT);

        BorderPane top = new BorderPane();
        top.setLeft(logo);
        top.setCenter(nav);
        top.setRight(right);
        BorderPane.setAlignment(logo, Pos.CENTER_LEFT);
        BorderPane.setMargin(nav, new Insets(0, 24, 0, 24));
        return top;
    }

    private Node buildSwitch() {
        Rectangle track = new Rectangle(38, 20);
        track.setArcWidth(20);
        track.setArcHeight(20);
        Circle thumb = new Circle(8, Color.WHITE);
        StackPane sp = new StackPane(track, thumb);
        sp.setAlignment(Pos.CENTER_LEFT);
        sp.setMaxSize(38, 20);
        sp.setCursor(Cursor.HAND);
        Runnable apply = () -> {
            track.setFill(moodSync ? Color.web("#7dd3fc") : Color.web("#ffffff44"));
            thumb.setTranslateX(moodSync ? 20 : 2);
        };
        apply.run();
        sp.setOnMouseClicked(e -> {
            moodSync = !moodSync;
            apply.run();
            applyBackground();
        });
        return sp;
    }

    // =====================================================================
    //  Left card = three pages (Library/Moods list, Listen now, Radio)
    // =====================================================================
    private Node buildLibraryCard() {
        listPage = buildListPage();
        homePage = buildHomePage();
        radioPage = buildRadioPage();
        pages.getChildren().addAll(listPage, homePage, radioPage);

        VBox card = new VBox(pages);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(24, 24, 18, 24));
        VBox.setVgrow(pages, Priority.ALWAYS);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(560);
        return card;
    }

    // ---------------- list page (Library + Moods) ----------------
    private VBox buildListPage() {
        coverFallback.setPrefSize(COVER, COVER);
        coverFallback.getChildren().add(icon(NOTE, 60, Color.web("#ffffff77")));
        coverView.setFitWidth(COVER);
        coverView.setFitHeight(COVER);
        coverView.setPreserveRatio(false);
        coverView.setVisible(false);
        StackPane cover = new StackPane(coverFallback, coverView);
        cover.setMinSize(COVER, COVER);
        cover.setPrefSize(COVER, COVER);
        cover.setMaxSize(COVER, COVER);
        Rectangle clip = new Rectangle(COVER, COVER);
        clip.setArcWidth(38);
        clip.setArcHeight(38);
        cover.setClip(clip);
        StackPane coverWrap = new StackPane(cover);
        coverWrap.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 24, 0.1, 0, 10);");
        coverWrap.setMaxSize(COVER, COVER);

        headerChips.setAlignment(Pos.CENTER_LEFT);
        titleLabel.getStyleClass().add("big-title");
        titleLabel.setStyle("-fx-font-size: 40px;");
        playlistSub.getStyleClass().add("muted");

        headerPlay.getStyleClass().add("play-pill");
        headerPlay.setOnAction(e -> onHeaderPlay());

        shuffleBtn.getStyleClass().add("icon-btn");
        shuffleBtn.setGraphic(icon(SHUFFLE, 20, Color.WHITE));
        shuffleBtn.setOnAction(e -> playback.setShuffle(shuffleBtn.isSelected()));

        likeBtn.getStyleClass().add("icon-btn");
        likeBtn.setOnAction(e -> {
            Song s = currentOrSelected();
            if (s == null) return;
            toggleFavourite(s);
        });

        Button moreBtn = new Button();
        moreBtn.getStyleClass().add("icon-btn");
        moreBtn.setGraphic(icon(MORE, 20, Color.WHITE));
        ContextMenu moreMenu = new ContextMenu();
        MenuItem addItem = new MenuItem("Add songs...");
        addItem.setOnAction(e -> addSongs());
        MenuItem folderItem = new MenuItem("Add folder...");
        folderItem.setOnAction(e -> addFolder());
        MenuItem newPlaylistItem = new MenuItem("New playlist...");
        newPlaylistItem.setOnAction(e -> createPlaylist());
        MenuItem renamePlaylistItem = new MenuItem("Rename this playlist...");
        renamePlaylistItem.setOnAction(e -> renameCurrentPlaylist());
        MenuItem deletePlaylistItem = new MenuItem("Delete this playlist");
        deletePlaylistItem.setOnAction(e -> deleteCurrentPlaylist());
        MenuItem clearRelatedItem = new MenuItem("Clear \"More like this\"");
        clearRelatedItem.setOnAction(e -> { relatedSeed = null; refreshAll(); });
        MenuItem clearQueueItem = new MenuItem("Clear queue");
        clearQueueItem.setOnAction(e -> { playback.clearQueue(); refreshAll(); });
        MenuItem removeItem = new MenuItem("Remove selected song");
        removeItem.setOnAction(e -> {
            Song s = listView.getSelectionModel().getSelectedItem();
            if (s != null) removeSong(s);
        });
        MenuItem rereadItem = new MenuItem("Re-read tags from files");
        rereadItem.setOnAction(e -> { for (Song x : library.songs()) loadMetadata(x); });
        moreMenu.getItems().addAll(addItem, folderItem, newPlaylistItem, renamePlaylistItem,
                deletePlaylistItem, clearRelatedItem, rereadItem, clearQueueItem, removeItem);
        moreBtn.setOnAction(e -> moreMenu.show(moreBtn, Side.BOTTOM, 0, 6));

        HBox actions = new HBox(12, headerPlay, shuffleBtn, likeBtn, moreBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(6, headerChips, titleLabel, playlistSub, actions);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(24, coverWrap, info);
        header.setAlignment(Pos.CENTER_LEFT);

        // mode bar: All songs / Favourites / Queue + Sort
        ToggleGroup modeGroup = new ToggleGroup();
        for (ToggleButton b : new ToggleButton[]{allBtn, favBtn, queueBtn, playlistsBtn}) {
            b.getStyleClass().add("seg-btn");
            b.setToggleGroup(modeGroup);
        }
        allBtn.setSelected(true);
        allBtn.setOnAction(e -> { relatedSeed = null; mode = Mode.ALL; refreshAll(); });
        favBtn.setOnAction(e -> { relatedSeed = null; mode = Mode.FAV; refreshAll(); });
        queueBtn.setOnAction(e -> { relatedSeed = null; mode = Mode.QUEUE; refreshAll(); });
        playlistsBtn.setOnAction(e -> {
            relatedSeed = null;
            mode = Mode.PLAYLIST;
            if (playlists.isEmpty()) createPlaylist();
            refreshAll();
        });
        modeGroup.selectedToggleProperty().addListener((o, old, now) -> { if (now == null && old != null) old.setSelected(true); });
        playlistBox.setPrefWidth(160);
        playlistBox.setPromptText("Playlist");
        playlistBox.setOnAction(e -> {
            if (updatingPlaylistBox) return;
            selectedPlaylist = playlistBox.getValue();
            if (mode == Mode.PLAYLIST) refreshAll();
        });

        sortBox.getItems().addAll(SortOrder.labels());
        sortBox.setValue(SortOrder.DATE_ADDED.label());
        sortBox.setPrefWidth(170);
        sortBox.setOnAction(e -> {
            library.setSortOrder(SortOrder.fromLabel(sortBox.getValue()));
            refreshAll();
        });
        Label sortLabel = new Label("Sort by");
        sortLabel.getStyleClass().add("muted");
        Region modeSpacer = new Region();
        HBox.setHgrow(modeSpacer, Priority.ALWAYS);
        modeBar.getChildren().addAll(allBtn, favBtn, queueBtn, playlistsBtn, playlistBox, modeSpacer, sortLabel, sortBox);
        modeBar.setAlignment(Pos.CENTER_LEFT);

        // mood filter chips (Moods tab)
        moodChips.setPrefWrapLength(600);

        // table header
        Label hNum = fixed(new Label("#"), COL_NUM, Pos.CENTER_LEFT);
        Label hTitle = new Label("Title");
        HBox.setHgrow(hTitle, Priority.ALWAYS);
        hTitle.setMaxWidth(Double.MAX_VALUE);
        Label hMood = fixed(new Label("Mood"), COL_MOOD, Pos.CENTER_LEFT);
        Region heartSpace = new Region();
        heartSpace.setMinWidth(COL_HEART);
        heartSpace.setPrefWidth(COL_HEART);
        Label hDur = fixed(new Label(), COL_DUR, Pos.CENTER_RIGHT);
        hDur.setGraphic(icon(CLOCK, 14, Color.web("#ffffff99")));
        Region thumbSpace = new Region();
        thumbSpace.setMinWidth(56);
        thumbSpace.setPrefWidth(56);
        HBox head = new HBox(hNum, thumbSpace, hTitle, hMood, heartSpace, hDur);
        head.getStyleClass().add("table-head");
        head.setAlignment(Pos.CENTER_LEFT);
        Separator sep = new Separator();
        sep.setOpacity(0.25);

        // list + placeholder
        listView.setCellFactory(lv -> new TrackCell());
        listView.setFixedCellSize(60);
        placeholderText.getStyleClass().add("muted");
        placeholderText.setWrapText(true);
        placeholderText.setMaxWidth(380);
        placeholderText.setStyle("-fx-text-alignment: center;");
        placeholderAdd.getStyleClass().add("link-btn");
        placeholderAdd.setOnAction(e -> addSongs());
        VBox placeholder = new VBox(12, placeholderText, placeholderAdd);
        placeholder.setAlignment(Pos.CENTER);
        listView.setPlaceholder(placeholder);
        VBox.setVgrow(listView, Priority.ALWAYS);

        return new VBox(12, header, modeBar, moodChips, head, sep, listView);
    }

    // ---- one row of the main track list ----
    private class TrackCell extends ListCell<Song> {
        private final Label num = fixed(new Label(), COL_NUM, Pos.CENTER_LEFT);
        private final Label title = new Label();
        private final Label artist = new Label();
        private final HBox moodHolder = new HBox();
        private final Button heart = new Button();
        private final Label dur = fixed(new Label(), COL_DUR, Pos.CENTER_RIGHT);
        private final HBox row = new HBox();
        private final ImageView thumbImg = new ImageView();
        private final Node noteIcon = icon(NOTE, 18, Color.web("#ffffff99"));
        private final StackPane thumb = new StackPane(noteIcon, thumbImg);

        TrackCell() {
            thumb.setMinSize(44, 44);
            thumb.setPrefSize(44, 44);
            thumb.setMaxSize(44, 44);
            Rectangle tclip = new Rectangle(44, 44);
            tclip.setArcWidth(14);
            tclip.setArcHeight(14);
            thumb.setClip(tclip);
            HBox.setMargin(thumb, new Insets(0, 12, 0, 0));
            num.getStyleClass().add("muted");
            title.getStyleClass().add("track-title");
            artist.getStyleClass().addAll("muted", "track-artist");
            dur.getStyleClass().add("muted");
            VBox titleBox = new VBox(2, title, artist);
            titleBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(titleBox, Priority.ALWAYS);
            titleBox.setMaxWidth(Double.MAX_VALUE);
            moodHolder.setMinWidth(COL_MOOD);
            moodHolder.setPrefWidth(COL_MOOD);
            moodHolder.setAlignment(Pos.CENTER_LEFT);
            heart.getStyleClass().add("heart-btn");
            heart.setMinWidth(COL_HEART);
            heart.setPrefWidth(COL_HEART);
            heart.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
            heart.setOnAction(e -> {
                Song s = getItem();
                if (s == null) return;
                toggleFavourite(s);
            });
            row.getStyleClass().add("track-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(num, thumb, titleBox, moodHolder, heart, dur);
            row.setOnMouseClicked(e -> {
                Song s = getItem();
                if (s != null && e.getClickCount() == 2) {
                    if (page == Page.LIBRARY && mode == Mode.QUEUE) playback.removeFromQueue(s);   // leaves the queue when played
                    playback.select(s);
                    play(s, true);
                }
            });
        }

        @Override
        protected void updateItem(Song s, boolean empty) {
            super.updateItem(s, empty);
            setText(null);
            if (empty || s == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            num.setText(String.valueOf(getIndex() + 1));
            title.setText(s.getTitle());
            artist.setText(artistLine(s));
            showImage(thumbImg, covers.get(s), 44, 44);
            noteIcon.setVisible(covers.get(s) == null);
            thumb.setStyle(coverGradient(moodColor(s.getMood())));
            moodHolder.getChildren().setAll(chip(s.getMood(), moodColor(s.getMood())));
            heart.setGraphic(icon(s.isLiked() ? HEART_FILLED : HEART, 18,
                    s.isLiked() ? Color.web("#7dd3fc") : Color.web("#ffffff99")));
            dur.setText(s.getDurationText());
            row.pseudoClassStateChanged(PLAYING, s == loaded());

            ContextMenu cm = new ContextMenu();
            cm.getItems().addAll(queueItems(s));
            cm.getItems().add(relatedItem(s));
            cm.getItems().add(playlistMenu(s));
            if (page == Page.LIBRARY && mode == Mode.QUEUE) {
                MenuItem rm = new MenuItem("Remove from queue");
                rm.setOnAction(e -> { playback.removeFromQueue(s); refreshAll(); });
                cm.getItems().add(rm);
            }
            if (page == Page.LIBRARY && mode == Mode.PLAYLIST && selectedPlaylist != null) {
                MenuItem rmPl = new MenuItem("Remove from playlist");
                rmPl.setOnAction(e -> {
                    playlists.removeSong(selectedPlaylist, s);
                    persistLibrary();
                    refreshAll();
                });
                cm.getItems().add(rmPl);
            }
            MenuItem fav = new MenuItem(s.isLiked() ? "Remove from favourites" : "Add to favourites");
            fav.setOnAction(e -> toggleFavourite(s));
            Menu moodMenu = new Menu("Set mood");
            for (String m : MOODS) {
                MenuItem mi = new MenuItem(m);
                mi.setOnAction(e -> { s.setMood(m); persistLibrary(); graph.rebuild(library.songs()); refreshAll(); });
                moodMenu.getItems().add(mi);
            }
            MenuItem remove = new MenuItem("Remove from library");
            remove.setOnAction(e -> removeSong(s));
            cm.getItems().addAll(new SeparatorMenuItem(), fav, moodMenu, remove);
            setContextMenu(cm);
            setGraphic(row);
        }
    }

    private List<MenuItem> queueItems(Song s) {
        MenuItem next = new MenuItem("Play next");
        next.setOnAction(e -> { playback.enqueueFront(s); refreshAll(); });
        MenuItem add = new MenuItem("Add to queue");
        add.setOnAction(e -> { playback.enqueue(s); refreshAll(); });
        return List.of(next, add);
    }

    private MenuItem relatedItem(Song s) {
        MenuItem more = new MenuItem("More like this (BFS)");
        more.setOnAction(e -> showRelated(s));
        return more;
    }

    private Menu playlistMenu(Song s) {
        Menu menu = new Menu("Add to playlist");
        if (playlists.isEmpty()) {
            MenuItem create = new MenuItem("New playlist...");
            create.setOnAction(e -> {
                String name = createPlaylist();
                if (name != null) {
                    playlists.addSong(name, s);
                    persistLibrary();
                    refreshAll();
                }
            });
            menu.getItems().add(create);
            return menu;
        }
        for (String name : playlists.names()) {
            MenuItem mi = new MenuItem(name);
            mi.setOnAction(e -> {
                playlists.addSong(name, s);
                persistLibrary();
                refreshAll();
            });
            menu.getItems().add(mi);
        }
        MenuItem create = new MenuItem("New playlist...");
        create.setOnAction(e -> {
            String name = createPlaylist();
            if (name != null) {
                playlists.addSong(name, s);
                persistLibrary();
                refreshAll();
            }
        });
        menu.getItems().addAll(new SeparatorMenuItem(), create);
        return menu;
    }

    // ---------------- home page (Listen now) ----------------
    private VBox buildHomePage() {
        Label title = new Label("Listen now");
        title.getStyleClass().add("big-title");
        title.setStyle("-fx-font-size: 40px;");
        Label sub = new Label("Pick up where you left off");
        sub.getStyleClass().add("muted");

        VBox recentCol = homeSection("Recently played", "Last 8 songs (LRU cache)", recentList, false, "Nothing played yet");
        VBox topCol = homeSection("Most played", "Your top 10 (min-heap)", topList, true, "Play some songs to build your top 10");
        HBox cols = new HBox(20, recentCol, topCol);
        HBox.setHgrow(recentCol, Priority.ALWAYS);
        HBox.setHgrow(topCol, Priority.ALWAYS);
        VBox.setVgrow(cols, Priority.ALWAYS);

        return new VBox(18, new VBox(4, title, sub), cols);
    }

    private VBox homeSection(String heading, String note, ListView<Song> lv, boolean ranked, String emptyMsg) {
        Label h = new Label(heading);
        h.getStyleClass().add("section-title");
        Label n = new Label(note);
        n.getStyleClass().add("muted");
        lv.setCellFactory(x -> new MiniCell(ranked));
        lv.setFixedCellSize(58);
        Label empty = new Label(emptyMsg);
        empty.getStyleClass().add("muted");
        lv.setPlaceholder(empty);
        VBox.setVgrow(lv, Priority.ALWAYS);
        VBox col = new VBox(8, new VBox(2, h, n), lv);
        col.setMinWidth(0);
        col.setMaxWidth(Double.MAX_VALUE);
        return col;
    }

    private class MiniCell extends ListCell<Song> {
        private final boolean ranked;
        private final Label rank = fixed(new Label(), 30, Pos.CENTER_LEFT);
        private final Label title = new Label();
        private final Label artist = new Label();
        private final HBox tail = new HBox();
        private final HBox row = new HBox(8);

        MiniCell(boolean ranked) {
            this.ranked = ranked;
            rank.getStyleClass().add("muted");
            title.getStyleClass().add("track-title");
            artist.getStyleClass().addAll("muted", "track-artist");
            VBox box = new VBox(2, title, artist);
            box.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(box, Priority.ALWAYS);
            box.setMaxWidth(Double.MAX_VALUE);
            tail.setAlignment(Pos.CENTER_RIGHT);
            row.getStyleClass().add("track-row");
            row.setAlignment(Pos.CENTER_LEFT);
            if (ranked) row.getChildren().add(rank);
            row.getChildren().addAll(box, tail);
            row.setOnMouseClicked(e -> {
                Song s = getItem();
                if (s != null && e.getClickCount() == 2) {
                    playback.select(s);
                    play(s, true);
                }
            });
        }

        @Override
        protected void updateItem(Song s, boolean empty) {
            super.updateItem(s, empty);
            setText(null);
            if (empty || s == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            rank.setText(String.valueOf(getIndex() + 1));
            title.setText(s.getTitle());
            artist.setText(s.getArtist().isEmpty() ? "Unknown artist" : s.getArtist());
            if (ranked) {
                Label plays = new Label(s.getPlayCount() + (s.getPlayCount() == 1 ? " play" : " plays"));
                plays.getStyleClass().add("muted");
                tail.getChildren().setAll(plays);
            } else {
                tail.getChildren().setAll(chip(s.getMood(), moodColor(s.getMood())));
            }
            row.pseudoClassStateChanged(PLAYING, s == loaded());
            ContextMenu cm = new ContextMenu();
            cm.getItems().addAll(queueItems(s));
            cm.getItems().add(relatedItem(s));
            cm.getItems().add(playlistMenu(s));
            setContextMenu(cm);
            setGraphic(row);
        }
    }

    // ---------------- radio page ----------------
    private VBox buildRadioPage() {
        Label title = new Label("Radio");
        title.getStyleClass().add("big-title");
        title.setStyle("-fx-font-size: 40px;");
        Label sub = new Label("Endless shuffled stations built from your library");
        sub.getStyleClass().add("muted");

        stationStatus.setStyle("-fx-font-weight: bold;");
        stopRadioBtn.getStyleClass().add("link-btn");
        stopRadioBtn.setOnAction(e -> {
            playback.stopStation();
            refreshAll();
        });
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox status = new HBox(12, stationStatus, sp, stopRadioBtn);
        status.setAlignment(Pos.CENTER_LEFT);
        status.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 16; -fx-padding: 12 18 12 18;");

        stationPane.setPadding(new Insets(4));
        ScrollPane scroll = new ScrollPane(stationPane);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return new VBox(16, new VBox(4, title, sub), status, scroll);
    }

    private void startStation(Station st) {
        Song first = playback.startStation(st);
        if (first == null) new Alert(Alert.AlertType.INFORMATION, "No songs available for \"" + st.name() + "\" yet.").show();
        else play(first, true);
    }

    private Node tile(String title, String sub, Color c, boolean chosen, Runnable action) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label s = new Label(sub);
        s.setWrapText(true);
        s.setStyle("-fx-text-fill: rgba(255,255,255,0.85);");
        VBox box = new VBox(6, t, s);
        box.getStyleClass().add("tile");
        box.setMinSize(200, 100);
        box.setPrefSize(200, 100);
        box.setMaxSize(200, 100);
        box.setStyle("-fx-background-color: linear-gradient(to bottom right, " + hex(c.deriveColor(0, 0.9, 0.7, 1))
                + ", " + hex(c.deriveColor(0, 0.9, 0.32, 1)) + ");");
        box.pseudoClassStateChanged(CHOSEN, chosen);
        box.setOnMouseClicked(e -> action.run());
        return box;
    }

    // =====================================================================
    //  Side column (mood card + artist card)
    // =====================================================================
    private Node buildSideColumn() {
        Label moodTitle = new Label("Mood of this playlist");
        moodTitle.getStyleClass().add("card-title");
        moodSubtitle.getStyleClass().add("muted");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox moodHead = new HBox(moodTitle, sp, moodSubtitle);
        moodHead.setAlignment(Pos.CENTER_LEFT);

        moodBar.setHgap(3);
        moodBar.setMinHeight(10);
        moodBar.setPrefHeight(10);
        Rectangle barClip = new Rectangle(10, 10);
        barClip.setArcWidth(10);
        barClip.setArcHeight(10);
        moodBar.setClip(barClip);
        moodBar.layoutBoundsProperty().addListener((o, a, b) -> {
            barClip.setWidth(b.getWidth());
            barClip.setHeight(b.getHeight());
        });

        VBox moodCard = new VBox(16, moodHead, moodBar, moodLegend);
        moodCard.getStyleClass().add("glass-card");
        moodCard.setPadding(new Insets(22, 24, 22, 24));

        Label visTitle = new Label("Visualizer");
        visTitle.getStyleClass().add("card-title");
        visStyle.getStyleClass().add("muted");
        visStyle.setText(spectrum.styleName() + "  ·  tap to change");
        Region visSp = new Region();
        HBox.setHgrow(visSp, Priority.ALWAYS);
        HBox visHead = new HBox(visTitle, visSp, visStyle);
        visHead.setAlignment(Pos.CENTER_LEFT);

        spectrum.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        mediaView.setPreserveRatio(true);
        mediaView.setVisible(false);
        mediaView.setManaged(false);

        StackPane panel = new StackPane(spectrum, mediaView);
        mediaView.fitWidthProperty().bind(panel.widthProperty().subtract(16));
        mediaView.fitHeightProperty().bind(panel.heightProperty().subtract(16));
        panel.setStyle("-fx-background-color: rgba(8,10,30,0.35); -fx-background-radius: 20;");
        panel.setPadding(new Insets(14));
        panel.setMinHeight(150);
        panel.setCursor(Cursor.HAND);
        Rectangle panelClip = new Rectangle(10, 10);
        panelClip.setArcWidth(40);
        panelClip.setArcHeight(40);
        panel.setClip(panelClip);
        panel.layoutBoundsProperty().addListener((o, a, b) -> {
            panelClip.setWidth(b.getWidth());
            panelClip.setHeight(b.getHeight());
        });
        panel.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && hasVideoTrack) {
                toggleVideoPopout(panel);
                return;
            }
            if (hasVideoTrack) {
                showVideo = !showVideo;
                updateVisualizerMode();
            } else {
                spectrum.nextStyle();
                visStyle.setText(spectrum.styleName() + "  ·  tap to change");
            }
        });
        VBox.setVgrow(panel, Priority.ALWAYS);

        visNow.getStyleClass().add("muted");
        visNow.setWrapText(true);
        VBox visCard = new VBox(14, visHead, panel, visNow);
        visCard.getStyleClass().add("glass-card");
        visCard.setPadding(new Insets(22, 24, 22, 24));
        VBox.setVgrow(visCard, Priority.ALWAYS);

        VBox col = new VBox(20, moodCard, visCard);
        col.setPrefWidth(340);
        col.setMinWidth(320);
        col.setMaxWidth(360);
        return col;
    }

    private void updateVisualizerMode() {
        if (hasVideoTrack && showVideo && (videoPopoutStage == null || !videoPopoutStage.isShowing())) {
            mediaView.setVisible(true);
            mediaView.setManaged(true);
            spectrum.setVisible(false);
            visStyle.setText("Video playback  ·  tap for visualizer  ·  double-click to pop out");
        } else {
            mediaView.setVisible(false);
            mediaView.setManaged(false);
            spectrum.setVisible(true);
            visStyle.setText(spectrum.styleName() + (hasVideoTrack ? "  ·  tap for video" : "  ·  tap to change"));
        }
    }

    private void toggleVideoPopout(StackPane mainPanel) {
        if (!hasVideoTrack || player == null) return;
        if (videoPopoutStage != null && videoPopoutStage.isShowing()) {
            videoPopoutStage.close();
            return;
        }
        mainPanel.getChildren().remove(mediaView);
        mediaView.fitWidthProperty().unbind();
        mediaView.fitHeightProperty().unbind();

        StackPane popoutRoot = new StackPane(mediaView);
        popoutRoot.setStyle("-fx-background-color: #050510;");
        Scene popoutScene = new Scene(popoutRoot, 854, 480);
        mediaView.fitWidthProperty().bind(popoutScene.widthProperty());
        mediaView.fitHeightProperty().bind(popoutScene.heightProperty());

        videoPopoutStage = new Stage();
        String title = playback.nowPlaying() != null ? playback.nowPlaying().getTitle() : "Video";
        videoPopoutStage.setTitle("LataBeats - " + title);
        videoPopoutStage.setScene(popoutScene);
        videoPopoutStage.setMinWidth(480);
        videoPopoutStage.setMinHeight(300);
        videoPopoutStage.setOnHidden(ev -> {
            mediaView.fitWidthProperty().unbind();
            mediaView.fitHeightProperty().unbind();
            if (!mainPanel.getChildren().contains(mediaView)) {
                mainPanel.getChildren().add(mediaView);
            }
            mediaView.fitWidthProperty().bind(mainPanel.widthProperty().subtract(16));
            mediaView.fitHeightProperty().bind(mainPanel.heightProperty().subtract(16));
            updateVisualizerMode();
        });
        videoPopoutStage.show();
        updateVisualizerMode();
    }

    // =====================================================================
    //  Player bar
    // =====================================================================
    private Node buildPlayerBar() {
        nowTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        nowArtist.getStyleClass().add("muted");
        VBox nowText = new VBox(3, nowTitle, nowArtist);
        nowText.setAlignment(Pos.CENTER_LEFT);
        nowThumb.setMinSize(52, 52);
        nowThumb.setPrefSize(52, 52);
        nowThumb.setMaxSize(52, 52);
        Rectangle nclip = new Rectangle(52, 52);
        nclip.setArcWidth(16);
        nclip.setArcHeight(16);
        nowThumb.setClip(nclip);
        nowThumb.setStyle(coverGradient(Color.web("#6366f1")));
        HBox nowBox = new HBox(12, nowThumb, nowText);
        nowBox.setAlignment(Pos.CENTER_LEFT);
        nowBox.setPrefWidth(280);
        nowBox.setMinWidth(220);

        Button back = iconButton(UNDO, 18);
        Button prev = iconButton(PREV, 18);
        Button next = iconButton(NEXT, 18);
        ToggleButton repeat = new ToggleButton();
        repeat.getStyleClass().add("icon-btn");
        repeat.setGraphic(icon(REPEAT, 18, Color.WHITE));
        playBtn.getStyleClass().add("play-circle");

        back.setOnAction(e -> goBack());
        prev.setOnAction(e -> play(playback.previous(), true));
        next.setOnAction(e -> play(playback.next(), true));
        playBtn.setOnAction(e -> togglePlay());
        repeat.setOnAction(e -> playback.setRepeatAll(repeat.isSelected()));

        HBox buttons = new HBox(12, back, prev, playBtn, next, repeat);
        buttons.setAlignment(Pos.CENTER);

        elapsedLabel.getStyleClass().add("muted");
        totalLabel.getStyleClass().add("muted");
        seek.setOnMousePressed(e -> seeking = true);
        seek.setOnMouseReleased(e -> {
            if (player != null) player.seek(Duration.seconds(seek.getValue()));
            seeking = false;
        });
        HBox seekRow = new HBox(10, elapsedLabel, seek, totalLabel);
        seekRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(seek, Priority.ALWAYS);

        VBox center = new VBox(8, buttons, seekRow);
        center.setAlignment(Pos.CENTER);
        HBox.setHgrow(center, Priority.ALWAYS);

        volume.setPrefWidth(110);
        volume.valueProperty().addListener((o, a, b) -> { if (player != null) player.setVolume(b.doubleValue()); });
        HBox volBox = new HBox(8, icon(VOLUME, 18, Color.web("#ffffffcc")), volume);
        volBox.setAlignment(Pos.CENTER_RIGHT);
        volBox.setPrefWidth(180);
        volBox.setMinWidth(160);

        HBox bar = new HBox(24, nowBox, center, volBox);
        bar.getStyleClass().add("player-bar");
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(14, 26, 14, 26));
        return bar;
    }

    // =====================================================================
    //  Tabs, search, sort
    // =====================================================================
    private void showPage(Page p) {
        page = p;
        applyPageVisibility();
        refreshAll();
    }

    private void applyPageVisibility() {
        boolean listish = page == Page.LIBRARY || page == Page.MOODS;
        setShown(listPage, listish);
        setShown(homePage, page == Page.LISTEN);
        setShown(radioPage, page == Page.RADIO);
        setShown(modeBar, page == Page.LIBRARY);
        setShown(moodChips, page == Page.MOODS);
    }

    private static void setShown(Node n, boolean on) {
        n.setVisible(on);
        n.setManaged(on);
    }

    private void onSearchChanged() {
        boolean has = !search.getText().isBlank();
        clearSearch.setVisible(!search.getText().isEmpty());
        if (has) {
            if (page != Page.LIBRARY) {                     // searching always happens in the Library
                page = Page.LIBRARY;
                navButtons.get(Page.LIBRARY).setSelected(true);
                applyPageVisibility();
            }
            if (mode == Mode.QUEUE) {
                mode = Mode.ALL;
                allBtn.setSelected(true);
            }
            relatedSeed = null;
        }
        refreshAll();
    }

    // =====================================================================
    //  Library actions
    // =====================================================================
    private void addSongs() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Add Songs & Videos");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Media (*.mp3, *.wav, *.m4a, *.mp4)", "*.mp3", "*.wav", "*.m4a", "*.mp4", "*.m4v"),
                new FileChooser.ExtensionFilter("MP4 Video & Audio (*.mp4, *.m4v)", "*.mp4", "*.m4v"),
                new FileChooser.ExtensionFilter("MP3 Audio (*.mp3)", "*.mp3"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );
        List<File> files = fc.showOpenMultipleDialog(stage);
        if (files != null) addFiles(files);
    }

    private void addFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose a music folder");
        File dir = dc.showDialog(stage);
        if (dir == null) return;
        addFiles(FileScanner.audioFilesIn(dir));
        rememberFolder(dir);
    }

    /** Adds files to the library and starts reading their tags in the background. */
    private void addFiles(List<File> files) {
        List<Song> added = library.addAll(files);
        favourites.applyTo(added);                       // mark the songs this user already hearted
        for (Song s : added) loadMetadata(s);
        graph.rebuild(library.songs());
        persistLibrary();
        refreshAll();
    }

    private void removeSong(Song s) {
        if (s == playback.nowPlaying()) stopPlayer();
        if (relatedSeed == s) relatedSeed = null;
        playlists.forget(s);
        library.remove(s);
        playback.forget(s);
        covers.remove(s);
        graph.rebuild(library.songs());
        persistLibrary();
        refreshAll();
    }

    private void onLikeChanged() {
        listView.refresh();
        updateLikeIcon();
        if (page == Page.LIBRARY && mode == Mode.FAV) refreshAll();
        else updateBadges();
    }

    // =====================================================================
    //  Playback
    // =====================================================================
    private void onHeaderPlay() {
        boolean contextView = page == Page.MOODS
                || (page == Page.LIBRARY && (mode == Mode.FAV || mode == Mode.PLAYLIST || relatedSeed != null));
        if (contextView) {
            if (player != null && loaded() != null && displayed.contains(loaded())) {
                togglePlay();
                return;
            }
            Song first = playback.playContext(displayed);       // first song now, the rest through the queue
            if (first != null) play(first, true);
        } else if (page == Page.LIBRARY && mode == Mode.QUEUE) {
            if (player != null) {
                togglePlay();
            } else {
                Song s = playback.takeFromQueue();
                if (s != null) play(s, true);
            }
        } else {
            togglePlay();
        }
    }

    private void play(Song s, boolean recordHistory) {
        if (s == null) return;
        stopPlayer();

        MediaPlayer p;
        try {
            Media media = new Media(new File(s.getPath()).toURI().toString());
            p = new MediaPlayer(media);
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Cannot play: " + s.getPath()).show();
            return;
        }
        player = p;
        playback.started(s, recordHistory);
        lastTime = 0;
        lastShownSecond = -1;
        seek.setValue(0);

        p.setVolume(volume.getValue());
        p.setAudioSpectrumNumBands(SpectrumView.BANDS);
        p.setAudioSpectrumInterval(0.04);
        p.setAudioSpectrumThreshold(-60);
        p.setAudioSpectrumListener((timestamp, duration, mags, phases) -> {
            float[] lv = new float[mags.length];
            for (int i = 0; i < lv.length; i++) {
                float v = (mags[i] + 60f) / 60f;                 // dB (-60..0) -> 0..1
                v *= 0.8f + 0.5f * i / lv.length;               // lift the quiet high bands a little
                lv[i] = Math.max(0f, Math.min(1f, v));
            }
            spectrum.setLevels(lv);
        });
        p.setOnReady(() -> {
            double total = p.getTotalDuration().toSeconds();
            seek.setMax(total);
            totalLabel.setText(fmt(p.getTotalDuration()));
            if (s.getDurationSeconds() <= 0) {
                s.setDurationSeconds(total);
                refreshAll();
            }
            int width = p.getMedia().getWidth();
            int height = p.getMedia().getHeight();
            hasVideoTrack = (width > 0 && height > 0);
            if (hasVideoTrack) {
                mediaView.setMediaPlayer(p);
                showVideo = true;
                if (videoPopoutStage != null && videoPopoutStage.isShowing()) {
                    videoPopoutStage.setTitle("LataBeats - " + s.getTitle());
                }
            } else {
                mediaView.setMediaPlayer(null);
            }
            updateVisualizerMode();
        });
        p.currentTimeProperty().addListener((o, a, t) -> {
            double now = t.toSeconds();
            if (!seeking) seek.setValue(now);
            stats.record(s.getMood(), lastTime, now);
            lastTime = now;
            int sec = (int) now;
            if (sec != lastShownSecond) {
                lastShownSecond = sec;
                elapsedLabel.setText(fmt(t));
                refreshMoodCard();
            }
        });
        p.statusProperty().addListener((o, a, b) -> updatePlayIcons());
        p.setOnEndOfMedia(() -> {
            Song n = playback.next();
            if (n != null) play(n, true);
            else updatePlayIcons();
        });
        p.play();
        playBtn.requestFocus();

        persistLibrary();
        refreshAll();
        listView.getSelectionModel().select(s);
        listView.scrollTo(s);
    }

    private void goBack() {
        Song s = playback.back();
        if (s != null) play(s, false);            // false: don't push history again, or Back would loop
    }

    private void togglePlay() {
        if (player == null) {
            Song s = currentOrSelected();
            if (s == null) s = playback.currentSong();
            if (s != null) {
                playback.select(s);
                play(s, true);
            }
            return;
        }
        if (player.getStatus() == MediaPlayer.Status.PLAYING) player.pause();
        else player.play();
        updatePlayIcons();
    }

    private void stopPlayer() {
        if (player != null) {
            mediaView.setMediaPlayer(null);
            hasVideoTrack = false;
            updateVisualizerMode();
            player.stop();
            player.dispose();
            player = null;
        }
        updatePlayIcons();
    }

    private void persistLibrary() {
        if (libraryStore != null) {
            libraryStore.save(library, playlists, playback, stats);
        }
    }

    private void refreshPlaylistBox() {
        if (updatingPlaylistBox) return;
        updatingPlaylistBox = true;
        try {
            List<String> names = playlists.names();
            if (selectedPlaylist == null || !names.contains(selectedPlaylist)) {
                selectedPlaylist = names.isEmpty() ? null : names.get(0);
            }
            if (!playlistBox.getItems().equals(names)) {
                playlistBox.getItems().setAll(names);
            }
            playlistBox.setValue(selectedPlaylist);
        } finally {
            updatingPlaylistBox = false;
        }
    }

    private String createPlaylist() {
        TextInputDialog d = new TextInputDialog("New Playlist");
        d.setTitle("New Playlist");
        d.setHeaderText(null);
        d.setContentText("Playlist name:");
        Optional<String> res = d.showAndWait();
        if (res.isPresent()) {
            String name = res.get().trim();
            if (!name.isEmpty()) {
                String created = playlists.create(name);
                selectedPlaylist = created;
                persistLibrary();
                refreshAll();
                return created;
            }
        }
        return null;
    }

    private void renameCurrentPlaylist() {
        if (selectedPlaylist == null) return;
        TextInputDialog d = new TextInputDialog(selectedPlaylist);
        d.setTitle("Rename Playlist");
        d.setHeaderText(null);
        d.setContentText("New playlist name:");
        d.showAndWait().ifPresent(newName -> {
            String clean = newName.trim();
            if (!clean.isEmpty()) {
                int i = playlists.names().indexOf(selectedPlaylist);
                playlists.rename(selectedPlaylist, clean);
                if (i >= 0 && i < playlists.names().size()) {
                    selectedPlaylist = playlists.names().get(i);
                }
                persistLibrary();
                refreshAll();
            }
        });
    }

    private void deleteCurrentPlaylist() {
        if (selectedPlaylist == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete playlist \"" + selectedPlaylist + "\"?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Playlist");
        alert.setHeaderText(null);
        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            playlists.delete(selectedPlaylist);
            selectedPlaylist = playlists.isEmpty() ? null : playlists.names().get(0);
            persistLibrary();
            refreshAll();
        }
    }

    private void showRelated(Song s) {
        if (s == null) return;
        page = Page.LIBRARY;
        ToggleButton b = navButtons.get(Page.LIBRARY);
        if (b != null) b.setSelected(true);
        applyPageVisibility();
        relatedSeed = s;
        refreshAll();
    }

    // =====================================================================
    //  Refreshing the UI
    // =====================================================================
    private void refreshAll() {
        refreshPlaylistBox();
        setShown(playlistBox, page == Page.LIBRARY && mode == Mode.PLAYLIST);
        rebuildView();
        updateHeader();
        updateBadges();
        if (page == Page.LISTEN) refreshHome();
        if (page == Page.MOODS) refreshMoodChips();
        if (page == Page.RADIO) refreshRadio();
        refreshMoodCard();
        updateNowPlaying();
        updateCover();
        updateLikeIcon();
        updatePlayIcons();
        applyBackground();
    }

    /** Works out which songs the main list should show for the current tab / mode / search text. */
    private void rebuildView() {
        List<Song> result = new ArrayList<>();
        String q = search.getText() == null ? "" : search.getText().trim();
        Set<Song> matches = q.isEmpty() ? null : library.search(q);

        if (page == Page.LIBRARY && mode == Mode.QUEUE) {
            result.addAll(playback.queue());
        } else if (page == Page.LIBRARY && relatedSeed != null && matches == null) {
            result.add(relatedSeed);
            result.addAll(graph.related(relatedSeed, 29));
        } else if (page == Page.LIBRARY && mode == Mode.PLAYLIST) {
            for (Song s : playlists.songs(selectedPlaylist)) {
                if (matches != null && !matches.contains(s)) continue;
                result.add(s);
            }
        } else {
            for (Song s : library.songs()) {
                if (page == Page.MOODS && !s.getMood().equals(selectedMood)) continue;
                if (page == Page.LIBRARY && mode == Mode.FAV && !s.isLiked()) continue;
                if (matches != null && !matches.contains(s)) continue;
                result.add(s);
            }
        }
        if (!displayed.equals(result)) {                      // only touch the list when something changed
            Song sel = listView.getSelectionModel().getSelectedItem();
            displayed.setAll(result);
            if (sel != null && displayed.contains(sel)) listView.getSelectionModel().select(sel);
        }
        listView.refresh();
        updatePlaceholder();
    }

    private void updatePlaceholder() {
        String q = search.getText() == null ? "" : search.getText().trim();
        String msg;
        boolean showAdd = false;
        if (page == Page.LIBRARY && mode == Mode.QUEUE) msg = "Your queue is empty. Right-click a song and choose 'Add to queue'.";
        else if (!q.isEmpty()) msg = "No results for '" + q + "'";
        else if (page == Page.MOODS) msg = "No songs with the mood '" + selectedMood + "' yet. Right-click a song to set its mood.";
        else if (page == Page.LIBRARY && mode == Mode.FAV) msg = "No favourites yet. Tap the heart on a song.";
        else if (page == Page.LIBRARY && mode == Mode.PLAYLIST) msg = "This playlist is empty. Right-click a song and choose 'Add to playlist'.";
        else if (page == Page.LIBRARY && relatedSeed != null) msg = "No related songs yet. Add more tracks that share artist, album or mood.";
        else {
            msg = "Your library is empty";
            showAdd = true;
        }
        placeholderText.setText(msg);
        setShown(placeholderAdd, showAdd);
    }

    private void updateBadges() {
        favBtn.setText("Favourites (" + library.likedCount() + ")");
        queueBtn.setText("Queue (" + playback.queueSize() + ")");
        playlistsBtn.setText("Playlists (" + playlists.names().size() + ")");
    }

    private void updateHeader() {
        String t, eyebrow;
        if (page == Page.MOODS) {
            t = selectedMood;
            eyebrow = "MOOD";
        } else if (page == Page.LIBRARY && relatedSeed != null) {
            t = "More like: " + relatedSeed.getTitle();
            eyebrow = "GRAPH BFS";
        } else if (mode == Mode.FAV) {
            t = "Favourites";
            eyebrow = "FAVOURITES";
        } else if (mode == Mode.QUEUE) {
            t = "Up Next";
            eyebrow = "QUEUE";
        } else if (mode == Mode.PLAYLIST) {
            t = selectedPlaylist != null ? selectedPlaylist : "Playlists";
            eyebrow = "PLAYLIST";
        } else {
            t = PLAYLIST_TITLE;
            eyebrow = "PLAYLIST";
        }
        titleLabel.setText(t);

        int n = displayed.size();
        double total = 0;
        for (Song s : displayed) total += s.getDurationSeconds();
        String q = search.getText() == null ? "" : search.getText().trim();
        boolean searching = !q.isEmpty() && !(page == Page.LIBRARY && mode == Mode.QUEUE);
        if (searching) playlistSub.setText(n + (n == 1 ? " result" : " results") + " for '" + q + "'");
        else playlistSub.setText(user.displayName() + "  ·  " + n + (n == 1 ? " song" : " songs") + "  ·  " + Math.round(total / 60) + " min");

        Label eb = new Label(eyebrow);
        eb.getStyleClass().add("eyebrow");
        Node chip;
        Color c;
        if (page == Page.MOODS) {
            chip = chip(selectedMood, moodColor(selectedMood));
            c = moodColor(selectedMood);
        } else if (loaded() != null) {
            chip = chip(loaded().getMood(), moodColor(loaded().getMood()));
            c = moodColor(loaded().getMood());
        } else {
            chip = chip("All moods", Color.web("#cbd5e1"));
            c = Color.web("#818cf8");
        }
        headerChips.getChildren().setAll(eb, chip);
        coverFallback.setStyle("-fx-background-color: linear-gradient(to bottom right, " + hex(c.deriveColor(0, 0.9, 0.75, 1))
                + ", " + hex(c.deriveColor(0, 0.9, 0.35, 1)) + ");");
    }

    private void refreshMoodChips() {
        moodChips.getChildren().clear();
        for (String m : MOODS) {
            int count = library.withMood(m).size();
            Label l = chip(m + "  ·  " + count, moodColor(m));
            l.getStyleClass().add("mood-filter");
            l.pseudoClassStateChanged(CHOSEN, m.equals(selectedMood));
            l.setOnMouseClicked(e -> {
                selectedMood = m;
                refreshAll();
            });
            moodChips.getChildren().add(l);
        }
    }

    private void refreshHome() {
        recentItems.setAll(playback.recentlyPlayed());
        topItems.setAll(library.mostPlayed(10));
        recentList.refresh();
        topList.refresh();
    }

    private void refreshRadio() {
        stationPane.getChildren().clear();
        Station active = playback.station();
        for (Station st : radio.stations()) {
            int n = st.songs().size();
            boolean on = active != null && active.name().equals(st.name());
            stationPane.getChildren().add(tile(st.name(), n + (n == 1 ? " song" : " songs") + "  ·  tap to start",
                    stationColor(st.accent()), on, () -> startStation(st)));
        }
        if (active != null) {
            stationStatus.setText("On air: " + active.name() + "  ·  " + playback.queueSize() + " songs queued");
        } else {
            stationStatus.setText("No station playing. Pick one below.");
        }
        setShown(stopRadioBtn, active != null);
    }

    private void refreshMoodCard() {
        ListeningStats.Breakdown b = stats.breakdown(library.songs());
        moodSubtitle.setText(b.byTime ? "By listening time" : "By song count");

        moodBar.getChildren().clear();
        moodBar.getColumnConstraints().clear();
        moodLegend.getChildren().clear();

        if (b.shares.isEmpty()) {
            Label empty = new Label("Add songs to see your moods");
            empty.getStyleClass().add("muted");
            moodLegend.getChildren().add(empty);
            return;
        }

        int col = 0;
        for (ListeningStats.Share sh : b.shares) {
            Region seg = new Region();
            seg.setPrefHeight(10);
            seg.setMinHeight(10);
            seg.setStyle("-fx-background-color: " + hex(moodColor(sh.mood)) + ";");
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(sh.fraction * 100);
            moodBar.getColumnConstraints().add(cc);
            moodBar.add(seg, col++, 0);
        }
        int shown = Math.min(4, b.shares.size());
        for (int i = 0; i < shown; i++) {
            ListeningStats.Share sh = b.shares.get(i);
            Label name = new Label(sh.mood, new Circle(4, moodColor(sh.mood)));
            name.setGraphicTextGap(10);
            name.getStyleClass().add("legend-text");
            Region fill = new Region();
            HBox.setHgrow(fill, Priority.ALWAYS);
            Label pct = new Label(Math.round(sh.fraction * 100) + "%");
            pct.getStyleClass().add("muted");
            HBox r = new HBox(name, fill, pct);
            r.setAlignment(Pos.CENTER_LEFT);
            moodLegend.getChildren().add(r);
        }
    }

    private void updateNowPlaying() {
        nowTitle.setText(loaded() != null ? loaded().getTitle() : "Nothing playing");
        nowArtist.setText(loaded() != null ? artistLine(loaded()) : "");
        showImage(nowThumbImg, loaded() != null ? covers.get(loaded()) : null, 52, 52);
        nowNote.setVisible(loaded() == null || covers.get(loaded()) == null);
        visNow.setText(loaded() != null ? loaded().getTitle() + "  ·  " + (loaded().getArtist().isEmpty() ? "Unknown artist" : loaded().getArtist())
                : "Play a song to see it come alive");
        spectrum.setColor(loaded() != null ? moodColor(loaded().getMood()) : Color.web("#7dd3fc"));
        nowThumb.setStyle(coverGradient(loaded() != null ? moodColor(loaded().getMood()) : Color.web("#6366f1")));
    }

    private void updateLikeIcon() {
        Song s = currentOrSelected();
        boolean liked = s != null && s.isLiked();
        likeBtn.setGraphic(icon(liked ? HEART_FILLED : HEART, 20, liked ? Color.web("#7dd3fc") : Color.WHITE));
    }

    private void updatePlayIcons() {
        boolean playing = player != null && player.getStatus() == MediaPlayer.Status.PLAYING;
        playBtn.setGraphic(icon(playing ? PAUSE : PLAY, 22, DARK));
        headerPlay.setGraphic(icon(playing ? PAUSE : PLAY, 16, DARK));
        headerPlay.setText(playing ? "Pause" : "Play");
        spectrum.setActive(playing);
    }

    /** Mood sync: tint the background with the colour of the current song's mood. */
    private void applyBackground() {
        if (!moodSync || loaded() == null) {
            root.setStyle("");
            return;
        }
        Color dark = moodColor(loaded().getMood()).deriveColor(0, 0.8, 0.38, 1);
        root.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #0f2a4a 0%, "
                + hex(dark) + " 55%, #12091f 100%);");
    }

    // =====================================================================
    //  Metadata (tags inside the file) and album art
    // =====================================================================

    private void loadMetadata(Song s) {
        File file = new File(s.getPath());
        Thread worker = new Thread(() -> {
            MetadataService.Result r = metadata.load(file);          // blocking work, so it runs off the UI thread
            Image cover = toImage(r.coverBytes());
            if (cover == null && r.coverFile != null) {
                Image im = new Image(r.coverFile.toURI().toString(), 800, 0, true, true);
                if (!im.isError()) cover = im;
            }
            Image finalCover = cover;
            Platform.runLater(() -> {
                if (r.seconds > 0) s.setDurationSeconds(r.seconds);
                library.applyTags(s, r.tags);
                if (finalCover != null) covers.put(s, finalCover);
                refreshAll();
                if (r.seconds <= 0) probeDuration(s);                // last resort: ask the media player
            });
        }, "tag-reader");
        worker.setDaemon(true);
        worker.start();
    }

    private static Image toImage(byte[] bytes) {
        if (bytes == null) return null;
        try {
            Image im = new Image(new ByteArrayInputStream(bytes), 800, 0, true, true);
            return im.isError() ? null : im;
        } catch (Exception e) {
            return null;
        }
    }

    private void probeDuration(Song s) {
        try {
            MediaPlayer probe = new MediaPlayer(new Media(new File(s.getPath()).toURI().toString()));
            probe.setOnReady(() -> {
                s.setDurationSeconds(probe.getTotalDuration().toSeconds());
                probe.dispose();
                refreshAll();
            });
            probe.setOnError(probe::dispose);
        } catch (Exception ignored) {
            // duration stays "--:--"
        }
    }

    private void updateCover() {
        Image img = loaded() != null ? covers.get(loaded()) : null;
        if (loaded() == null) {                                   // nothing playing: show the first picture we have
            for (Song s : displayed) {
                Image c = covers.get(s);
                if (c != null) {
                    img = c;
                    break;
                }
            }
        }
        showImage(coverView, img, COVER, COVER);
    }

    private static String artistLine(Song s) {
        String a = s.getArtist().isEmpty() ? "Unknown artist" : s.getArtist();
        return s.getAlbum().isEmpty() ? a : a + "  ·  " + s.getAlbum();
    }

    /** Shows an image center-cropped to w x h (like CSS object-fit: cover). */
    private static void showImage(ImageView iv, Image img, double w, double h) {
        iv.setImage(img);
        iv.setVisible(img != null);
        if (img == null) return;
        iv.setPreserveRatio(false);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        double iw = img.getWidth(), ih = img.getHeight();
        if (iw <= 0 || ih <= 0) {
            iv.setViewport(null);
            return;
        }
        double ar = w / h, iar = iw / ih;
        if (iar > ar) {
            double vw = ih * ar;
            iv.setViewport(new Rectangle2D((iw - vw) / 2, 0, vw, ih));
        } else {
            double vh = iw / ar;
            iv.setViewport(new Rectangle2D(0, (ih - vh) / 2, iw, vh));
        }
    }

    private static String coverGradient(Color c) {
        return "-fx-background-color: linear-gradient(to bottom right, " + hex(c.deriveColor(0, 0.9, 0.7, 1))
                + ", " + hex(c.deriveColor(0, 0.9, 0.32, 1)) + ");";
    }

    // =====================================================================
    //  Session: favourites, remembered folder, log out
    // =====================================================================

    /** Loads this user's favourites and music folder from MySQL, then fills the library. */
    private void startSession() {
        Thread t = new Thread(() -> {
            String problem = null;
            try {
                favourites.load();
            } catch (DatabaseException e) {
                problem = e.getMessage();
            }
            String finalProblem = problem;
            Platform.runLater(() -> {
                if (finalProblem != null) new Alert(Alert.AlertType.WARNING, "Could not load your favourites: " + finalProblem).show();
                List<File> files = new ArrayList<>();
                if (user.musicFolder() != null) {
                    File dir = new File(user.musicFolder());
                    if (dir.isDirectory()) files.addAll(FileScanner.audioFilesIn(dir));    // songs come from the folder, not from MySQL
                }
                files.addAll(startupFiles);
                if (!files.isEmpty()) addFiles(files);
                if (libraryStore != null) {
                    LibraryStore.Snapshot snap = libraryStore.load();
                    library.applySaved(snap);
                    playlists.restore(snap.playlists, library);
                    playback.restoreRecent(snap.recentPaths, library);
                    stats.restore(snap.moodSeconds);
                    graph.rebuild(library.songs());
                    refreshAll();
                }
            });
        }, "session-start");
        t.setDaemon(true);
        t.start();
    }

    private void toggleFavourite(Song s) {
        favourites.setFavourite(s, !s.isLiked());        // updates the song at once, saves to MySQL in the background
        onLikeChanged();
    }

    private void rememberFolder(File dir) {
        Thread t = new Thread(() -> {
            try {
                user = auth.rememberMusicFolder(user, dir);
            } catch (AuthException ex) {
                Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, ex.getMessage()).show());
            }
        }, "remember-folder");
        t.setDaemon(true);
        t.start();
    }

    private void logout() {
        stopPlayer();
        favourites.close();                              // makes sure every favourite is saved first
        new MainUI().begin(new Stage(), List.of());      // new login window + completely fresh state
        stage.close();
    }

    // =====================================================================
    //  Profile (picture, name comes from the logged-in user)
    // =====================================================================

    private void buildProfile() {
        avatarText.setStyle("-fx-font-weight: bold; -fx-text-fill: #3b1d12;");
        avatar.getChildren().addAll(avatarText, avatarImg);
        avatar.getStyleClass().add("avatar");
        avatar.setCursor(Cursor.HAND);
        avatar.setClip(new Circle(21, 21, 21));
        Tooltip.install(avatar, new Tooltip("Your profile - click to add or change your picture"));
        avatar.setOnMouseClicked(e -> showProfileMenu());
        refreshProfile();
    }

    private void showProfileMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem who = new MenuItem(user.displayName() + "  (@" + user.username() + ")");
        who.setDisable(true);
        MenuItem change = new MenuItem(profileImage == null ? "Add profile picture..." : "Change profile picture...");
        change.setOnAction(e -> chooseProfilePhoto());
        MenuItem remove = new MenuItem("Remove picture");
        remove.setDisable(profileImage == null);
        remove.setOnAction(e -> removeProfilePhoto());
        MenuItem rename = new MenuItem("Edit my name...");
        rename.setOnAction(e -> editProfileName());
        MenuItem logout = new MenuItem("Log out");
        logout.setOnAction(e -> logout());
        menu.getItems().addAll(who, new SeparatorMenuItem(), change, remove, rename, new SeparatorMenuItem(), logout);
        menu.show(avatar, Side.BOTTOM, 0, 8);
    }

    private void chooseProfilePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose your profile picture");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        Image img = new Image(f.toURI().toString(), 512, 0, true, true);
        if (img.isError()) {
            new Alert(Alert.AlertType.ERROR, "Could not open that image.").show();
            return;
        }
        profileImage = img;
        profileStore.setPhoto(f);
        profileStore.save();
        refreshProfile();
    }

    private void removeProfilePhoto() {
        profileImage = null;
        profileStore.clearPhoto();
        profileStore.save();
        refreshProfile();
    }

    private void editProfileName() {
        TextInputDialog d = new TextInputDialog(user.displayName());
        d.setTitle("Your profile");
        d.setHeaderText(null);
        d.setContentText("Your name:");
        d.showAndWait().ifPresent(entered -> {
            Thread t = new Thread(() -> {
                try {
                    user = auth.changeDisplayName(user, entered);         // saved in MySQL
                    Platform.runLater(() -> {
                        refreshProfile();
                        refreshAll();
                    });
                } catch (AuthException ex) {
                    Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, ex.getMessage()).show());
                }
            }, "rename");
            t.setDaemon(true);
            t.start();
        });
    }

    private void refreshProfile() {
        showImage(avatarImg, profileImage, 42, 42);
        avatarText.setVisible(profileImage == null);
        avatarText.setText(avatarLetters(user.displayName()));
    }

    private static String avatarLetters(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        if (words.length >= 2) {
            sb.append(words[0].charAt(0)).append(words[1].charAt(0));
        } else if (!words[0].isEmpty()) {
            sb.append(words[0], 0, Math.min(2, words[0].length()));
        }
        return sb.length() == 0 ? "ME" : sb.toString().toUpperCase();
    }

    private void loadProfileImage() {
        File photo = profileStore.photo();
        if (photo == null) return;
        Image im = new Image(photo.toURI().toString(), 512, 0, true, true);
        if (!im.isError()) profileImage = im;
    }

    // =====================================================================
    //  Small helpers
    // =====================================================================
    private Song loaded() {
        return playback.nowPlaying();
    }

    private static Color stationColor(String accent) {
        switch (accent) {
            case "all":        return Color.web("#818cf8");
            case "favourites": return Color.web("#f472b6");
            case "discover":   return Color.web("#34d399");
            default:           return moodColor(accent);
        }
    }

    private Song currentOrSelected() {
        return loaded() != null ? loaded() : listView.getSelectionModel().getSelectedItem();
    }

    private static Label fixed(Label l, double width, Pos align) {
        l.setMinWidth(width);
        l.setPrefWidth(width);
        l.setMaxWidth(width);
        l.setAlignment(align);
        return l;
    }

    private static Label chip(String text, Color dot) {
        Label l = new Label(text, new Circle(4, dot));
        l.setGraphicTextGap(7);
        l.getStyleClass().add("chip");
        return l;
    }

    private static Node icon(String path, double size, Color color) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.setFill(color);
        double scale = size / 24.0;
        p.setScaleX(scale);
        p.setScaleY(scale);
        return new Group(p);
    }

    private static Button iconButton(String path, double size) {
        Button b = new Button();
        b.getStyleClass().add("icon-btn");
        b.setGraphic(icon(path, size, Color.WHITE));
        return b;
    }

    private static Color moodColor(String mood) {
        return MOOD_COLORS.getOrDefault(mood, Color.web("#cbd5e1"));
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }

    private static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) sb.append(Character.toUpperCase(part.charAt(0)));
        }
        return sb.length() == 0 ? "♪" : sb.toString();
    }

    private static String fmt(Duration d) {
        if (d == null || d.isUnknown()) return "0:00";
        int sec = (int) d.toSeconds();
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}
