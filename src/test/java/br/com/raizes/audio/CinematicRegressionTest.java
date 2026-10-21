package br.com.raizes.audio;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.MediaPlayer;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;

/** Real decoder regressions: natural completion, responsive input, failure recovery and cancellation. */
public final class CinematicRegressionTest {
    private static final CinematicScene FIRST = new CinematicScene("Videos/cena1.mp4", "Primeira cena");
    private static final CinematicScene SECOND = new CinematicScene("Videos/cena2.mp4", "Segunda cena");

    public static void main(String[] args) {
        int result = 0;
        try {
            catalogConfiguration();
            manualInputMustNotComplete();
            naturalPlayback();
            cancellationAndStaleEvents();
            missingVideoRecovery();
            corruptedVideoDoesNotComplete();
            System.out.println("OK: término natural, entradas, cancelamento, eventos antigos, erro e recuperação.");
        } catch (Throwable error) { result = 1; error.printStackTrace(); }
        finally { Platform.exit(); }
        System.exit(result);
    }

    private static void catalogConfiguration() throws Exception {
        Path file = Files.createTempFile("raizes-cinematics-test-", ".properties");
        try {
            Files.writeString(file, """
                prologue.10.video=Videos/cena2.mp4
                prologue.2.video=Videos/cena1.mp4
                prologue.2.caption=Árvores e raízes
                prologue.3.video=
                chapter.2.1.video=Videos/saci.mp4
                chapter.4.1.video=Videos/negrinho.mp4
                """, StandardCharsets.UTF_8);
            CinematicCatalog catalog = new CinematicCatalog(file.toFile());
            check(catalog.prologue().size() == 2, "Entradas vazias devem ser ignoradas.");
            check(catalog.prologue().get(0).video().equals(FIRST.video()), "Ordem numérica incorreta.");
            check(catalog.prologue().get(0).caption().equals("Árvores e raízes"), "UTF-8 incorreto.");
            check(catalog.beforeChapter(1).isEmpty(), "Capítulo sem configuração não tem vídeos.");
            check(catalog.beforeChapter(2).size() == 1 && catalog.beforeChapter(4).size() == 1, "Sequências misturadas.");
        } finally { Files.deleteIfExists(file); }
    }

    private static void manualInputMustNotComplete() throws Exception {
        AtomicInteger errors = new AtomicInteger();
        CinematicManager manager = createManager(errors, new AtomicInteger());
        AtomicInteger finished = new AtomicInteger();
        try {
            manager.start(List.of(FIRST), "INTEGRAL", finished::incrementAndGet);
            MediaPlayer playing = awaitVideo(manager, "cena1.mp4");
            fx(() -> {
                JFXPanel panel = (JFXPanel) manager.panel();
                for (KeyCode key : List.of(KeyCode.ENTER, KeyCode.SPACE, KeyCode.RIGHT)) {
                    Event.fireEvent(panel.getScene(), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
                }
                Event.fireEvent(panel.getScene().getRoot(), new MouseEvent(MouseEvent.MOUSE_CLICKED,
                    100, 100, 100, 100, MouseButton.PRIMARY, 1, false, false, false, false, false, false, false, false, false, false, null));
                return null;
            });
            SwingUtilities.invokeAndWait(() -> { });
            check(finished.get() == 0 && fx(() -> player(manager)) == playing,
                "Clique, Enter e Espaço não podem concluir ou avançar o vídeo.");
            check(errors.get() == 0, "Entrada durante reprodução não deve causar erros.");
        } finally { dispose(manager); }
    }

    private static void naturalPlayback() throws Exception {
        AtomicInteger errors = new AtomicInteger();
        CinematicManager manager = createManager(errors, new AtomicInteger());
        AtomicInteger finished = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        try {
            manager.start(List.of(FIRST, SECOND), "PRÓLOGO", () -> {
                check(SwingUtilities.isEventDispatchThread(), "Conclusão fora do Swing.");
                check(manager.panel().isVisible(), "O frame final deve permanecer até a próxima tela ser preparada.");
                finished.incrementAndGet();
                done.countDown();
            });
            MediaPlayer firstPlayer = awaitVideo(manager, "cena1.mp4");
            Runnable staleEnd = fx(firstPlayer::getOnEndOfMedia);
            check(fx(() -> firstPlayer.getMedia().getWidth()) == 1024, "Vídeo não foi decodificado.");
            MediaPlayer secondPlayer = awaitVideo(manager, "cena2.mp4");
            Runnable lastEnd = fx(secondPlayer::getOnEndOfMedia);
            fx(() -> { staleEnd.run(); return null; });
            check(fx(() -> player(manager)) == secondPlayer, "Callback antigo avançou a cena seguinte.");
            check(done.await(15, TimeUnit.SECONDS), "A sequência não terminou naturalmente.");
            fx(() -> { lastEnd.run(); lastEnd.run(); return null; });
            SwingUtilities.invokeAndWait(() -> { });
            check(finished.get() == 1 && errors.get() == 0, "A conclusão deve ocorrer uma única vez, sem erros.");
            check(fx(firstPlayer::getStatus) == MediaPlayer.Status.DISPOSED, "Decoder anterior não foi liberado.");
        } finally { dispose(manager); }
        System.out.println("OK: duas cenas reproduzidas integralmente; conclusão única e frame final preservado.");
    }

    private static void cancellationAndStaleEvents() throws Exception {
        AtomicInteger cancelled = new AtomicInteger();
        CinematicManager manager = createManager(new AtomicInteger(), cancelled);
        AtomicInteger finished = new AtomicInteger();
        try {
            manager.start(List.of(FIRST), "ANTIGA", finished::incrementAndGet);
            MediaPlayer first = awaitVideo(manager, "cena1.mp4");
            Runnable oldEnd = fx(first::getOnEndOfMedia);
            Runnable oldError = fx(first::getOnError);
            manager.cancel();
            manager.cancel();
            awaitState(manager, CinematicManager.PlaybackState.IDLE);
            fx(() -> null); // Fence the FX task before checking its queued Swing callback.
            SwingUtilities.invokeAndWait(() -> { });
            check(cancelled.get() == 1 && finished.get() == 0,
                "Cancelar não pode liberar a fase: cancelled=" + cancelled.get() + ", finished=" + finished.get());
            manager.cancel();
            fx(() -> null);
            SwingUtilities.invokeAndWait(() -> { });
            check(cancelled.get() == 1, "Cancelar deve ser idempotente.");
            manager.start(List.of(SECOND), "NOVA", finished::incrementAndGet);
            MediaPlayer next = awaitVideo(manager, "cena2.mp4");
            fx(() -> { oldEnd.run(); oldError.run(); return null; });
            check(finished.get() == 0 && fx(() -> player(manager)) == next, "Callback de sessão cancelada afetou a nova.");
        } finally { dispose(manager); }
    }

    private static void missingVideoRecovery() throws Exception {
        Path directory = Files.createTempDirectory("raizes-video-retry-");
        Path video = directory.resolve("recovered.mp4");
        AtomicInteger errors = new AtomicInteger();
        CinematicManager manager = createManager(errors, new AtomicInteger());
        AtomicInteger finished = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        try {
            manager.start(List.of(new CinematicScene(video.toString(), "")), "AUSENTE", () -> { finished.incrementAndGet(); done.countDown(); });
            awaitState(manager, CinematicManager.PlaybackState.FAILED);
            fx(() -> null);
            SwingUtilities.invokeAndWait(() -> { });
            check(finished.get() == 0 && errors.get() == 1, "Arquivo ausente não pode iniciar a fase.");
            Files.copy(Path.of(SECOND.video()), video);
            manager.retry();
            awaitVideo(manager, "recovered.mp4");
            check(done.await(10, TimeUnit.SECONDS) && finished.get() == 1, "Retry deve completar apenas após reproduzir o vídeo.");
        } finally { dispose(manager); Files.deleteIfExists(video); Files.deleteIfExists(directory); }
    }

    private static void corruptedVideoDoesNotComplete() throws Exception {
        Path invalid = Files.createTempFile("raizes-invalid-video-", ".mp4");
        Files.writeString(invalid, "Arquivo inválido para testar recuperação.");
        AtomicInteger cancelled = new AtomicInteger();
        CinematicManager manager = createManager(new AtomicInteger(), cancelled);
        AtomicInteger finished = new AtomicInteger();
        try {
            manager.start(List.of(new CinematicScene(invalid.toString(), ""), SECOND), "CORROMPIDO", finished::incrementAndGet);
            awaitState(manager, CinematicManager.PlaybackState.FAILED);
            check(finished.get() == 0 && fx(() -> player(manager)) == null,
                "Formato inválido não pode ser pulado para liberar a campanha.");
            snapshot(manager, "video-error.png");
            fx(() -> {
                JFXPanel panel = (JFXPanel) manager.panel();
                javafx.scene.layout.HBox buttons = (javafx.scene.layout.HBox) field(manager, "recovery");
                javafx.scene.control.Button back = (javafx.scene.control.Button) buttons.getChildren().get(1);
                back.requestFocus();
                check(panel.getScene().getFocusOwner() == back, "Botão Voltar não recebeu foco.");
                Event.fireEvent(back, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SPACE, false, false, false, false));
                Event.fireEvent(back, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.SPACE, false, false, false, false));
                return null;
            });
            fx(() -> null);
            SwingUtilities.invokeAndWait(() -> { });
            check(cancelled.get() == 1 && finished.get() == 0, "Teclado em Voltar deve cancelar, sem tentar novamente.");
        } finally { dispose(manager); Files.deleteIfExists(invalid); }
    }

    private static CinematicManager createManager(AtomicInteger errors, AtomicInteger cancelled) throws Exception {
        CinematicManager[] result = new CinematicManager[1];
        SwingUtilities.invokeAndWait(() -> result[0] = new CinematicManager(message -> {
            check(SwingUtilities.isEventDispatchThread(), "Aviso fora do Swing.");
            errors.incrementAndGet();
        }, cancelled::incrementAndGet));
        return result[0];
    }

    private static MediaPlayer awaitVideo(CinematicManager manager, String name) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            MediaPlayer current = fx(() -> {
                MediaPlayer candidate = player(manager);
                return candidate != null && candidate.getMedia().getSource().endsWith(name)
                    && candidate.getStatus() == MediaPlayer.Status.PLAYING ? candidate : null;
            });
            if (current != null) return current;
            Thread.sleep(25);
        }
        throw new AssertionError("O vídeo não iniciou: " + name + "; estado=" + manager.state());
    }

    private static void awaitState(CinematicManager manager, CinematicManager.PlaybackState expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (manager.state() == expected) return;
            Thread.sleep(25);
        }
        throw new AssertionError("Estado esperado: " + expected + "; atual: " + manager.state());
    }

    private static MediaPlayer player(CinematicManager manager) throws Exception {
        return (MediaPlayer) field(manager, "player");
    }

    private static Object field(CinematicManager manager, String name) throws Exception {
        Field field = CinematicManager.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(manager);
    }

    private static void snapshot(CinematicManager manager, String name) throws Exception {
        Path directory = Path.of("out/project-tests/artifacts");
        Files.createDirectories(directory);
        var image = fx(() -> ((JFXPanel) manager.panel()).getScene().snapshot(null));
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", directory.resolve(name).toFile());
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    private static void dispose(CinematicManager manager) throws Exception {
        manager.dispose();
        fx(() -> null);
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
