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
    private final Game game;

    WorldRenderer(Game game) {
        this.game = game;
    }

    void draw(Graphics2D g) {
        g.setColor(new Color(75, 151, 83));
        g.fillRect(0, 0, W, H);
        g.translate(-game.camera, 0);
        if (game.worldBackground != null) g.drawImage(game.worldBackground, 0, 78, game);
        else {
            g.setColor(new Color(47, 95, 57));
            g.fillRect(0, 78, WORLD_W, GROUND_Y - 78);
        }
        for (TreeDecoration tree : game.trees) g.drawImage(tree.image(), tree.x(), tree.y(), game);
        for (Rectangle platform : game.platforms) {
            if (platform.y == GROUND_Y) drawGround(g, platform);
            else drawPlatform(g, platform);
        }
        g.setColor(PHASE_FILTERS[game.currentPhase]);
        g.fillRect((int) game.camera, 78, W, H - 78);
        if (game.currentPhase == 2) drawFireStations(g);
        if (game.currentPhase == 0) {
            for (Thing trap : game.traps) if (!trap.hit) drawTrap(g, trap.x, trap.y);
        }
        for (Thing fire : game.fires) if (!fire.hit) drawFire(g, fire.x, fire.y);
        for (Thing coin : game.coins) if (!coin.hit) drawObjective(g, coin);
        for (Thing enemy : game.enemies) {
            if (enemy.hit) continue;
            g.setColor(new Color(106, 55, 42)); g.fillOval(enemy.x - 20, 552, 40, 48);
            g.setColor(new Color(245, 177, 130)); g.fillOval(enemy.x - 13, 558, 26, 22);
            g.setColor(Color.WHITE); g.fillOval(enemy.x - 9, 564, 6, 6); g.fillOval(enemy.x + 3, 564, 6, 6);
        }
        drawPlayer(g);
        g.translate(game.camera, 0);

        g.setColor(new Color(25, 48, 51, 220)); g.fillRect(0, 0, W, 78);
        game.drawText(g, Phase.values()[game.currentPhase].title, 24, 27, 15, new Color(255, 226, 125));
        String objective = switch (game.currentPhase) {
            case 0 -> "Sementes: " + game.coinsCollected + "/12   Armadilhas: " + game.traps.stream().filter(t -> t.hit).count() + "/3";
            case 1 -> "Travessia: " + Math.min(100, (int) (game.x * 100 / 3020)) + "%";
            case 2 -> "Focos: " + game.fires.stream().filter(f -> f.hit).count() + "/" + game.fires.size() + "   Balde: " + (game.bucketFull ? "cheio" : "vazio");
            default -> "Residuos: " + game.coinsCollected + "/10";
        };
        game.drawText(g, objective + "   VIDAS " + "*".repeat(game.lives), 24, 58, 17, Color.WHITE);
        String controls = game.currentPhase == 0 || game.currentPhase == 2
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
        BufferedImage artwork = game.platformArtwork.get(collision.width);
        if (artwork != null) g.drawImage(artwork, collision.x, collision.y, game);
        else {
            g.setColor(new Color(115, 72, 48)); g.fillRect(collision.x, collision.y, collision.width, collision.height);
            g.setColor(new Color(70, 170, 78)); g.fillRect(collision.x, collision.y + PLATFORM_SURFACE_OFFSET, collision.width, 9);
        }
    }

    private void drawFireStations(Graphics2D g) {
        for (int sourceX : new int[] { 145, 1650 }) {
            g.setColor(new Color(70, 126, 188)); g.fillRoundRect(sourceX - 26, 542, 52, 58, 8, 8);
            g.setColor(new Color(190, 230, 255)); g.fillOval(sourceX - 17, 552, 34, 16);
            g.setColor(Color.WHITE); g.drawString("AGUA", sourceX - 19, 535);
        }
    }

    private void drawFire(Graphics2D g, int fireX, int fireY) {
        int flicker = (int) (Math.sin(game.elapsed * 12 + fireX) * 5);
        g.setColor(new Color(255, 99, 24, 100)); g.fillOval(fireX - 30, fireY - 35, 60, 55);
        g.setColor(new Color(232, 66, 23)); g.fillOval(fireX - 19, fireY - 28 - flicker, 38, 42);
        g.setColor(new Color(255, 202, 50)); g.fillOval(fireX - 9, fireY - 14 - flicker, 18, 27);
    }

    private void drawTrap(Graphics2D g, int trapX, int trapY) {
        g.setColor(new Color(82, 53, 34)); g.setStroke(new java.awt.BasicStroke(6));
        g.drawLine(trapX - 20, trapY - 4, trapX + 20, trapY + 5);
        g.drawLine(trapX - 17, trapY - 11, trapX + 17, trapY + 12);
        g.setStroke(new java.awt.BasicStroke(1));
        g.setColor(new Color(231, 196, 125)); g.fillOval(trapX - 5, trapY - 4, 10, 10);
    }

    private void drawObjective(Graphics2D g, Thing item) {
        int itemY = game.currentPhase == 3 ? item.y : (int) (420 + Math.sin(game.elapsed * 3 + item.x) * 30);
        if (game.currentPhase == 3) {
            g.setColor(new Color(74, 98, 57)); g.fillRoundRect(item.x - 11, itemY, 22, 18, 4, 4);
            g.setColor(new Color(218, 199, 151)); g.fillRect(item.x - 8, itemY + 4, 16, 10);
            g.setColor(new Color(129, 180, 91)); g.drawRect(item.x - 11, itemY, 22, 18);
        } else {
            g.setColor(new Color(255, 215, 55)); g.fillOval(item.x - 11, itemY, 22, 22);
            g.setColor(new Color(255, 246, 151)); g.fillOval(item.x - 5, itemY + 4, 5, 7);
        }
    }

    private void drawPlayer(Graphics2D g) {
        int px = (int) game.x, py = (int) game.y + 5;
        BufferedImage image = game.walking ? ((game.walkingFrame / 8) % 2 == 0 ? game.playerWalk1 : game.playerWalk2) : game.playerImage;
        if (image != null) {
            if (game.facingRight) g.drawImage(image, px - 8, py - 20, 58, 78, game);
            else g.drawImage(image, px + 50, py - 20, px - 8, py + 58, 0, 0, image.getWidth(), image.getHeight(), game);
        } else {
            g.setColor(new Color(217, 92, 70)); g.fillOval(px, py, 38, 38);
            g.setColor(new Color(242, 177, 139)); g.fillOval(px + 5, py - 17, 28, 28);
        }
    }
}
