package br.com.raizes.audio;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import javax.swing.SwingUtilities;

/** Repeated real decoding catches native buffer leaks that a Java heap check misses. */
public final class CinematicResourceRegressionTest {
    private static final long MIB = 1024 * 1024;

    public static void main(String[] args) {
        int result = 0;
        CinematicManager manager = null;
        try {
            String video = "Videos/Cenas/CenaFase1.mp4";
            check(Files.isRegularFile(Path.of(video)), "Vídeo de regressão ausente: " + video);
            AtomicInteger errors = new AtomicInteger();
            AtomicInteger completed = new AtomicInteger();
            AtomicInteger cancelled = new AtomicInteger();
            manager = edt(() -> new CinematicManager(message -> {
                errors.incrementAndGet();
                System.err.println(message);
            }, cancelled::incrementAndGet));
            CinematicManager current = manager;
            long firstCommit = 0;
            for (int cycle = 0; cycle < 3; cycle++) {
                current.start(List.of(new CinematicScene(video, "")), "RECURSOS", completed::incrementAndGet);
                MediaPlayer playing = awaitPlaying(current);
                // This wait is on the test's main thread; UI threads continue decoding normally.
                TimeUnit.SECONDS.sleep(8);
                check(fx(playing::getStatus) == MediaPlayer.Status.PLAYING,
                    "Reprodução interrompida antes do cancelamento.");
                current.cancel();
                fx(() -> null);
                edt(() -> null);
                check(fx(playing::getStatus) == MediaPlayer.Status.DISPOSED, "Player anterior não foi descartado.");
                check(fx(() -> player(current)) == null, "Gerenciador reteve player após cancelamento.");
                check(current.state() == CinematicManager.PlaybackState.IDLE, "Estado não retornou a IDLE.");
                check(cancelled.get() == cycle + 1 && completed.get() == 0 && errors.get() == 0,
                    "Cancelar não pode concluir vídeo ou produzir callbacks duplicados.");
                TimeUnit.SECONDS.sleep(1);
                long committed = committedMemory();
                System.out.printf("Ciclo %d: player=DISPOSED, memória comprometida=%.1f MiB%n", cycle + 1,
                    committed / (double) MIB);
                if (cycle == 0) firstCommit = committed;
                // On Windows this metric is the process commit charge, including native media buffers.
                // The first completed cycle warms up the decoder/CSS. Leave room for JVM/driver variation.
                if (System.getProperty("os.name", "").startsWith("Windows") && committed >= 0 && firstCommit >= 0) {
                    check(committed - firstCommit <= 256 * MIB,
                        "Memória cresceu mais de 256 MiB após aquecimento e descarte dos players. "
                            + "JavaFX=" + System.getProperty("javafx.runtime.version")
                            + "; crescimento=" + (committed - firstCommit) / MIB + " MiB. "
                            + "Verifique a correção nativa JDK-8389515 do OpenJFX.");
                }
            }
            System.out.println("OK: três reentradas de vídeo, descarte e memória nativa limitada após aquecimento.");
        } catch (Throwable error) {
            result = 1;
            error.printStackTrace();
        } finally {
            if (manager != null) manager.dispose();
            Platform.exit();
        }
        System.exit(result);
    }

    private static long committedMemory() {
        var bean = ManagementFactory.getOperatingSystemMXBean();
        return bean instanceof com.sun.management.OperatingSystemMXBean operatingSystem
            ? operatingSystem.getCommittedVirtualMemorySize() : -1;
    }

    private static MediaPlayer awaitPlaying(CinematicManager manager) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            MediaPlayer candidate = fx(() -> {
                MediaPlayer value = player(manager);
                return value != null && value.getStatus() == MediaPlayer.Status.PLAYING ? value : null;
            });
            if (candidate != null) return candidate;
            check(manager.state() != CinematicManager.PlaybackState.FAILED, "Falha ao preparar vídeo de regressão.");
            TimeUnit.MILLISECONDS.sleep(25);
        }
        throw new AssertionError("Vídeo não iniciou em 20 segundos: " + manager.state());
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

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
