package br.com.raizes.audio;

import br.com.raizes.assets.AssetManager;
import br.com.raizes.config.GameConfig;
import java.awt.Component;
import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import javax.swing.SwingUtilities;

/** Owns playback on the JavaFX thread and delivers game callbacks on the Swing thread. */
public final class CinematicManager {
    private final Consumer<String> onError;
    private final JFXPanel panel = new JFXPanel();
    private final AtomicLong sequence = new AtomicLong();
    // All fields below belong exclusively to the JavaFX Application Thread.
    private final Set<KeyCode> pressed = EnumSet.noneOf(KeyCode.class);
    private final PauseTransition startupTimeout = new PauseTransition(Duration.seconds(12));
    private final PauseTransition retryDelay = new PauseTransition(Duration.millis(200));
    private MediaView mediaView;
    private Label title;
    private Label caption;
    private MediaPlayer player;
    private List<CinematicScene> scenes = List.of();
    private Runnable onFinished;
    private String sequenceTitle;
    private boolean active;
    private int sceneIndex;
    private int retryCount;
    private long activeSequence;
    private long sceneRevision;

    public CinematicManager(Consumer<String> onError) {
        this.onError = Objects.requireNonNull(onError);
        panel.setBounds(0, 0, GameConfig.W, GameConfig.H);
        panel.setVisible(false);
        Platform.setImplicitExit(false);
        Platform.runLater(this::initializePanel);
    }

    public Component panel() { return panel; }

    public void start(List<CinematicScene> playlist, String heading, Runnable finished) {
        List<CinematicScene> snapshot = List.copyOf(playlist);
        Objects.requireNonNull(finished);
        long request = sequence.incrementAndGet();
        Platform.runLater(() -> {
            if (request != sequence.get()) return;
            activeSequence = request;
            scenes = snapshot;
            sequenceTitle = Objects.requireNonNullElse(heading, "CINEMÁTICA");
            onFinished = finished;
            sceneIndex = 0;
            retryCount = 0;
            active = true;
            pressed.clear();
            onSwingThread(() -> {
                if (request == sequence.get() && !snapshot.isEmpty()) {
                    panel.setVisible(true);
                    panel.requestFocusInWindow();
                }
            });
            loadCurrentScene();
        });
    }

    public void advance() { Platform.runLater(this::advanceOnFxThread); }

    public void skip() { Platform.runLater(this::finish); }

    /** Releases native decoders when the game is closed or playback is cancelled. */
    public void dispose() {
        long request = sequence.incrementAndGet();
        Platform.runLater(() -> {
            if (request != sequence.get()) return;
            active = false;
            sceneRevision++;
            releasePlayer();
            scenes = List.of();
            onFinished = null;
            onSwingThread(() -> { if (request == sequence.get()) panel.setVisible(false); });
        });
    }

    private void initializePanel() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        mediaView.fitWidthProperty().bind(root.widthProperty());
        mediaView.fitHeightProperty().bind(root.heightProperty());

        title = new Label();
        title.setStyle("-fx-text-fill: #ffd85c; -fx-font-size: 22px; -fx-font-weight: bold;");
        caption = new Label();
        caption.setWrapText(true);
        caption.setMaxWidth(980);
        caption.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        VBox dialogue = new VBox(10, title, caption);
        dialogue.setAlignment(Pos.CENTER_LEFT);
        dialogue.setPadding(new Insets(20, 30, 22, 30));
        dialogue.setMaxWidth(Double.MAX_VALUE);
        dialogue.setStyle("-fx-background-color: rgba(8, 22, 23, 0.88); -fx-background-radius: 12px;");
        // Scenes without narration keep the video unobstructed.
        dialogue.visibleProperty().bind(caption.textProperty().isNotEmpty());
        dialogue.managedProperty().bind(dialogue.visibleProperty());
        BorderPane overlay = new BorderPane();
        BorderPane.setMargin(dialogue, new Insets(0, 70, 42, 70));
        overlay.setBottom(dialogue);
        root.getChildren().addAll(mediaView, overlay);

        Scene scene = new Scene(root, GameConfig.W, GameConfig.H);
        scene.setOnKeyPressed(event -> {
            if (!pressed.add(event.getCode())) return;
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) advanceOnFxThread();
            else if (event.getCode() == KeyCode.ESCAPE) finish();
        });
        scene.setOnKeyReleased(event -> pressed.remove(event.getCode()));
        scene.setOnMouseClicked(event -> advanceOnFxThread());
        panel.setScene(scene);
    }

    private void advanceOnFxThread() {
        if (!active || activeSequence != sequence.get()) return;
        sceneIndex++;
        retryCount = 0;
        loadCurrentScene();
    }

    private void loadCurrentScene() {
        long revision = ++sceneRevision;
        releasePlayer();
        if (!active || activeSequence != sequence.get()) return;
        File video = null;
        while (sceneIndex < scenes.size()) {
            video = AssetManager.optionalFile(scenes.get(sceneIndex).video());
            if (video != null) break;
            reportError("Vídeo não encontrado: " + scenes.get(sceneIndex).video(), null);
            sceneIndex++;
            retryCount = 0;
        }
        if (sceneIndex >= scenes.size()) { finish(); return; }

        CinematicScene current = scenes.get(sceneIndex);
        title.setText(sequenceTitle + "  •  CENA " + (sceneIndex + 1));
        caption.setText(current.caption());
        String path = video.getAbsolutePath();
        try {
            Media media = new Media(video.toURI().toString());
            media.setOnError(() -> failScene(revision, path, media.getError()));
            MediaPlayer nextPlayer = new MediaPlayer(media);
            player = nextPlayer;
            mediaView.setMediaPlayer(nextPlayer);
            nextPlayer.setOnError(() -> failScene(revision, path, nextPlayer.getError()));
            nextPlayer.setOnHalted(() -> failScene(revision, path, nextPlayer.getError()));
            nextPlayer.setOnEndOfMedia(() -> {
                if (isCurrent(revision)) advanceOnFxThread();
            });
            nextPlayer.setOnPlaying(() -> { if (isCurrent(revision)) startupTimeout.stop(); });
            nextPlayer.setOnReady(() -> { if (isCurrent(revision)) nextPlayer.play(); });
            startupTimeout.setOnFinished(event -> failScene(revision, path,
                new IllegalStateException("O vídeo não iniciou em 12 segundos.")));
            startupTimeout.playFromStart();
            if (media.getError() != null) failScene(revision, path, media.getError());
            else if (nextPlayer.getError() != null) failScene(revision, path, nextPlayer.getError());
            else if (nextPlayer.getStatus() == MediaPlayer.Status.READY) nextPlayer.play();
        } catch (RuntimeException exception) {
            failScene(revision, path, exception);
        }
    }

    private boolean isCurrent(long revision) {
        return active && activeSequence == sequence.get() && revision == sceneRevision;
    }

    private void failScene(long revision, String path, Throwable error) {
        if (!isCurrent(revision)) return;
        // Invalidate immediately: Media and MediaPlayer may report the same error.
        long failedRevision = ++sceneRevision;
        releasePlayer();
        // Native decoders can fail transiently during rapid teardown/recreation.
        if (retryCount == 0 && error instanceof MediaException mediaError
                && mediaError.getType() == MediaException.Type.UNKNOWN) {
            retryCount++;
            System.err.println("[Entre Mitos e Raízes] Reiniciando o decoder de " + path + ": " + error);
            retryDelay.setOnFinished(event -> { if (isCurrent(failedRevision)) loadCurrentScene(); });
            retryDelay.playFromStart();
            return;
        }
        reportError("Não foi possível reproduzir " + path, error);
        Platform.runLater(() -> {
            if (isCurrent(failedRevision)) advanceOnFxThread();
        });
    }

    private void reportError(String message, Throwable error) {
        System.err.println("[Entre Mitos e Raízes] " + message + (error == null ? "" : ": " + error));
        if (error != null) error.printStackTrace(System.err);
        long request = activeSequence;
        onSwingThread(() -> {
            if (request == sequence.get()) onError.accept("Uma cena não pôde ser carregada. Continuando a campanha.");
        });
    }

    private void finish() {
        if (!active || activeSequence != sequence.get()) return;
        active = false;
        sceneRevision++;
        releasePlayer();
        scenes = List.of();
        pressed.clear();
        Runnable finished = onFinished;
        onFinished = null;
        long request = activeSequence;
        onSwingThread(() -> {
            if (request != sequence.get()) return;
            panel.setVisible(false);
            if (finished != null) finished.run();
        });
    }

    private void releasePlayer() {
        retryDelay.stop();
        retryDelay.setOnFinished(null);
        startupTimeout.stop();
        startupTimeout.setOnFinished(null);
        if (mediaView != null) mediaView.setMediaPlayer(null);
        MediaPlayer previous = player;
        player = null;
        if (previous == null) return;
        previous.setOnReady(null);
        previous.setOnPlaying(null);
        previous.setOnEndOfMedia(null);
        previous.setOnError(null);
        previous.setOnHalted(null);
        previous.getMedia().setOnError(null);
        previous.dispose();
    }

    private static void onSwingThread(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }
}
