package br.com.raizes.audio;

import br.com.raizes.assets.AssetManager;
import br.com.raizes.config.GameConfig;
import java.io.File;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javax.swing.SwingUtilities;

/** Owns JavaFX video playback and its in-game narration overlay. */
public final class CinematicManager {
    private final String[] videos;
    private final String[] narration;
    private final Consumer<String> onError;
    private final Runnable onFinished;
    private final JFXPanel panel = new JFXPanel();
    private volatile MediaPlayer player;
    private volatile boolean active;
    private volatile int sceneIndex;
    private volatile Label title;
    private volatile Label caption;

    public CinematicManager(String[] videos, String[] narration, Consumer<String> onError, Runnable onFinished) {
        this.videos = videos.clone();
        this.narration = narration.clone();
        this.onError = onError;
        this.onFinished = onFinished;
        panel.setBounds(0, 0, GameConfig.W, GameConfig.H);
        panel.setVisible(false);
        Platform.setImplicitExit(false);
        Platform.runLater(this::initializePanel);
    }

    public JFXPanel panel() { return panel; }

    public void setVisible(boolean visible) { panel.setVisible(visible); }

    public void start() {
        onSwingThread(() -> {
            stopPlayer();
            sceneIndex = 0;
            active = true;
            panel.setVisible(true);
            launchNextAvailableScene();
        });
    }

    public void advance() {
        onSwingThread(() -> {
            if (!active) return;
            sceneIndex++;
            stopPlayer();
            launchNextAvailableScene();
        });
    }

    public void skip() {
        onSwingThread(this::finish);
    }

    private void initializePanel() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");
        MediaView mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        mediaView.fitWidthProperty().bind(root.widthProperty());
        mediaView.fitHeightProperty().bind(root.heightProperty());

        BorderPane overlay = new BorderPane();
        title = new Label();
        title.setStyle("-fx-text-fill: #ffd85c; -fx-font-size: 22px; -fx-font-weight: bold;");
        caption = new Label();
        caption.setWrapText(true);
        caption.setMaxWidth(980);
        caption.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        VBox dialogue = new VBox(10, title, caption);
        dialogue.setAlignment(Pos.CENTER_LEFT);
        dialogue.setPadding(new Insets(20, 30, 22, 195));
        dialogue.setMaxWidth(Double.MAX_VALUE);
        dialogue.setStyle("-fx-background-color: rgba(8, 22, 23, 0.88); -fx-background-radius: 12px;");
        BorderPane.setMargin(dialogue, new Insets(0, 70, 42, 70));
        overlay.setBottom(dialogue);
        root.getChildren().addAll(mediaView, overlay);

        Scene scene = new Scene(root, GameConfig.W, GameConfig.H);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER || event.getCode() == javafx.scene.input.KeyCode.SPACE) advance();
            else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) skip();
        });
        scene.setOnMouseClicked(event -> advance());
        panel.setScene(scene);
        if (active) onSwingThread(this::launchNextAvailableScene);
    }

    private void launchNextAvailableScene() {
        if (!active) return;
        File videoFile = null;
        while (sceneIndex < videos.length && videoFile == null) {
            videoFile = AssetManager.optionalFile(videos[sceneIndex]);
            if (videoFile == null) sceneIndex++;
        }
        if (sceneIndex >= videos.length) {
            finish();
            return;
        }

        int currentScene = sceneIndex;
        File resolvedVideo = videoFile;
        Platform.runLater(() -> {
            if (!active || currentScene != sceneIndex || title == null || caption == null) return;
            stopPlayer();
            try {
                title.setText("PRÓLOGO  •  CENA " + (currentScene + 1));
                caption.setText(currentScene < narration.length ? narration[currentScene] : "");
                MediaPlayer nextPlayer = new MediaPlayer(new Media(resolvedVideo.toURI().toString()));
                player = nextPlayer;
                nextPlayer.setOnEndOfMedia(this::advance);
                nextPlayer.setOnError(() -> onSwingThread(() -> {
                    if (active && currentScene == sceneIndex) {
                        onError.accept("Não foi possível reproduzir " + resolvedVideo.getName() + ". A próxima cena será iniciada.");
                        advance();
                    }
                }));
                nextPlayer.setOnReady(() -> {
                    if (player == nextPlayer && active) {
                        MediaView view = (MediaView) ((StackPane) panel.getScene().getRoot()).getChildren().get(0);
                        view.setMediaPlayer(nextPlayer);
                        nextPlayer.play();
                    }
                });
            } catch (RuntimeException exception) {
                onSwingThread(() -> {
                    if (active && currentScene == sceneIndex) {
                        onError.accept("Falha ao abrir " + resolvedVideo.getName() + ". A próxima cena será iniciada.");
                        advance();
                    }
                });
            }
        });
    }

    private void finish() {
        if (!active) return;
        active = false;
        stopPlayer();
        panel.setVisible(false);
        onFinished.run();
    }

    private void stopPlayer() {
        MediaPlayer currentPlayer = player;
        player = null;
        if (currentPlayer == null) return;
        if (Platform.isFxApplicationThread()) currentPlayer.stop();
        else Platform.runLater(currentPlayer::stop);
    }

    private void onSwingThread(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else SwingUtilities.invokeLater(task);
    }
}
