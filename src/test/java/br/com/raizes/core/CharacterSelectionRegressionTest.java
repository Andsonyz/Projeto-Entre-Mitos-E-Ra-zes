package br.com.raizes.core;

import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.persistence.SaveManager;
import br.com.raizes.character.CharacterDefinition;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Properties;
import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javax.swing.SwingUtilities;

public final class CharacterSelectionRegressionTest {
    public static void main(String[] args) {
        int result = 0;
        try { selectionBeforeVideo(); pendingVideoResume(); System.out.println("OK: seleção, bloqueio por teclado/mouse/API, saves antigos e vídeos pendentes."); }
        catch (Throwable error) { result = 1; error.printStackTrace(); }
        finally { Platform.exit(); }
        System.exit(result);
    }

    private static void selectionBeforeVideo() throws Exception {
        Path directory = Files.createTempDirectory("raizes-selection-");
        SaveManager saves = new SaveManager(directory);
        Game game = edt(() -> {
            Game created = new Game(new CinematicCatalog((java.io.File) null), saves);
            created.timer.stop();
            created.setSize(1280, 720);
            return created;
        });
        try {
            press(game, KeyEvent.VK_ENTER);
            press(game, KeyEvent.VK_ENTER);
            edt(() -> {
                for (char letter : "Aventureiro".toCharArray()) for (KeyListener listener : game.getKeyListeners())
                    listener.keyTyped(new KeyEvent(game, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, letter));
                return null;
            });
            press(game, KeyEvent.VK_ENTER);
            press(game, KeyEvent.VK_ENTER);
            check(edt(() -> game.screen.name()).equals("CHARACTER_SELECT"),
                "Confirmar o nome deve abrir a seleção de personagem antes de iniciar vídeo ou fase.");
            check(game.character() == CharacterDefinition.MALE, "Masculino deve ser o padrão.");
            screenshot(game, "character-male.png");
            press(game, KeyEvent.VK_RIGHT);
            press(game, KeyEvent.VK_ENTER);
            check(edt(() -> game.screen == GameState.CHARACTER_SELECT) && !saves.exists(0), "Teclado confirmou o feminino.");
            click(game, 840, 360);
            click(game, 640, 575);
            check(edt(() -> game.screen == GameState.CHARACTER_SELECT) && !saves.exists(0), "Mouse confirmou o feminino.");
            check(!edt(() -> game.confirmCharacter(CharacterDefinition.FEMALE)), "API interna confirmou o feminino.");
            screenshot(game, "character-female-locked.png");
            press(game, KeyEvent.VK_ESCAPE);
            check(edt(() -> game.screen) == GameState.NAME, "Voltar deve retornar ao nome.");
            press(game, KeyEvent.VK_ENTER);
            press(game, KeyEvent.VK_ENTER);
            check(edt(() -> game.screen) == GameState.PHASE_INTRO, "Masculino não foi confirmado.");
            check(saves.read(0).getProperty("character").equals("male")
                && saves.read(0).getProperty("name").equals("Aventureiro"), "Escolha ou nome não foram preservados.");
            Properties legacy = new Properties();
            legacy.setProperty("name", "Save antigo");
            legacy.setProperty("phase", "0");
            legacy.setProperty("coins", "3");
            for (String id : new String[] { null, "female", "unknown" }) {
                if (id == null) legacy.remove("character"); else legacy.setProperty("character", id);
                check(saves.write(0, legacy), "Não gravou fixture legada.");
                edt(() -> {
                    java.lang.reflect.Method load = Game.class.getDeclaredMethod("loadSave", int.class);
                    load.setAccessible(true);
                    load.invoke(game, 0);
                    return null;
                });
                check(game.character() == CharacterDefinition.MALE && game.coinsCollected == 3,
                    "Save legado ou personagem indisponível não adotou o masculino.");
            }
        } finally {
            edt(() -> { game.cinematicManager.dispose(); return null; });
            Files.deleteIfExists(directory.resolve("slot-1.properties"));
            Files.deleteIfExists(directory);
        }
    }

    private static void pendingVideoResume() throws Exception {
        Path directory = Files.createTempDirectory("raizes-pending-video-");
        Path configuration = directory.resolve("cinematics.properties");
        Files.writeString(configuration, "prologue.1.video=missing-prologue.mp4\nchapter.1.1.video=missing-chapter.mp4\n");
        SaveManager saves = new SaveManager(directory.resolve("saves"));
        Game game = edt(() -> {
            Game created = new Game(new CinematicCatalog(configuration.toFile()), saves);
            created.timer.stop();
            return created;
        });
        try {
            edt(() -> { game.screen = GameState.CHARACTER_SELECT; game.confirmCharacter(CharacterDefinition.MALE); return null; });
            press(game, KeyEvent.VK_ESCAPE);
            edt(() -> { invokeLoad(game); return null; });
            check(edt(() -> game.screen) == GameState.CINEMATIC,
                "Continue não pode liberar uma fase com prólogo cancelado ou com falha.");
            Properties pending = saves.read(0);
            check(Boolean.parseBoolean(pending.getProperty("pendingPrologue")), "Prólogo pendente não foi salvo.");
            press(game, KeyEvent.VK_ESCAPE);
            edt(() -> {
                java.lang.reflect.Method restart = Game.class.getDeclaredMethod("startSavedPhase", int.class);
                restart.setAccessible(true);
                restart.invoke(game, 0);
                return null;
            });
            check(Boolean.parseBoolean(saves.read(0).getProperty("pendingPrologue")),
                "Reiniciar capítulo não pode dispensar prólogo ainda pendente.");
            pending.setProperty("pendingPrologue", "false");
            pending.setProperty("pendingChapter", "true");
            saves.write(0, pending);
            press(game, KeyEvent.VK_ESCAPE);
            edt(() -> { invokeLoad(game); return null; });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (game.cinematicManager.state() != br.com.raizes.audio.CinematicManager.PlaybackState.FAILED
                    && System.nanoTime() < deadline) Thread.sleep(20);
            check(game.cinematicManager.state() == br.com.raizes.audio.CinematicManager.PlaybackState.FAILED,
                "Fixture ausente deveria oferecer recuperação.");
            press(game, KeyEvent.VK_ESCAPE);
            edt(() -> { invokeLoad(game); return null; });
            check(edt(() -> game.screen) == GameState.CINEMATIC,
                "Continue não pode liberar capítulo com vídeo pendente.");
        } finally {
            edt(() -> { game.cinematicManager.dispose(); return null; });
            Files.deleteIfExists(directory.resolve("saves/slot-1.properties"));
            Files.deleteIfExists(directory.resolve("saves"));
            Files.deleteIfExists(configuration);
            Files.deleteIfExists(directory);
        }
    }

    private static void invokeLoad(Game game) throws Exception {
        java.lang.reflect.Method method = Game.class.getDeclaredMethod("loadSave", int.class);
        method.setAccessible(true);
        method.invoke(game, 0);
    }

    private static void click(Game game, int x, int y) throws Exception {
        edt(() -> {
            for (MouseListener listener : game.getMouseListeners()) listener.mouseClicked(new MouseEvent(game,
                MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1));
            return null;
        });
    }

    private static void screenshot(Game game, String name) throws Exception {
        Path artifacts = Path.of("out/project-tests/artifacts");
        Files.createDirectories(artifacts);
        edt(() -> {
            BufferedImage image = new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try { game.paintComponent(graphics); } finally { graphics.dispose(); }
            ImageIO.write(image,"png",artifacts.resolve(name).toFile());
            return null;
        });
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

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeLater(task);
        return task.get(20, TimeUnit.SECONDS);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
