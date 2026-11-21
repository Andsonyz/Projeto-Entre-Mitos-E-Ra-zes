package br.com.raizes.core;

import br.com.raizes.model.Phase;
import br.com.raizes.model.Thing;
import br.com.raizes.model.TreeDecoration;
import static br.com.raizes.config.GameConfig.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;

/** Desenha o cenário, os objetos da fase e a interface do mundo. */
final class WorldRenderer {
    private static final int[] WATER_SOURCE_XS = { 145, 1650 };
    private final Game game;

    WorldRenderer(Game game) {
        this.game = game;
    }

    private BufferedImage scenery;
    private int preparedPhase = -1;
    private boolean preparedCelebration;
    private long preparedVegetation = -1;

    void prepare() {
        if (scenery != null && preparedPhase == game.currentPhase
                && preparedCelebration == game.celebrationActive && preparedVegetation == game.vegetationGeneration) return;
        BufferedImage prepared = new BufferedImage(WORLD_W, H - 78, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prepared.createGraphics();
        try {
            g.translate(0, -78);
            g.setColor(new Color(47, 95, 57));
            g.fillRect(0, 78, WORLD_W, H - 78);
            if (game.worldBackground != null)
                g.drawImage(game.worldBackground, (WORLD_W - game.worldBackground.getWidth()) / 2, 78, game);
            for (TreeDecoration tree : game.trees) g.drawImage(tree.image(), tree.x(), tree.y(), game);
            for (Rectangle platform : game.platforms) {
                if (platform.y == GROUND_Y) drawGround(g, platform); else drawPlatform(g, platform);
            }
            if (!game.celebrationActive) {
                g.setColor(PHASE_FILTERS[game.currentPhase]);
                g.fillRect(0, 78, WORLD_W, H - 78);
                drawGuardian(g, game.currentPhase, 3110);
            } else {
                for (int guardian=0; guardian<Game.CELEBRATION_GUARDIANS.length; guardian++)
                    drawGuardian(g, guardian, Game.CELEBRATION_GUARDIANS[guardian]);
            }
        } finally { g.dispose(); }
        scenery = prepared;
        preparedPhase = game.currentPhase;
        preparedCelebration = game.celebrationActive;
        preparedVegetation = game.vegetationGeneration;
    }

    private void drawGuardian(Graphics2D g, int guardian, int centerX) {
        BufferedImage portrait = game.guardianWorldImage(guardian);
        g.drawImage(portrait, centerX-portrait.getWidth()/2, GROUND_Y-portrait.getHeight(), game);
        String name = Phase.values()[guardian].guardian;
        game.drawText(g, name, centerX - name.length()*4, GROUND_Y-portrait.getHeight()-15, 12, new Color(255,226,125));
    }

    void draw(Graphics2D g) {
        g.setColor(new Color(25, 48, 51));
        g.fillRect(0, 0, W, H);
        g.translate(-game.camera, 0);
        g.drawImage(scenery, 0, 78, game);
        if (game.currentPhase == 2) drawFireStations(g);
        if (game.currentPhase == 0) {
            for (Thing trap : game.traps)
                if (!trap.hit && intersectsCamera(trap.x - 30, trap.x + 30)) drawTrap(g, trap.x, trap.y);
        }
        for (Thing fire : game.fires)
            if (!fire.hit && intersectsCamera(fire.x - 48, fire.x + 48)) drawFire(g, fire.x, fire.y);
        for (Thing coin : game.coins)
            if (!coin.hit && intersectsCamera(coin.x - 33, coin.x + 33)) drawObjective(g, coin);
        for (Thing enemy : game.enemies) {
            if (enemy.hit || !intersectsCamera(enemy.x - 20, enemy.x + 20)) continue;
            int enemyY = GROUND_Y - 48;
            g.setColor(new Color(106, 55, 42)); g.fillOval(enemy.x - 20, enemyY, 40, 48);
            g.setColor(new Color(245, 177, 130)); g.fillOval(enemy.x - 13, enemyY + 6, 26, 22);
            g.setColor(Color.WHITE); g.fillOval(enemy.x - 9, enemyY + 12, 6, 6); g.fillOval(enemy.x + 3, enemyY + 12, 6, 6);
        }
        drawPlayer(g);
        g.translate(game.camera, 0);

        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, W, 78);
        g.setColor(new Color(214, 177, 91, 190));
        g.setStroke(new java.awt.BasicStroke(1));
        g.drawRect(0, 0, W - 1, 77);
        g.setStroke(new java.awt.BasicStroke(1));
        game.drawText(g, game.celebrationActive ? "FASE FINAL - CELEBRA\u00c7\u00c3O DA FLORESTA" : Phase.values()[game.currentPhase].title,
            34, 31, 15, new Color(255, 226, 125));
        game.drawText(g, game.objectiveStatus() + (game.celebrationActive ? "" : "   VIDAS " + "*".repeat(game.lives)), 34, 58, 17, Color.WHITE);
        String controls = game.celebrationActive ? "E conversar   M mapa   F5 salvar" : game.currentPhase == 0 || game.currentPhase == 2
            ? "E interage   A/D correr   Espaco pular   M mapa   Esc pausa"
            : "A/D correr   Espaco pular   M mapa   F5 salvar   Esc pausa";
        game.drawText(g, controls, 690, 58, 15, Color.WHITE);
    }

    private void drawGround(Graphics2D g, Rectangle collision) {
        BufferedImage groundTile = game.groundTile;
        if (groundTile == null) {
            g.setColor(new Color(91, 125, 54)); g.fillRect(collision.x, collision.y, collision.width, collision.height);
            return;
        }
        Shape previousClip = g.getClip();
        int tileWidth = groundTile.getWidth();
        int firstTile = Math.floorDiv(collision.x, tileWidth) * tileWidth;
        int artY = collision.y - GROUND_ART_SURFACE_OFFSET;
        g.clipRect(collision.x, artY, collision.width, collision.height + GROUND_ART_SURFACE_OFFSET);
        for (int tileX = firstTile; tileX < collision.x + collision.width; tileX += tileWidth)
            g.drawImage(groundTile, tileX, artY, game);
        g.setClip(previousClip);
    }

    private void drawPlatform(Graphics2D g, Rectangle collision) {
        BufferedImage artwork = game.platformArtwork.get(Game.platformArtKey(collision));
        if (artwork != null) g.drawImage(artwork, collision.x, collision.y, game);
        else {
            g.setColor(new Color(115, 72, 48)); g.fillRect(collision.x, collision.y, collision.width, collision.height);
            g.setColor(new Color(70, 170, 78)); g.fillRect(collision.x, collision.y + PLATFORM_SURFACE_OFFSET, collision.width, 9);
        }
    }

    private void drawFireStations(Graphics2D g) {
        for (int sourceX : WATER_SOURCE_XS) {
            if (!intersectsCamera(sourceX - 79, sourceX + 79)) continue;
            if (game.lakeImage != null) {
                // Keep the pond low and compact so its stone rim sits naturally in the ground.
                drawItemImage(g, game.lakeImage, sourceX, GROUND_Y + 4);
            } else {
                g.setColor(new Color(70, 126, 188)); g.fillRoundRect(sourceX - 26, GROUND_Y - 58, 52, 58, 8, 8);
                g.setColor(new Color(190, 230, 255)); g.fillOval(sourceX - 17, GROUND_Y - 48, 34, 16);
                g.setColor(Color.WHITE); g.drawString("AGUA", sourceX - 19, GROUND_Y - 65);
            }
        }
    }

    private void drawFire(Graphics2D g, int fireX, int fireY) {
        int fireBaseY = fireY + 42;
        if (game.fireImage != null) {
            drawItemImage(g, game.fireImage, fireX, fireBaseY + 2);
            return;
        }
        int flicker = (int) (Math.sin(game.elapsed * 12 + fireX) * 5);
        g.setColor(new Color(255, 99, 24, 100)); g.fillOval(fireX - 30, fireBaseY - 70, 60, 55);
        g.setColor(new Color(232, 66, 23)); g.fillOval(fireX - 19, fireBaseY - 63 - flicker, 38, 42);
        g.setColor(new Color(255, 202, 50)); g.fillOval(fireX - 9, fireBaseY - 49 - flicker, 18, 27);
    }

    private void drawTrap(Graphics2D g, int trapX, int trapY) {
        if (game.trapImage != null) {
            drawItemImage(g, game.trapImage, trapX, GROUND_Y);
            return;
        }
        g.setColor(new Color(82, 53, 34)); g.setStroke(new java.awt.BasicStroke(6));
        g.drawLine(trapX - 20, trapY - 4, trapX + 20, trapY + 5);
        g.drawLine(trapX - 17, trapY - 11, trapX + 17, trapY + 12);
        g.setStroke(new java.awt.BasicStroke(1));
        g.setColor(new Color(231, 196, 125)); g.fillOval(trapX - 5, trapY - 4, 10, 10);
    }

    private void drawItemImage(Graphics2D g, BufferedImage image, int centerX, int bottomY) {
        // Item artwork is fitted once by GameAssets, not resampled for every visible frame.
        g.drawImage(image, centerX - image.getWidth() / 2, bottomY - image.getHeight(), game);
    }

    private boolean intersectsCamera(double left, double right) {
        return right > game.camera && left < game.camera + W;
    }

    private void drawObjective(Graphics2D g, Thing item) {
        int itemY = game.coinY(item);
        if (game.currentPhase == 3) {
            if (game.trashImage != null) {
                drawItemImage(g, game.trashImage, item.x, itemY + 28);
                return;
            }
            g.setColor(new Color(74, 98, 57)); g.fillRoundRect(item.x - 11, itemY, 22, 18, 4, 4);
            g.setColor(new Color(218, 199, 151)); g.fillRect(item.x - 8, itemY + 4, 16, 10);
            g.setColor(new Color(129, 180, 91)); g.drawRect(item.x - 11, itemY, 22, 18);
        } else {
            if (game.currentPhase == 0 && game.seedImage != null) {
                drawItemImage(g, game.seedImage, item.x, itemY + 22);
                return;
            }
            g.setColor(new Color(255, 215, 55)); g.fillOval(item.x - 11, itemY, 22, 22);
            g.setColor(new Color(255, 246, 151)); g.fillOval(item.x - 5, itemY + 4, 5, 7);
        }
    }

    private void drawPlayer(Graphics2D g) {
        game.playerAnimator.draw(g, game.x + 2 + PLAYER_WIDTH / 2.0, game.y + PLAYER_HEIGHT,
            PLAYER_DRAW_WIDTH, PLAYER_DRAW_HEIGHT, game.facingRight);
    }
}
