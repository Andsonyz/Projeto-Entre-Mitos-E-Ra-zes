package br.com.raizes.core;

import br.com.raizes.assets.AssetManager;
import br.com.raizes.audio.AmbientAudio;
import br.com.raizes.character.CharacterDefinition;
import br.com.raizes.character.SpriteAnimationSet;
import br.com.raizes.model.Phase;
import static br.com.raizes.config.GameConfig.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.IntConsumer;

/** Images decoded and prepared once, off the Swing thread during normal startup. */
public final class GameAssets {
    public static final String LOADING_IMAGE = "Image/Cenarios/Menu/Carregamento.png";
    private GameAssets() { }
    BufferedImage groundTile, menuBackground, platformImage, celebrationBackground;
    final BufferedImage[] guardianWorldImages = new BufferedImage[Phase.values().length];
    BufferedImage seedImage, trashImage, trapImage, lakeImage, fireImage;
    BufferedImage slotTitleImage, nameTitleArtwork;
    BufferedImage pauseTitleImage, pauseIndicatorImage, settingsTitleImage;
    final BufferedImage[] slotImages = new BufferedImage[2];
    final BufferedImage[] treeImages = new BufferedImage[3];
    final BufferedImage[] guardianImages = new BufferedImage[Phase.values().length];
    final BufferedImage[] backgrounds = new BufferedImage[Phase.values().length];
    final BufferedImage[] menuImages = new BufferedImage[5];
    final Rectangle[] menuOpaqueBounds = new Rectangle[5];
    private void loadImages(java.util.function.IntConsumer progress) {
        for (Phase phase : Phase.values()) {
            guardianImages[phase.ordinal()] = fitImageToBox(image(phase.portrait), 350, 400);
            guardianWorldImages[phase.ordinal()] = fitImageToBox(guardianImages[phase.ordinal()], 100, 140);
        }
        for (Phase phase : Phase.values()) {
            BufferedImage source = image(phase.background);
            double scale = Math.max(WORLD_W / (double) source.getWidth(), (H - 78) / (double) source.getHeight());
            backgrounds[phase.ordinal()] = resizeImage(source, (int) Math.ceil(source.getWidth() * scale),
                (int) Math.ceil(source.getHeight() * scale));
            progress.accept(15 + phase.ordinal() * 10);
        }
        BufferedImage finalSource = image("Image/Cenarios/BackGrounds/BackgroundFinal.png");
        double finalScale = Math.max(WORLD_W / (double) finalSource.getWidth(), (H - 78) / (double) finalSource.getHeight());
        celebrationBackground = resizeImage(finalSource, (int)Math.ceil(finalSource.getWidth()*finalScale), (int)Math.ceil(finalSource.getHeight()*finalScale));
        treeImages[0] = loadTree("Image/Cenarios/Elementos/arvore01.png");
        treeImages[1] = loadTree("Image/Cenarios/Elementos/arvore02.png");
        treeImages[2] = loadTree("Image/Cenarios/Elementos/arvore03.png");
        seedImage = fitImageToBox(cropOpaqueImage(image("Image/Cenarios/Elementos/semente.png")), 32, 34);
        trashImage = fitImageToBox(cropOpaqueImage(image("Image/Cenarios/Elementos/lixo.png")), 66, 54);
        trapImage = fitImageToBox(cropOpaqueImage(image("Image/Cenarios/Elementos/armadilha.png")), 60, 60);
        lakeImage = fitImageToBox(cropOpaqueImage(image("Image/Cenarios/Elementos/lago.png")), 158, 88);
        fireImage = fitImageToBox(cropOpaqueImage(image("Image/Cenarios/Elementos/fogo.png")), 96, 82);
        BufferedImage groundSource = image("Image/Cenarios/Elementos/chao.png");
        if (groundSource != null) {
            int renderHeight = 120 + GROUND_OVERDRAW;
            groundTile = resizeImage(groundSource, Math.round(renderHeight * groundSource.getWidth() / (float) groundSource.getHeight()), renderHeight);
        }
        BufferedImage platformSource = image("Image/Cenarios/Elementos/plataforma.png");
        if (platformSource != null) platformImage = resizeImage(platformSource, 800, 400);
        menuBackground = fitImageToBox(image("Image/Cenarios/Menu/MENU.png"), W, H + 40);
        progress.accept(65);
        slotTitleImage = cropOpaqueImage(image("Image/Cenarios/Menu/Slots/Escolha-Slot.png"));
        BufferedImage nameSource = image("Image/Cenarios/Menu/Slots/Como-chama.jpg");
        nameTitleArtwork = fitImageToBox(cropRegion(nameSource, 0, 640, nameSource.getWidth(), 290), 1200, 160);
        slotImages[0] = cropOpaqueImage(image("Image/Cenarios/Menu/Slots/Slot-1.png"));
        slotImages[1] = cropOpaqueImage(image("Image/Cenarios/Menu/Slots/Slot-2.png"));
        pauseTitleImage = cropOpaqueImage(image("Image/Cenarios/Menu/Pausa/pausa.png"));
        pauseIndicatorImage = removeDarkBackground(image("Image/Cenarios/Menu/Pausa/indicador.jpg"));
        settingsTitleImage = cropOpaqueImage(image("Image/Cenarios/Menu/Configuracoes/configuracao.png"));
        menuImages[0] = image("Image/Cenarios/Menu/NovoGame.png");
        menuImages[1] = image("Image/Cenarios/Menu/Continuar.png");
        menuImages[2] = image("Image/Cenarios/Menu/Configuracoes.png");
        menuImages[3] = image("Image/Cenarios/Menu/Interrogacao.png");
        menuImages[4] = image("Image/Cenarios/Menu/Sair.png");
        for (int i = 0; i < menuImages.length; i++) {
            menuImages[i] = fitImageToBox(cropOpaqueImage(menuImages[i]), 260, 64);
            menuOpaqueBounds[i] = opaqueBounds(menuImages[i]);
        }
        progress.accept(85);
    }

    static BufferedImage resizeImage(BufferedImage source, int width, int height) {
        if (source == null || width < 1 || height < 1) return null;
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    static BufferedImage fitImageToBox(BufferedImage source, int maxWidth, int maxHeight) {
        if (source == null) return null;
        double scale = Math.min(maxWidth / (double) source.getWidth(), maxHeight / (double) source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        return resizeImage(source, width, height);
    }

    static BufferedImage loadTree(String path) {
        if (AssetManager.optionalFile(path) == null) return null;
        BufferedImage source = image(path);
        if (source == null) return null;
        int height = 360;
        int width = Math.round(height * source.getWidth() / (float) source.getHeight());
        return resizeImage(source, width, height);
    }

    static BufferedImage image(String path) {
        BufferedImage result = AssetManager.image(path);
        if (result == null) throw new IllegalStateException("Imagem ausente ou inválida: " + path);
        return result;
    }

    static BufferedImage cropRegion(BufferedImage image, int x, int y, int width, int height) {
        if (image == null || x < 0 || y < 0 || width <= 0 || height <= 0
            || x + width > image.getWidth() || y + height > image.getHeight()) return null;
        return image.getSubimage(x, y, width, height);
    }

    static Rectangle opaqueBounds(BufferedImage image) {
        if (image == null) return null;
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 24) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < minX ? new Rectangle() : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    static BufferedImage removeDarkBackground(BufferedImage source) {
        if (source == null) return null;
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage transparent = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = source.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int brightness = Math.max((pixel >>> 16) & 0xff, Math.max((pixel >>> 8) & 0xff, pixel & 0xff));
            int alpha = brightness <= 38 ? 0 : brightness >= 82 ? 255 : (brightness - 38) * 255 / 44;
            pixels[i] = (alpha << 24) | (pixel & 0x00ffffff);
        }
        transparent.setRGB(0, 0, width, height, pixels, 0, width);
        return cropOpaqueImage(transparent);
    }

    static BufferedImage cropOpaqueImage(BufferedImage image) {
        if (image == null) return null;
        Rectangle content = opaqueBounds(image);
        if (content == null || content.isEmpty()) return image;
        return image.getSubimage(content.x, content.y, content.width, content.height);
    }
    public static GameAssets load(IntConsumer progress) {
        GameAssets assets = new GameAssets();
        progress.accept(0);
        assets.loadImages(value -> {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            progress.accept(value);
        });
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
        SpriteAnimationSet.load(CharacterDefinition.MALE);
        progress.accept(95);
        AmbientAudio.validateAssets();
        progress.accept(100);
        return assets;
    }
}
