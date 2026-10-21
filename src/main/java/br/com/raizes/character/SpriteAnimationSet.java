package br.com.raizes.character;

import br.com.raizes.assets.AssetManager;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cuts the inspected irregular sheets once and gives every pose a common foot anchor. */
public final class SpriteAnimationSet {
    private static final int ALPHA_THRESHOLD = 16;
    private static final double REFERENCE_CELL_WIDTH = 256;
    private static final int MARGIN = 4;
    private static final Map<CharacterDefinition, SpriteAnimationSet> CACHE = new EnumMap<>(CharacterDefinition.class);
    private record Band(int start, int end) { }
    private record Cut(BufferedImage image, int x, int y, int width, int height,
                       double anchorX, double baseline, double scale) { }

    private final Map<AnimationState, List<BufferedImage>> frames = new EnumMap<>(AnimationState.class);
    private final CharacterDefinition character;
    private final int width, height;

    public static synchronized SpriteAnimationSet load(CharacterDefinition character) {
        if (!character.selectable()) throw new IllegalArgumentException("Personagem indisponível: " + character.label());
        return CACHE.computeIfAbsent(character, SpriteAnimationSet::new);
    }

    private SpriteAnimationSet(CharacterDefinition character) {
        this.character = character;
        Map<CharacterDefinition.Sheet, List<Cut>> sheets = new HashMap<>();
        for (CharacterDefinition.Animation animation : character.animations().values()) {
            sheets.computeIfAbsent(animation.sheet(), SpriteAnimationSet::cutSheet);
        }
        double halfWidth = 0, aboveFeet = 0;
        for (List<Cut> cuts : sheets.values()) for (Cut cut : cuts) {
            aboveFeet = Math.max(aboveFeet, cut.baseline * cut.scale);
            for (int y = cut.y; y < cut.y + cut.height; y++) for (int x = cut.x; x < cut.x + cut.width; x++) {
                if ((cut.image.getRGB(x, y) >>> 24) > ALPHA_THRESHOLD) {
                    halfWidth = Math.max(halfWidth, Math.abs(x - cut.x - cut.anchorX) * cut.scale);
                }
            }
        }
        width = (int) Math.ceil(halfWidth * 2) + MARGIN * 2;
        height = (int) Math.ceil(aboveFeet) + MARGIN * 2;
        Map<CharacterDefinition.Sheet, List<BufferedImage>> normalized = new HashMap<>();
        sheets.forEach((sheet, cuts) -> normalized.put(sheet, cuts.stream().map(this::normalize).toList()));
        character.animations().forEach((state, animation) -> {
            List<BufferedImage> sheet = normalized.get(animation.sheet());
            frames.put(state, List.copyOf(sheet.subList(animation.firstFrame(), animation.firstFrame() + animation.frameCount())));
        });
    }

    private static List<Cut> cutSheet(CharacterDefinition.Sheet spec) {
        BufferedImage sheet = AssetManager.image(spec.path());
        if (sheet == null) throw new IllegalStateException("Folha de sprites ausente: " + spec.path());
        Cut[] result = new Cut[spec.columns() * spec.rows()];
        double nominalWidth = sheet.getWidth() / (double) spec.columns();
        for (int column = 0; column < spec.columns(); column++) {
            int x0 = column * sheet.getWidth() / spec.columns();
            int x1 = (column + 1) * sheet.getWidth() / spec.columns();
            List<Band> bands = new ArrayList<>();
            int start = -1;
            for (int y = 0; y <= sheet.getHeight(); y++) {
                boolean occupied = false;
                if (y < sheet.getHeight()) for (int x = x0; x < x1; x++) {
                    if ((sheet.getRGB(x, y) >>> 24) > ALPHA_THRESHOLD) { occupied = true; break; }
                }
                if (occupied && start < 0) start = y;
                else if (!occupied && start >= 0) { bands.add(new Band(start, y)); start = -1; }
            }
            if (bands.size() != spec.rows()) throw new IllegalArgumentException(
                "Recorte ambíguo em " + spec.path() + ", coluna " + column + ": " + bands.size() + " faixas.");
            for (int row = 0; row < spec.rows(); row++) {
                Band band = bands.get(row);
                int top = Math.max(0, band.start - 2);
                int bottom = Math.min(sheet.getHeight(), band.end + 2);
                result[row * spec.columns() + column] = new Cut(sheet, x0, top, x1 - x0, bottom - top,
                    (column + 0.5) * nominalWidth - x0, band.end - top, REFERENCE_CELL_WIDTH / nominalWidth);
            }
        }
        return List.of(result);
    }

    private BufferedImage normalize(Cut cut) {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.translate(anchorX(), anchorY());
            graphics.scale(cut.scale, cut.scale);
            graphics.translate(-cut.anchorX, -cut.baseline);
            graphics.drawImage(cut.image, 0, 0, cut.width, cut.height,
                cut.x, cut.y, cut.x + cut.width, cut.y + cut.height, null);
        } finally { graphics.dispose(); }
        return canvas;
    }

    public CharacterDefinition character() { return character; }
    public int width() { return width; }
    public int height() { return height; }
    public double anchorX() { return width / 2.0; }
    public double anchorY() { return height - MARGIN; }
    public int frameCount(AnimationState state) { return frames.get(state).size(); }
    public BufferedImage frame(AnimationState state, int index) { return frames.get(state).get(index); }
}
