package br.com.raizes.character;

import br.com.raizes.config.GameConfig;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public final class CharacterSpriteRegressionTest {
    public static void main(String[] args) throws Exception {
        check(GameConfig.PLAYER_WIDTH == 42 && GameConfig.PLAYER_HEIGHT == 58, "A hitbox existente foi alterada.");
        check(!CharacterDefinition.FEMALE.selectable() && CharacterDefinition.FEMALE.animations().isEmpty(),
            "Feminino deve permanecer bloqueado e sem recursos substitutos.");
        for (String id : new String[] { null, "female", "desconhecido", "male" })
            check(CharacterDefinition.fromSave(id) == CharacterDefinition.MALE, "Save incompatível não normalizado.");
        try { new PlayerAnimator(CharacterDefinition.FEMALE); throw new AssertionError("Feminino foi carregado."); }
        catch (IllegalArgumentException expected) { }

        SpriteAnimationSet sprites = SpriteAnimationSet.load(CharacterDefinition.MALE);
        for (AnimationState state : AnimationState.values()) {
            int count = state == AnimationState.JUMP || state == AnimationState.FALL ? 4 : 16;
            check(sprites.frameCount(state) == count, "Quantidade incorreta de quadros: " + state);
            for (int index = 0; index < count; index++) {
                BufferedImage frame = sprites.frame(state, index);
                check(frame.getWidth() == sprites.width() && frame.getHeight() == sprites.height(), "Canvas variável.");
                int pixels = 0, bottom = -1;
                for (int y = 0; y < frame.getHeight(); y++) for (int x = 0; x < frame.getWidth(); x++) {
                    if ((frame.getRGB(x,y) >>> 24) <= 16) continue;
                    pixels++;
                    bottom = Math.max(bottom, y);
                    check(x > 0 && x < frame.getWidth()-1 && y > 0 && y < frame.getHeight()-1,
                        "Sprite cortado na borda: " + state + " " + index);
                }
                check(pixels > 1000, "Quadro vazio: " + state + " " + index);
                check(Math.abs(bottom - (sprites.anchorY() - 1)) <= 2, "Âncora vertical varia: " + state + " " + index);
            }
        }
        PlayerAnimator slow = new PlayerAnimator(CharacterDefinition.MALE);
        PlayerAnimator fast = new PlayerAnimator(CharacterDefinition.MALE);
        for (int i=0;i<30;i++) slow.update(AnimationState.RUN, 1.0/30);
        for (int i=0;i<120;i++) fast.update(AnimationState.RUN, 1.0/120);
        check(slow.frameIndex() == fast.frameIndex(), "Animação depende da taxa de atualização.");
        slow.reset();
        slow.update(AnimationState.RUN, 0.07);
        slow.update(AnimationState.RUN, 0.07);
        check(slow.frameIndex() == 2, "Animação reiniciada a cada atualização.");
        slow.update(AnimationState.FALL, 10);
        check(slow.frameIndex() == 3, "Queda não preserva a pose final.");
        slow.update(AnimationState.IDLE, 0);
        check(slow.frameIndex() == 0, "Mudança de estado não reiniciou a animação.");
        for (int i=0;i<10;i++) slow.image();
        check(slow.frameIndex() == 0, "Desenho não deve avançar o relógio.");
        createContactSheet(sprites);
        System.out.println("OK: 56 poses, transparência, recortes, âncora, proporção, relógio e bloqueio de personagem.");
    }

    private static void createContactSheet(SpriteAnimationSet sprites) throws Exception {
        Path artifacts = Path.of("out/project-tests/artifacts");
        Files.createDirectories(artifacts);
        BufferedImage contact = new BufferedImage(1024, 960, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = contact.createGraphics();
        graphics.setColor(new Color(32,48,45));
        graphics.fillRect(0,0,1024,960);
        int row = 0;
        for (AnimationState state : AnimationState.values()) {
            for (int index=0;index<sprites.frameCount(state);index++) {
                int tileX = (index%8)*128, tileY = (row+index/8)*120;
                graphics.setColor(new Color(100,135,120));
                graphics.drawLine(tileX, tileY+98, tileX+127, tileY+98);
                graphics.drawRect(tileX+64-21, tileY+98-58, 42,58);
                PlayerAnimator animator = new PlayerAnimator(CharacterDefinition.MALE);
                animator.update(state, index*CharacterDefinition.MALE.animations().get(state).secondsPerFrame());
                animator.draw(graphics, tileX+64, tileY+98, 58,78,true);
                graphics.setColor(Color.WHITE);
                graphics.drawString(state+" "+index,tileX+8,tileY+115);
            }
            row += (sprites.frameCount(state)+7)/8;
        }
        graphics.dispose();
        ImageIO.write(contact,"png",artifacts.resolve("sprites-contact.png").toFile());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
