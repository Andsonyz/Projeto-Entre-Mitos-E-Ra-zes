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
import javafx.scene.media.MediaPlayer;
import javax.swing.SwingUtilities;

/** Standalone integration tests: uses the real JavaFX decoder without opening a window. */
public final class CinematicRegressionTest {
    private static final CinematicScene FIRST = new CinematicScene("Videos/cena1.mp4", "Primeira cena");
    private static final CinematicScene SECOND = new CinematicScene("Videos/cena2.mp4", "Segunda cena");

    public static void main(String[] args) {
        int result = 0;
        try {
            catalogConfiguration();
            naturalPlayback();
            staleEventsAndSkipping();
            replacementAndEmptyPlaylist();
            invalidAndMissingVideos();
            System.out.println("OK: catálogo, reprodução completa, avanço, eventos antigos, substituição e falhas.");
        } catch (Throwable error) {
            result = 1;
            error.printStackTrace();
        } finally {
            Platform.exit();
        }
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
            check(catalog.prologue().get(0).video().equals(FIRST.video()), "A ordem deve ser numérica.");
            check(catalog.prologue().get(0).caption().equals("Árvores e raízes"), "Legendas devem preservar UTF-8.");
            check(catalog.beforeChapter(1).isEmpty(), "Capítulos sem vídeos devem continuar normalmente.");
            check(catalog.beforeChapter(2).size() == 1 && catalog.beforeChapter(4).size() == 1,
                "Cada capítulo deve ter sua própria sequência.");
            check(new CinematicCatalog((java.io.File) null).prologue().isEmpty(), "Configuração ausente deve ser opcional.");
        } finally { Files.deleteIfExists(file); }
    }

    private static void naturalPlayback() throws Exception {
        AtomicInteger errors = new AtomicInteger();
        CinematicManager manager = createManager(errors);
        AtomicInteger finished = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        try {
            manager.start(List.of(FIRST, SECOND), "PRÓLOGO", completion(finished, done));
            MediaPlayer firstPlayer = awaitVideo(manager, "cena1.mp4");
            check(fx(() -> firstPlayer.getMedia().getWidth()) == 1024, "O vídeo deve ser decodificado.");
            awaitVideo(manager, "cena2.mp4");
            check(done.await(15, TimeUnit.SECONDS), "A sequência deve terminar automaticamente.");
            SwingUtilities.invokeAndWait(() -> { });
            check(errors.get() == 0 && finished.get() == 1, "As duas cenas devem terminar sem erros.");
            check(fx(firstPlayer::getStatus) == MediaPlayer.Status.DISPOSED, "O primeiro decoder deve ser liberado.");
        } finally { dispose(manager); }
        System.out.println("OK: cena1.mp4 e cena2.mp4 reproduzidas até o fim.");
    }

    private static void staleEventsAndSkipping() throws Exception {
        AtomicInteger errors = new AtomicInteger();
        CinematicManager manager = createManager(errors);
        AtomicInteger finished = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        try {
            manager.start(List.of(FIRST, SECOND), "AVANÇO", completion(finished, done));
            MediaPlayer firstPlayer = awaitVideo(manager, "cena1.mp4");
            Runnable oldEnd = fx(firstPlayer::getOnEndOfMedia);
            Runnable oldError = fx(firstPlayer::getOnError);
            manager.advance();
            MediaPlayer secondPlayer = awaitVideo(manager, "cena2.mp4");
            fx(() -> { oldEnd.run(); oldError.run(); return null; });
            check(fx(() -> player(manager)) == secondPlayer, "Eventos antigos não podem pular a próxima cena.");
            check(errors.get() == 0, "Erros de um decoder antigo devem ser ignorados.");
            manager.skip();
            manager.skip();
            check(done.await(5, TimeUnit.SECONDS), "Pular deve retornar ao jogo.");
            fx(() -> null);
            SwingUtilities.invokeAndWait(() -> { });
            check(finished.get() == 1, "A conclusão não pode ser duplicada.");
            check(fx(secondPlayer::getStatus) == MediaPlayer.Status.DISPOSED, "Pular deve liberar o decoder.");
        } finally { dispose(manager); }
    }

    private static void replacementAndEmptyPlaylist() throws Exception {
        CinematicManager manager = createManager(new AtomicInteger());
        AtomicInteger oldFinished = new AtomicInteger();
        AtomicInteger newFinished = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        try {
            manager.start(List.of(FIRST), "ANTIGA", oldFinished::incrementAndGet);
            MediaPlayer oldPlayer = awaitVideo(manager, "cena1.mp4");
            manager.start(List.of(SECOND), "NOVA", completion(newFinished, done));
            awaitVideo(manager, "cena2.mp4");
            check(fx(oldPlayer::getStatus) == MediaPlayer.Status.DISPOSED, "Substituir deve liberar o player anterior.");
            manager.skip();
            check(done.await(5, TimeUnit.SECONDS), "A nova sequência deve concluir.");
            check(oldFinished.get() == 0 && newFinished.get() == 1, "A sequência substituída não pode retornar ao capítulo antigo.");
            CountDownLatch emptyDone = new CountDownLatch(1);
            manager.start(List.of(), "VAZIA", emptyDone::countDown);
            check(emptyDone.await(5, TimeUnit.SECONDS), "Uma sequência vazia deve continuar imediatamente.");
        } finally { dispose(manager); }
    }

    private static void invalidAndMissingVideos() throws Exception {
        System.out.println("Teste de recuperação: os dois avisos de arquivo abaixo são esperados.");
        Path invalid = Files.createTempFile("raizes-invalid-video-", ".mp4");
        Files.writeString(invalid, "Arquivo inválido para testar recuperação.");
        AtomicInteger errors = new AtomicInteger();
        CinematicManager manager = createManager(errors);
        AtomicInteger finished = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);
        try {
            manager.start(List.of(new CinematicScene("Videos/arquivo-inexistente-test.mp4", ""),
                new CinematicScene(invalid.toString(), ""), SECOND), "RECUPERAÇÃO", completion(finished, done));
            awaitVideo(manager, "cena2.mp4");
            check(done.await(10, TimeUnit.SECONDS), "Uma falha não pode travar a campanha.");
            SwingUtilities.invokeAndWait(() -> { });
            check(errors.get() == 2, "Cada cena com problema deve gerar um único aviso.");
            check(finished.get() == 1, "A sequência deve concluir uma única vez após as falhas.");
        } finally { dispose(manager); Files.deleteIfExists(invalid); }
    }

    private static CinematicManager createManager(AtomicInteger errors) throws Exception {
        CinematicManager[] manager = new CinematicManager[1];
        SwingUtilities.invokeAndWait(() -> manager[0] = new CinematicManager(message -> {
            check(SwingUtilities.isEventDispatchThread(), "Avisos devem chegar à thread do Swing.");
            errors.incrementAndGet();
        }));
        return manager[0];
    }

    private static Runnable completion(AtomicInteger count, CountDownLatch done) {
        return () -> {
            check(SwingUtilities.isEventDispatchThread(), "A conclusão deve chegar à thread do Swing.");
            count.incrementAndGet();
            done.countDown();
        };
    }

    private static MediaPlayer awaitVideo(CinematicManager manager, String name) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (System.nanoTime() < deadline) {
            MediaPlayer current = fx(() -> {
                MediaPlayer candidate = player(manager);
                return candidate != null && candidate.getMedia().getSource().endsWith(name)
                    && candidate.getStatus() == MediaPlayer.Status.PLAYING ? candidate : null;
            });
            if (current != null) return current;
            Thread.sleep(25);
        }
        throw new AssertionError("O vídeo não iniciou: " + name);
    }

    private static MediaPlayer player(CinematicManager manager) throws Exception {
        Field field = CinematicManager.class.getDeclaredField("player");
        field.setAccessible(true);
        return (MediaPlayer) field.get(manager);
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
