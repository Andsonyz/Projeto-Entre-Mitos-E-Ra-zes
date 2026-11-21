package br.com.raizes.core;

import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.persistence.SaveManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;
import javafx.application.Platform;

/** Guards the bounded-resolution artwork used repeatedly by the world renderer. */
public final class WorldRendererRegressionTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("raizes-world-renderer-");
        Game game = edt(() -> {
            Game created = new Game(new CinematicCatalog(), new SaveManager(directory));
            created.timer.stop();
            return created;
        });
        try {
            assertWithinBox(game.seedImage, 32, 34, "semente");
            assertWithinBox(game.trashImage, 66, 54, "lixo");
            assertWithinBox(game.trapImage, 60, 60, "armadilha");
            assertWithinBox(game.lakeImage, 158, 88, "lago");
            assertWithinBox(game.fireImage, 96, 82, "fogo");
            System.out.println("OK: arte de mundo limitada ao tamanho máximo de desenho.");
        } finally {
            edt(() -> { game.cinematicManager.dispose(); return null; });
            FutureTask<Void> released = new FutureTask<>(() -> null);
            Platform.runLater(released);
            released.get();
            Files.deleteIfExists(directory);
            Platform.exit();
        }
    }

    private static void assertWithinBox(java.awt.image.BufferedImage image, int width, int height, String label) {
        check(image != null, "Imagem ausente: " + label);
        check(image.getWidth() <= width && image.getHeight() <= height,
            label + " está em " + image.getWidth() + " × " + image.getHeight()
                + "; excede a caixa " + width + " × " + height + ".");
        boolean hasVisiblePixel = false;
        for (int y = 0; y < image.getHeight() && !hasVisiblePixel; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 24) { hasVisiblePixel = true; break; }
            }
        }
        check(hasVisiblePixel, "A arte redimensionada ficou vazia: " + label);
    }

    private static <T> T edt(java.util.concurrent.Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
