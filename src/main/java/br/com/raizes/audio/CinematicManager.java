package br.com.raizes.audio;

import br.com.raizes.assets.AssetManager;
import br.com.raizes.config.GameConfig;
import java.awt.Component;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import javax.swing.SwingUtilities;

/** Completion means natural end of every scene; cancellation and failure never complete a playlist. */
public final class CinematicManager {
    public enum PlaybackState { IDLE, LOADING, PLAYING, FAILED, COMPLETED }

    private final Consumer<String> onError;
    private final Runnable onCancelled;
    private final JFXPanel panel = new JFXPanel();
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicBoolean sessionOpen = new AtomicBoolean();
    private volatile PlaybackState state = PlaybackState.IDLE;
    // Remaining playback state is confined to the JavaFX thread.
    private final PauseTransition startupTimeout = new PauseTransition(Duration.seconds(12));
    private final PauseTransition retryDelay = new PauseTransition(Duration.millis(200));
    private MediaView mediaView;
    private Label title, caption, statusMessage;
    private VBox statusBox;
    private ProgressIndicator progress;
    private HBox recovery;
    private MediaPlayer player;
    private List<CinematicScene> scenes = List.of();
    private Runnable onFinished;
    private String sequenceTitle;
    private boolean active;
    private int sceneIndex, retryCount;
    private long activeSequence, sceneRevision;

    public CinematicManager(Consumer<String> onError) { this(onError, () -> { }); }

    public CinematicManager(Consumer<String> onError, Runnable onCancelled) {
        this.onError = Objects.requireNonNull(onError);
        this.onCancelled = Objects.requireNonNull(onCancelled);
        panel.setBounds(0, 0, GameConfig.W, GameConfig.H);
        panel.setVisible(false);
        Platform.setImplicitExit(false);
        Platform.runLater(this::initializePanel);
    }

    public Component panel() { return panel; }
    public PlaybackState state() { return state; }

    public void start(List<CinematicScene> playlist, String heading, Runnable finished) {
        List<CinematicScene> snapshot = List.copyOf(playlist);
        Objects.requireNonNull(finished);
        long request = sequence.incrementAndGet();
        sessionOpen.set(true);
        Platform.runLater(() -> {
            if (request != sequence.get()) return;
            activeSequence = request;
            scenes = snapshot;
            sequenceTitle = Objects.requireNonNullElse(heading, "CINEMÁTICA");
            onFinished = finished;
            sceneIndex = 0;
            retryCount = 0;
            active = true;
            onSwingThread(() -> {
                if (request == sequence.get() && !snapshot.isEmpty()) {
                    panel.setVisible(true);
                    panel.requestFocusInWindow();
                }
            });
            loadCurrentScene();
        });
    }

    public void retry() {
        long request = sequence.get();
        Platform.runLater(() -> {
            if (request == sequence.get() && active && state == PlaybackState.FAILED) {
                retryCount = 0;
                loadCurrentScene();
            }
        });
    }

    public void cancel() { stop(true); }
    public void dispose() { stop(false); }

    private void stop(boolean notifyCancellation) {
        boolean wasOpen = sessionOpen.getAndSet(false);
        if (notifyCancellation && !wasOpen) return;
        long request = sequence.incrementAndGet(); // Also invalidates completion already queued on Swing.
        Platform.runLater(() -> {
            if (request != sequence.get()) return;
            active = false;
            state = PlaybackState.IDLE;
            sceneRevision++;
            releasePlayer();
            scenes = List.of();
            onFinished = null;
            onSwingThread(() -> {
                if (request != sequence.get()) return;
                panel.setVisible(false);
                if (notifyCancellation && wasOpen) onCancelled.run();
            });
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
        dialogue.setStyle("-fx-background-color: rgba(8, 22, 23, 0.88); -fx-background-radius: 12px;");
        dialogue.visibleProperty().bind(caption.textProperty().isNotEmpty());
        dialogue.managedProperty().bind(dialogue.visibleProperty());
        BorderPane overlay = new BorderPane();
        overlay.setMouseTransparent(true);
        BorderPane.setMargin(dialogue, new Insets(0, 70, 42, 70));
        overlay.setBottom(dialogue);

        progress = new ProgressIndicator();
        progress.setMaxSize(54, 54);
        statusMessage = new Label();
        statusMessage.setWrapText(true);
        statusMessage.setMaxWidth(760);
        statusMessage.setStyle("-fx-text-fill: white; -fx-font-size: 23px; -fx-text-alignment: center;");
        Button retry = new Button("Tentar novamente");
        retry.setOnAction(event -> retry());
        Button back = new Button("Voltar");
        back.setOnAction(event -> cancel());
        recovery = new HBox(20, retry, back);
        recovery.setAlignment(Pos.CENTER);
        statusBox = new VBox(24, progress, statusMessage, recovery);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.92); -fx-padding: 36px;");
        root.getChildren().addAll(mediaView, overlay, statusBox);
        Scene scene = new Scene(root, GameConfig.W, GameConfig.H);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) { cancel(); event.consume(); }
            else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                if (state == PlaybackState.FAILED && scene.getFocusOwner() instanceof Button) return;
                if (state == PlaybackState.FAILED) retry();
                event.consume(); // Input cannot synthesize a natural end of media.
            }
        });
        panel.setScene(scene);
    }

    private void loadCurrentScene() {
        long revision = ++sceneRevision;
        releasePlayer();
        if (!active || activeSequence != sequence.get()) return;
        if (sceneIndex >= scenes.size()) { finishNaturally(); return; }
        CinematicScene current = scenes.get(sceneIndex);
        title.setText(sequenceTitle + "  •  CENA " + (sceneIndex + 1));
        caption.setText(current.caption());
        showStatus(PlaybackState.LOADING, "Carregando vídeo…", false);
        startupTimeout.setOnFinished(event -> failScene(revision, current.video(),
            new IllegalStateException("O vídeo não iniciou em 12 segundos.")));
        startupTimeout.playFromStart();
        // Filesystem access and extraction of packaged resources cannot block either UI thread.
        CompletableFuture.supplyAsync(() -> AssetManager.optionalFile(current.video())).whenComplete((file, error) ->
            Platform.runLater(() -> {
                if (!isCurrent(revision)) return;
                if (error != null) failScene(revision, current.video(), error);
                else if (file == null) failScene(revision, current.video(),
                    new IllegalArgumentException("Vídeo não encontrado: " + current.video()));
                else preparePlayer(revision, file);
            }));
    }

    private void preparePlayer(long revision, File file) {
        String path = file.getAbsolutePath();
        try {
            Media media = new Media(file.toURI().toString());
            media.setOnError(() -> failScene(revision, path, media.getError()));
            MediaPlayer nextPlayer = new MediaPlayer(media);
            player = nextPlayer;
            mediaView.setMediaPlayer(nextPlayer);
            nextPlayer.setOnError(() -> failScene(revision, path, nextPlayer.getError()));
            nextPlayer.setOnHalted(() -> failScene(revision, path, nextPlayer.getError()));
            nextPlayer.setOnEndOfMedia(() -> {
                if (!isCurrent(revision) || state != PlaybackState.PLAYING) return;
                // Invalidate this scene before accepting another end callback.
                sceneRevision++;
                sceneIndex++;
                retryCount = 0;
                if (sceneIndex == scenes.size()) finishNaturally();
                else loadCurrentScene();
            });
            nextPlayer.setOnPlaying(() -> {
                if (!isCurrent(revision)) return;
                startupTimeout.stop();
                state = PlaybackState.PLAYING;
                statusBox.setVisible(false);
            });
            nextPlayer.setOnStalled(() -> {
                if (!isCurrent(revision)) return;
                statusMessage.setText("Aguardando o vídeo…");
                progress.setVisible(true);
                recovery.setVisible(false);
                statusBox.setVisible(true);
                startupTimeout.playFromStart();
            });
            nextPlayer.setOnReady(() -> { if (isCurrent(revision)) nextPlayer.play(); });
            if (media.getError() != null) failScene(revision, path, media.getError());
            else if (nextPlayer.getError() != null) failScene(revision, path, nextPlayer.getError());
            else if (nextPlayer.getStatus() == MediaPlayer.Status.READY) nextPlayer.play();
        } catch (RuntimeException exception) { failScene(revision, path, exception); }
    }

    private boolean isCurrent(long revision) {
        return active && activeSequence == sequence.get() && revision == sceneRevision;
    }

    private void failScene(long revision, String path, Throwable error) {
        if (!isCurrent(revision)) return;
        long failedRevision = ++sceneRevision;
        releasePlayer();
        if (retryCount == 0 && error instanceof MediaException mediaError
                && mediaError.getType() == MediaException.Type.UNKNOWN) {
            retryCount++;
            showStatus(PlaybackState.LOADING, "Preparando o vídeo novamente…", false);
            System.err.println("[Entre Mitos e Raízes] Reiniciando decoder de " + path + ": " + error);
            retryDelay.setOnFinished(event -> { if (isCurrent(failedRevision)) loadCurrentScene(); });
            retryDelay.playFromStart();
            return;
        }
        String message = "Não foi possível reproduzir " + new File(path).getName()
            + ". Tente novamente ou volte à tela anterior.";
        showStatus(PlaybackState.FAILED, message, true);
        System.err.println("[Entre Mitos e Raízes] " + path + ": " + error);
        long request = activeSequence;
        onSwingThread(() -> { if (request == sequence.get()) onError.accept(message); });
    }

    private void showStatus(PlaybackState next, String message, boolean failed) {
        state = next;
        statusMessage.setText(message);
        progress.setVisible(!failed);
        progress.setManaged(!failed);
        recovery.setVisible(failed);
        recovery.setManaged(failed);
        statusBox.setVisible(true);
    }

    private void finishNaturally() {
        if (!active || activeSequence != sequence.get()) return;
        active = false;
        state = PlaybackState.COMPLETED;
        sceneRevision++;
        startupTimeout.stop();
        retryDelay.stop();
        statusBox.setVisible(false);
        Runnable finished = onFinished;
        onFinished = null;
        long request = activeSequence;
        onSwingThread(() -> {
            if (request != sequence.get()) return;
            // The last video frame remains attached until the next screen is ready.
            sessionOpen.set(false);
            if (finished != null) finished.run();
            if (request != sequence.get()) return; // The callback may start another playlist.
            panel.setVisible(false);
            Platform.runLater(() -> { if (request == sequence.get()) releasePlayer(); });
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
        previous.setOnStalled(null);
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
