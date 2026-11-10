package musicplayer.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import musicplayer.db.Database;
import musicplayer.db.DatabaseException;
import musicplayer.model.User;
import musicplayer.service.AuthException;
import musicplayer.service.AuthService;
import musicplayer.service.LocalSettings;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * The login / registration window. It only draws the forms and calls AuthService - all rules
 * (validation, duplicates, lock-out) live in the service layer.
 */
public class AuthView {

    private final Stage stage;
    private final Database database;
    private final AuthService auth;
    private final LocalSettings settings;
    private final Consumer<User> onSuccess;

    private final TextField loginUser = field("login-user", "User name or e-mail");
    private final PasswordField loginPass = password("login-pass", "Password");
    private final TextField regUser = field("reg-user", "Choose a user name");
    private final TextField regEmail = field("reg-email", "you@example.com");
    private final PasswordField regPass = password("reg-pass", "At least 8 characters");
    private final PasswordField regConfirm = password("reg-confirm", "Type the password again");
    private final Button signInBtn = new Button("Sign in");
    private final Button registerBtn = new Button("Create account");
    private final Label message = new Label();
    private VBox signInForm, registerForm;

    public AuthView(Stage stage, Database database, AuthService auth, LocalSettings settings, Consumer<User> onSuccess) {
        this.stage = stage;
        this.database = database;
        this.auth = auth;
        this.settings = settings;
        this.onSuccess = onSuccess;
    }

    public void show() {
        // logo
        HBox bars = new HBox(2);
        bars.setAlignment(Pos.BOTTOM_CENTER);
        for (double h : new double[]{12, 24, 17, 28, 14}) {
            Rectangle r = new Rectangle(4, h);
            r.setArcWidth(4);
            r.setArcHeight(4);
            r.setFill(Color.WHITE);
            bars.getChildren().add(r);
        }
        Label logoText = new Label("latabeats");
        logoText.getStyleClass().add("logo-text");
        logoText.setStyle("-fx-font-size: 30px;");
        HBox logo = new HBox(12, bars, logoText);
        logo.setAlignment(Pos.CENTER);
        Label tagline = new Label("Your music, your favourites, everywhere you sign in.");
        tagline.getStyleClass().add("muted");
        tagline.setWrapText(true);
        tagline.setStyle("-fx-text-alignment: center;");

        // tabs
        ToggleButton signInTab = new ToggleButton("Sign in");
        ToggleButton registerTab = new ToggleButton("Create account");
        signInTab.setId("tab-signin");
        registerTab.setId("tab-register");
        ToggleGroup group = new ToggleGroup();
        for (ToggleButton b : new ToggleButton[]{signInTab, registerTab}) {
            b.getStyleClass().add("seg-btn");
            b.setToggleGroup(group);
        }
        signInTab.setSelected(true);
        HBox tabs = new HBox(8, signInTab, registerTab);
        tabs.setAlignment(Pos.CENTER);

        signInForm = buildSignInForm();
        registerForm = buildRegisterForm();
        showForm(false);
        group.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null) old.setSelected(true);
            else showForm(now == registerTab);
        });

        message.setWrapText(true);
        message.setMaxWidth(Double.MAX_VALUE);
        message.setId("auth-message");
        setShown(message, false);

        VBox card = new VBox(16, logo, tagline, tabs, signInForm, registerForm, message);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(32, 34, 24, 34));
        card.setMaxWidth(440);
        card.setMaxHeight(Region.USE_PREF_SIZE);                // card is only as tall as its content
        card.setMinWidth(440);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("root-bg");
        root.setPadding(new Insets(24));

        Scene scene = new Scene(root, 560, 760);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("LataBeats - Sign in");
        stage.setScene(scene);
        stage.setMinWidth(520);
        stage.setMinHeight(700);
        stage.show();

        // remember the last user name; jump straight to the password box
        String last = settings.lastUser();
        loginUser.setText(last);
        if (last.isEmpty()) loginUser.requestFocus();
        else loginPass.requestFocus();
    }

    // ---------------- forms ----------------
    private VBox buildSignInForm() {
        loginUser.setOnAction(e -> loginPass.requestFocus());
        loginPass.setOnAction(e -> doLogin());
        signInBtn.setId("btn-signin");
        styleSubmit(signInBtn);
        signInBtn.setOnAction(e -> doLogin());
        return new VBox(8, label("User name or e-mail"), loginUser, label("Password"), loginPass, spacer(6), signInBtn);
    }

    private VBox buildRegisterForm() {
        regUser.setOnAction(e -> regEmail.requestFocus());
        regEmail.setOnAction(e -> regPass.requestFocus());
        regPass.setOnAction(e -> regConfirm.requestFocus());
        regConfirm.setOnAction(e -> doRegister());
        registerBtn.setId("btn-register");
        styleSubmit(registerBtn);
        registerBtn.setOnAction(e -> doRegister());
        Label hint = new Label("Password: at least 8 characters with a letter and a digit.");
        hint.getStyleClass().add("muted");
        hint.setStyle("-fx-font-size: 11px;");
        hint.setWrapText(true);
        return new VBox(8, label("User name"), regUser, label("E-mail"), regEmail,
                label("Password"), regPass, hint, label("Confirm password"), regConfirm, spacer(6), registerBtn);
    }

    private void showForm(boolean register) {
        setShown(signInForm, !register);
        setShown(registerForm, register);
        setShown(message, false);
        stage.setTitle(register ? "LataBeats - Create account" : "LataBeats - Sign in");
    }

    // ---------------- actions ----------------
    private void doLogin() {
        String u = loginUser.getText(), p = loginPass.getText();
        run("Signing in...", () -> auth.login(u, p));
    }

    private void doRegister() {
        String u = regUser.getText(), e = regEmail.getText(), p = regPass.getText(), c = regConfirm.getText();
        run("Creating your account...", () -> auth.register(u, e, p, c));
    }

    /** Runs the database work off the UI thread so the window never freezes. */
    private void run(String busyText, Callable<User> job) {
        setBusy(true);
        showMessage(busyText, false);
        Thread t = new Thread(() -> {
            User user = null;
            String error = null;
            try {
                database.init();                 // creates the database/tables on first use
                user = job.call();
            } catch (AuthException | DatabaseException e) {
                error = e.getMessage();
            } catch (Exception e) {
                error = "Unexpected error: " + e;
            }
            User result = user;
            String err = error;
            Platform.runLater(() -> {
                setBusy(false);
                if (result != null) {
                    settings.setLastUser(result.username());
                    onSuccess.accept(result);    // opens the player window first ...
                    stage.close();               // ... then closes this one (so the app never has zero windows)
                } else {
                    showMessage(err, true);
                    loginPass.clear();
                }
            });
        }, "auth");
        t.setDaemon(true);
        t.start();
    }

    // ---------------- small helpers ----------------
    private void setBusy(boolean busy) {
        signInBtn.setDisable(busy);
        registerBtn.setDisable(busy);
    }

    private void showMessage(String text, boolean error) {
        message.setText(text);
        message.getStyleClass().removeAll("error-text", "ok-text", "muted");
        message.getStyleClass().add(error ? "error-text" : "muted");
        setShown(message, true);
    }

    private static void styleSubmit(Button b) {
        b.getStyleClass().add("play-pill");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-alignment: center;");
    }

    private static TextField field(String id, String prompt) {
        TextField f = new TextField();
        f.setId(id);
        f.setPromptText(prompt);
        f.getStyleClass().add("auth-field");
        return f;
    }

    private static PasswordField password(String id, String prompt) {
        PasswordField f = new PasswordField();
        f.setId(id);
        f.setPromptText(prompt);
        f.getStyleClass().add("auth-field");
        return f;
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    private static Node spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setPrefHeight(h);
        return r;
    }

    private static void setShown(Node n, boolean on) {
        n.setVisible(on);
        n.setManaged(on);
    }
}
