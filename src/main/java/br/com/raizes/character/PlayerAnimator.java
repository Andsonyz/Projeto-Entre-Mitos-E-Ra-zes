package br.com.raizes.character;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Objects;

/** Animation clock and drawing are independent of physical position and collision dimensions. */
public final class PlayerAnimator {
    private final SpriteAnimationSet sprites;
    private AnimationState state = AnimationState.IDLE;
    private double elapsed;

    public PlayerAnimator(CharacterDefinition character) { sprites = SpriteAnimationSet.load(character); }
    public void reset() { state = AnimationState.IDLE; elapsed = 0; }

    public void update(AnimationState next, double seconds) {
        Objects.requireNonNull(next);
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Tempo de animação inválido.");
        if (next != state) { state = next; elapsed = 0; }
        CharacterDefinition.Animation animation = sprites.character().animations().get(state);
        double duration = animation.frameCount() * animation.secondsPerFrame();
        elapsed = animation.loop() ? (elapsed + seconds) % duration : Math.min(duration, elapsed + seconds);
    }

    public AnimationState state() { return state; }
    public int frameIndex() {
        CharacterDefinition.Animation animation = sprites.character().animations().get(state);
        int index = (int) Math.floor((elapsed + 1e-9) / animation.secondsPerFrame());
        return animation.loop() ? index % animation.frameCount() : Math.min(index, animation.frameCount() - 1);
    }
    public BufferedImage image() { return sprites.frame(state, frameIndex()); }
    public SpriteAnimationSet sprites() { return sprites; }

    public void draw(Graphics2D graphics, double feetX, double feetY, int maxWidth, int maxHeight, boolean facingRight) {
        double scale = Math.min(maxWidth / (double) sprites.width(), maxHeight / (double) sprites.height());
        AffineTransform transform = new AffineTransform();
        transform.translate(feetX, feetY);
        transform.scale(facingRight ? scale : -scale, scale);
        transform.translate(-sprites.anchorX(), -sprites.anchorY());
        Graphics2D copy = (Graphics2D) graphics.create();
        try {
            copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            copy.drawImage(image(), transform, null);
        } finally { copy.dispose(); }
    }
}
