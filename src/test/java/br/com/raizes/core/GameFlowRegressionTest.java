package br.com.raizes.core;

import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.audio.CinematicManager;
import br.com.raizes.model.Question;
import br.com.raizes.model.Thing;
import br.com.raizes.persistence.SaveManager;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;

/** Exercises real game inputs and chapter transitions with isolated saves and no window. */
public final class GameFlowRegressionTest {
    public static void main(String[] args) {
        int result = 0;
        AtomicReference<Throwable> asynchronousError = new AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> asynchronousError.compareAndSet(null, error));
        try {
            campaignFlow();
            check(asynchronousError.get() == null, "Falha assíncrona: " + asynchronousError.get());
            System.out.println("OK: menu, prólogo, quatro capítulos, objetivos, quiz, renderização e saves isolados.");
        } catch (Throwable error) {
            result = 1;
            error.printStackTrace();
        } finally { Platform.exit(); }
        System.exit(result);
    }

    private static void campaignFlow() throws Exception {
        Path directory = Files.createTempDirectory("raizes-game-flow-");
        Path configuration = directory.resolve("cinematics.properties");
        Path saveDirectory = directory.resolve("saves");
        Files.writeString(configuration, """
            prologue.1.video=Videos/cena1.mp4
            chapter.1.1.video=Videos/cena2.mp4
            chapter.2.1.video=Videos/cena1.mp4
            """, StandardCharsets.UTF_8);
        SaveManager saves = new SaveManager(saveDirectory);
        Game game = edt(() -> {
            Game created = new Game(new CinematicCatalog(configuration.toFile()), saves);
            created.timer.stop();
            created.setSize(1280, 720);
            return created;
        });
        try {
            press(game, KeyEvent.VK_ENTER);
            expectScreen(game, GameState.SLOTS);
            press(game, KeyEvent.VK_ENTER);
            expectScreen(game, GameState.NAME);
            edt(() -> {
                for (char letter : "Teste".toCharArray()) {
                    KeyEvent event = new KeyEvent(game, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, letter);
                    for (KeyListener listener : game.getKeyListeners()) listener.keyTyped(event);
                }
                return null;
            });
            press(game, KeyEvent.VK_ENTER);
            press(game, KeyEvent.VK_ENTER);
            expectScreen(game, GameState.CHARACTER_SELECT);
            press(game, KeyEvent.VK_ENTER);
            awaitVideo(game, "cena1.mp4");
            assertSuspendedDuringVideo(game);
            check(saves.read(0).getProperty("name").equals("Teste"), "A campanha deve preservar o apelido.");
            awaitVideo(game, "cena2.mp4");
            awaitScreen(game, GameState.PHASE_INTRO);

            Properties checkpoint = null;
            for (int phase = 0; phase < 4; phase++) {
                check(edt(() -> game.currentPhase) == phase, "A campanha deve seguir a ordem das fases.");
                press(game, KeyEvent.VK_ENTER);
                expectScreen(game, GameState.WORLD);
                if (phase == 0) {
                    edt(() -> { game.x = 3030; invoke(game, "updateWorld", new Class<?>[] { double.class }, 0.016); return null; });
                    expectScreen(game, GameState.WORLD);
                }
                edt(() -> {
                    for (Thing item : game.coins) item.hit = true;
                    game.coinsCollected = game.coins.size();
                    for (Thing trap : game.traps) trap.hit = true;
                    for (Thing fire : game.fires) fire.hit = true;
                    game.x = 2970;
                    invoke(game, "updateWorld", new Class<?>[] { double.class }, 0.016);
                    game.x = 3030;
                    invoke(game, "updateWorld", new Class<?>[] { double.class }, 0.016);
                    return null;
                });
                expectScreen(game, GameState.DIALOGUE);
                for (int line = 0; line < 8 && edt(() -> game.screen) == GameState.DIALOGUE; line++) press(game, KeyEvent.VK_ENTER);
                expectScreen(game, GameState.BATTLE);
                if (phase == 0) {
                    quizControls(game);
                    renderScreens(game);
                }
                int answered = 0;
                while (edt(() -> game.screen) == GameState.BATTLE && answered++ < 5) {
                    if (answered == 5) edt(() -> {
                        game.held.add(KeyEvent.VK_D);
                        game.vy = 123;
                        game.camera = 1800;
                        return null;
                    });
                    int answer = edt(() -> game.quizQuestions.get(integerField(game, "quizIndex")).correctOption());
                    press(game, KeyEvent.VK_1 + answer);
                    press(game, KeyEvent.VK_ENTER);
                }
                check(answered == 5, "Cada fase deve exigir cinco respostas corretas.");
                if (phase < 3) {
                    check(edt(() -> game.vy == 0 && game.camera == 0 && game.held.isEmpty()),
                        "Movimento e câmera devem ser reiniciados ao trocar de fase.");
                    check(saves.read(0).getProperty("phase").equals(String.valueOf(phase + 1)),
                        "A mudança de fase deve ser salva antes da introdução.");
                    if (phase == 0) {
                        awaitVideo(game, "cena1.mp4");
                        check(Boolean.parseBoolean(saves.read(0).getProperty("pendingChapter")),
                            "Vídeo pendente precisa sobreviver a cancelamento ou fechamento.");
                    }
                    awaitScreen(game, GameState.PHASE_INTRO);
                    if (phase == 0) {
                        checkpoint = saves.read(0);
                        check(!Boolean.parseBoolean(checkpoint.getProperty("pendingChapter"))
                            && !Boolean.parseBoolean(checkpoint.getProperty("pendingPrologue")),
                            "Somente término natural pode limpar os vídeos pendentes.");
                    }
                }
            }
            expectScreen(game, GameState.END);
            check(Boolean.parseBoolean(saves.read(0).getProperty("campaignComplete")), "A conclusão deve ser salva.");

            // Invalid numeric values must not poison world coordinates or camera calculations.
            checkpoint.setProperty("x", "NaN");
            checkpoint.setProperty("y", "Infinity");
            check(saves.write(0, checkpoint), "O checkpoint de teste deve ser gravado.");
            verifySlotIsolation(saves);
            press(game, KeyEvent.VK_ENTER);
            openContinue(game);
            press(game, KeyEvent.VK_ENTER);
            expectScreen(game, GameState.PHASE_INTRO);
            check(edt(() -> game.currentPhase == 1 && Double.isFinite(game.x) && Double.isFinite(game.y)
                && Double.isFinite(game.camera)), "Continuar deve restaurar a fase e corrigir coordenadas inválidas.");

            // Continuing does not replay videos; explicitly restarting an unlocked chapter does.
            press(game, KeyEvent.VK_ESCAPE);
            openContinue(game);
            press(game, KeyEvent.VK_DOWN);
            press(game, KeyEvent.VK_ENTER);
            expectScreen(game, GameState.PHASE_SELECT);
            press(game, KeyEvent.VK_DOWN);
            press(game, KeyEvent.VK_DOWN);
            press(game, KeyEvent.VK_ENTER);
            expectScreen(game, GameState.PHASE_SELECT);
            press(game, KeyEvent.VK_UP);
            press(game, KeyEvent.VK_ENTER);
            expectScreen(game, GameState.SAVE_CONFIRM);
            press(game, KeyEvent.VK_ENTER);
            awaitVideo(game, "cena1.mp4");
            awaitScreen(game, GameState.PHASE_INTRO);
            check(edt(() -> game.x == 110 && game.y == 500 && game.lives == 3 && game.coinsCollected == 0),
                "Reiniciar uma fase deve restaurar seu estado inicial.");
        } finally {
            edt(() -> { game.timer.stop(); game.cinematicManager.dispose(); return null; });
            FutureTask<Void> released = new FutureTask<>(() -> null);
            Platform.runLater(released);
            released.get(5, TimeUnit.SECONDS);
            Files.deleteIfExists(saveDirectory.resolve("slot-1.properties"));
            Files.deleteIfExists(saveDirectory.resolve("slot-2.properties"));
            Files.deleteIfExists(saveDirectory);
            Files.deleteIfExists(configuration);
            Files.deleteIfExists(directory);
        }
    }

    private static void assertSuspendedDuringVideo(Game game) throws Exception {
        double[] before = edt(() -> new double[] { game.x, game.y, game.vy, game.elapsed, game.camera,
            game.enemies.get(0).x, game.phaseLoadStartedAt, game.playerAnimator.frameIndex() });
        press(game, KeyEvent.VK_D);
        press(game, KeyEvent.VK_SPACE);
        press(game, KeyEvent.VK_ENTER);
        edt(() -> { invoke(game, "updateWorld", new Class<?>[] { double.class }, 1.0); return null; });
        java.util.concurrent.CountDownLatch responsive = new java.util.concurrent.CountDownLatch(8);
        javax.swing.Timer heartbeat = edt(() -> {
            game.timer.start();
            javax.swing.Timer timer = new javax.swing.Timer(20, event -> responsive.countDown());
            timer.start();
            return timer;
        });
        check(responsive.await(2, TimeUnit.SECONDS), "Interface não respondeu durante o vídeo.");
        edt(() -> { heartbeat.stop(); game.timer.stop(); return null; });
        expectScreen(game, GameState.CINEMATIC);
        double[] after = edt(() -> new double[] { game.x, game.y, game.vy, game.elapsed, game.camera,
            game.enemies.get(0).x, game.phaseLoadStartedAt, game.playerAnimator.frameIndex() });
        check(java.util.Arrays.equals(before, after), "Física, inimigos, relógio ou animação avançaram durante o vídeo.");
    }

    private static void quizControls(Game game) throws Exception {
        press(game, KeyEvent.VK_LEFT);
        check(edt(() -> integerField(game, "battleChoice")) == 3, "Seta esquerda deve selecionar a última alternativa.");
        press(game, KeyEvent.VK_RIGHT);
        check(edt(() -> integerField(game, "battleChoice")) == 0, "Seta direita deve retornar à primeira alternativa.");
        press(game, KeyEvent.VK_DOWN);
        check(edt(() -> integerField(game, "battleChoice")) == 1, "Seta para baixo deve avançar a seleção.");
        press(game, KeyEvent.VK_UP);
        Question previous = edt(() -> game.quizQuestions.get(0));
        press(game, KeyEvent.VK_1 + (previous.correctOption() + 1) % 4);
        press(game, KeyEvent.VK_ENTER);
        check(edt(() -> integerField(game, "quizIndex") == 0 && game.quizQuestions.get(0) != previous),
            "Uma resposta errada deve reiniciar a rodada com uma primeira pergunta diferente.");
    }

    private static void verifySlotIsolation(SaveManager saves) {
        Properties first = saves.read(0);
        Properties second = new Properties();
        second.setProperty("name", "Outro slot");
        second.setProperty("phase", "0");
        check(saves.write(1, second) && saves.hasSave(1), "O segundo slot deve ser independente.");
        Properties copy = saves.read(0);
        copy.setProperty("name", "Alteração sem gravar");
        check(saves.read(0).equals(first), "Ler um save não deve expor o cache para alteração externa.");
        check(saves.delete(1) && !saves.exists(1) && saves.read(0).equals(first), "Excluir um slot deve preservar o outro.");
    }

    private static void renderScreens(Game game) throws Exception {
        Path artifacts = Path.of("out", "project-tests", "artifacts");
        Files.createDirectories(artifacts);
        edt(() -> {
            GameState previous = game.screen;
            int previousDialogue = game.dialogueIndex;
            game.dialogueIndex = 0;
            try {
                for (GameState screen : GameState.values()) {
                    game.screen = screen;
                    BufferedImage frame = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = frame.createGraphics();
                    try { game.paintComponent(graphics); }
                    finally { graphics.dispose(); }
                    if (screen == GameState.MENU || screen == GameState.WORLD || screen == GameState.BATTLE
                            || screen == GameState.SAVE_CONFIRM) {
                        ImageIO.write(frame, "png", artifacts.resolve(screen.name().toLowerCase() + ".png").toFile());
                    }
                }
            } finally { game.screen = previous; game.dialogueIndex = previousDialogue; }
            return null;
        });
        System.out.println("OK: todas as telas renderizadas; amostras em out/project-tests/artifacts.");
    }

    private static void openContinue(Game game) throws Exception {
        press(game, KeyEvent.VK_DOWN);
        press(game, KeyEvent.VK_ENTER);
        expectScreen(game, GameState.SLOTS);
        press(game, KeyEvent.VK_ENTER);
        expectScreen(game, GameState.SAVE_OPTIONS);
    }

    private static void press(Game game, int code) throws Exception {
        edt(() -> {
            for (KeyListener listener : game.getKeyListeners()) {
                listener.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED));
                listener.keyReleased(new KeyEvent(game, KeyEvent.KEY_RELEASED, 0, 0, code, KeyEvent.CHAR_UNDEFINED));
            }
            return null;
        });
    }

    private static void awaitVideo(Game game, String name) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            FutureTask<Boolean> check = new FutureTask<>(() -> {
                Field field = CinematicManager.class.getDeclaredField("player");
                field.setAccessible(true);
                MediaPlayer player = (MediaPlayer) field.get(game.cinematicManager);
                return player != null && player.getMedia().getSource().endsWith(name)
                    && player.getStatus() == MediaPlayer.Status.PLAYING;
            });
            Platform.runLater(check);
            if (check.get(5, TimeUnit.SECONDS)) return;
            Thread.sleep(25);
        }
        FutureTask<String> diagnostics = new FutureTask<>(() -> {
            Field field = CinematicManager.class.getDeclaredField("player");
            field.setAccessible(true);
            MediaPlayer player = (MediaPlayer) field.get(game.cinematicManager);
            return "screen=" + game.screen + ", playback=" + game.cinematicManager.state()
                + ", player=" + (player == null ? "null" : player.getStatus() + " "
                    + player.getMedia().getSource() + " at " + player.getCurrentTime());
        });
        Platform.runLater(diagnostics);
        throw new AssertionError("Vídeo de capítulo não iniciou: " + name + "; " + diagnostics.get(5, TimeUnit.SECONDS));
    }

    private static void awaitScreen(Game game, GameState screen) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            if (edt(() -> game.screen) == screen) return;
            Thread.sleep(25);
        }
        expectScreen(game, screen);
    }

    private static void expectScreen(Game game, GameState screen) throws Exception {
        check(edt(() -> game.screen) == screen, "Tela esperada: " + screen + "; atual: " + edt(() -> game.screen));
    }

    private static int integerField(Game game, String name) throws Exception {
        Field field = Game.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(game);
    }

    private static void invoke(Game game, String name, Class<?>[] parameters, Object... args) throws Exception {
        Method method = Game.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        method.invoke(game, args);
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeLater(task);
        return task.get(20, TimeUnit.SECONDS);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
