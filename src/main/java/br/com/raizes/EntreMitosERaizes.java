package br.com.raizes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class EntreMitosERaizes {
    private EntreMitosERaizes() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Entre Mitos e Raizes - A Trilha do Curupira");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new Game());
            window.pack();
            window.setLocationRelativeTo(null);
            window.setResizable(false);
            window.setVisible(true);
        });
    }

    private enum Screen { MENU, SLOTS, NAME, WORLD, PAUSE, SETTINGS, CONTROLS, EXTRAS, MAP, DIALOGUE, BATTLE, END }

    private static final class Game extends JPanel {
        private static final int W = 1280, H = 720, WORLD_W = 3200;
        private final Set<Integer> held = new HashSet<>();
        private final List<Rectangle> platforms = new ArrayList<>();
        private final List<Thing> coins = new ArrayList<>();
        private final List<Thing> enemies = new ArrayList<>();
        private final Timer timer;
        private Screen screen = Screen.MENU;
        private Screen settingsReturn = Screen.MENU;
        private BufferedImage playerImage, playerWalk1, playerWalk2, curupiraImage, gameBackground, menuBackground, menuTitle;
        private final BufferedImage[] menuImages = new BufferedImage[5];
        private final Rectangle[] menuBounds = new Rectangle[5];
        private String name = "Viajante", toast = "";
        private String[] dialogue = { "Curupira", "A floresta esta em perigo. Colete as sementes e avance pela trilha.", "Pule os obstaculos, pise nos inimigos e chegue a clareira.", "Boa sorte, " + name + "!" };
        private int menuIndex, slotIndex, settingsIndex, lives = 3, coinsCollected, battleChoice;
        private int textScale = 1;
        private boolean contrast, nameConfirm, won, onGround;
        private boolean facingRight = true, walking;
        private int walkingFrame;
        private double x = 110, y = 500, vy, camera, elapsed;
        private int dialogueIndex;
        private long toastUntil;

        Game() {
            setPreferredSize(new Dimension(W, H));
            setFocusable(true);
            loadImages();
            buildLevel();
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) { held.add(e.getKeyCode()); keyPressedGame(e.getKeyCode()); }
                @Override public void keyReleased(KeyEvent e) { held.remove(e.getKeyCode()); }
                @Override public void keyTyped(KeyEvent e) {
                    if (screen == Screen.NAME && !nameConfirm && name.length() < 18) {
                        char c = e.getKeyChar();
                        if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') name += c;
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    if (screen != Screen.MENU) return;
                    menuIndex = menuAt(e.getX(), e.getY());
                    setCursor(menuIndex >= 0 ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR) : java.awt.Cursor.getDefaultCursor());
                    repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (screen == Screen.MENU) {
                        int selected = menuAt(e.getX(), e.getY());
                        if (selected >= 0) { menuIndex = selected; menuKey(KeyEvent.VK_ENTER); }
                    }
                }
            });
            timer = new Timer(16, e -> tick());
            timer.start();
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        }

        private void loadImages() {
            playerImage = image("Image/Personagens/Greg/GregParadoD.png");
            playerWalk1 = image("Image/Personagens/Greg/GregAndandoDPT1.png");
            playerWalk2 = image("Image/Personagens/Greg/GregAndandoDPT2.png");
            curupiraImage = image("Image/Personagens/guardioes/CURUPIRA.PNG");
            gameBackground = image("Image/Cenarios/FundoDoJogo.jpeg");
            menuBackground = image("Image/Cenarios/Menu/MENU.jpg");
            menuTitle = image("Image/Cenarios/Menu/Titulo.png");
            menuImages[0] = image("Image/Cenarios/Menu/NovoGame.png");
            menuImages[1] = image("Image/Cenarios/Menu/Continuar.png");
            menuImages[2] = image("Image/Cenarios/Menu/Configuracoes.png");
            menuImages[3] = image("Image/Cenarios/Menu/Interrogacao.png");
            menuImages[4] = image("Image/Cenarios/Menu/Sair.png");
        }

        private BufferedImage image(String path) {
            try { return ImageIO.read(new File(path)); }
            catch (IOException ignored) { return null; }
        }

        private void buildLevel() {
            platforms.clear(); coins.clear(); enemies.clear();
            platforms.add(new Rectangle(0, 600, WORLD_W, 120));
            int[][] ledges = { {330, 500, 190, 28}, {640, 430, 190, 28}, {960, 520, 220, 28}, {1320, 450, 210, 28}, {1640, 370, 190, 28}, {1980, 500, 230, 28}, {2310, 420, 220, 28}, {2630, 520, 180, 28} };
            for (int[] p : ledges) platforms.add(new Rectangle(p[0], p[1], p[2], p[3]));
            int[] coinXs = { 360, 420, 675, 735, 1010, 1080, 1370, 1450, 1690, 1750, 2030, 2110, 2360, 2440, 2680, 2750, 2940 };
            for (int coinX : coinXs) coins.add(new Thing(coinX, 0));
            enemies.add(new Thing(760, 0)); enemies.add(new Thing(1500, 0)); enemies.add(new Thing(2180, 0)); enemies.add(new Thing(2820, 0));
        }

        private void tick() {
            double dt = 0.016;
            elapsed += dt;
            if (screen == Screen.WORLD) updateWorld(dt);
            if (screen == Screen.DIALOGUE && held.contains(KeyEvent.VK_ENTER)) { held.remove(KeyEvent.VK_ENTER); advanceDialogue(); }
            if (toastUntil < System.currentTimeMillis()) toast = "";
            repaint();
        }

        private void updateWorld(double dt) {
            double dx = (held.contains(KeyEvent.VK_D) || held.contains(KeyEvent.VK_RIGHT) ? 1 : 0) - (held.contains(KeyEvent.VK_A) || held.contains(KeyEvent.VK_LEFT) ? 1 : 0);
            walking = dx != 0;
            if (walking) { walkingFrame++; if (dx > 0) facingRight = true; else facingRight = false; }
            else walkingFrame = 0;
            x = clamp(x + dx * 260 * dt, 0, WORLD_W - 42);
            if ((held.contains(KeyEvent.VK_SPACE) || held.contains(KeyEvent.VK_W) || held.contains(KeyEvent.VK_UP)) && onGround) { vy = -590; onGround = false; }
            vy += 1350 * dt;
            double oldY = y;
            y += vy * dt;
            onGround = false;
            Rectangle body = new Rectangle((int)x, (int)y, 38, 58);
            for (Rectangle platform : platforms) {
                if (body.intersects(platform) && oldY + 58 <= platform.y + 8 && vy >= 0) { y = platform.y - 58; vy = 0; onGround = true; body.y = (int)y; }
            }
            if (y > H + 100) loseLife();
            for (Thing coin : coins) if (!coin.hit && body.intersects(new Rectangle(coin.x - 12, coinY(coin), 24, 24))) { coin.hit = true; coinsCollected++; }
            for (Thing enemy : enemies) {
                if (enemy.hit) continue;
                enemy.x += Math.sin(elapsed + enemy.x) * 0.7;
                Rectangle enemyRect = new Rectangle(enemy.x - 20, 552, 40, 48);
                if (body.intersects(enemyRect)) {
                    if (vy > 0 && y + 58 < 575) { enemy.hit = true; vy = -360; }
                    else loseLife();
                }
            }
            camera = clamp(x - 340, 0, WORLD_W - W);
            if (x > 3020) {
                if (coinsCollected >= 12) { screen = Screen.DIALOGUE; dialogueIndex = 0; }
                else showToast("Colete pelo menos 12 sementes para abrir a clareira.");
            }
        }

        private void loseLife() {
            lives--; x = 110; y = 500; vy = 0;
            if (lives <= 0) { lives = 3; coinsCollected = 0; buildLevel(); showToast("A floresta lhe deu outra chance."); }
        }

        private void keyPressedGame(int key) {
            if (screen == Screen.MENU) menuKey(key);
            else if (screen == Screen.SLOTS) slotsKey(key);
            else if (screen == Screen.NAME) nameKey(key);
            else if (screen == Screen.WORLD) worldKey(key);
            else if (screen == Screen.PAUSE) pauseKey(key);
            else if (screen == Screen.SETTINGS) settingsKey(key);
            else if (screen == Screen.DIALOGUE) dialogueKey(key);
            else if (screen == Screen.BATTLE) battleKey(key);
            else if (screen == Screen.END) { if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) screen = Screen.MENU; }
            else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_M) screen = screen == Screen.MAP ? Screen.WORLD : Screen.MENU;
        }

        private void menuKey(int key) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) menuIndex = (menuIndex + 5 - 1) % 5;
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) menuIndex = (menuIndex + 1) % 5;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                if (menuIndex == 0 || menuIndex == 1) { slotIndex = menuIndex == 1 && firstSave() >= 0 ? firstSave() : 0; screen = Screen.SLOTS; }
                else if (menuIndex == 2) { settingsReturn = Screen.MENU; screen = Screen.SETTINGS; }
                else if (menuIndex == 3) screen = Screen.EXTRAS;
                else System.exit(0);
            }
        }

        private void slotsKey(int key) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) slotIndex = 1 - slotIndex;
            if (key == KeyEvent.VK_DELETE) deleteSave(slotIndex);
            if (key == KeyEvent.VK_ESCAPE) screen = Screen.MENU;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (hasSave(slotIndex)) loadSave(slotIndex); else { name = ""; nameConfirm = false; screen = Screen.NAME; } }
        }

        private void nameKey(int key) {
            if (key == KeyEvent.VK_BACK_SPACE && !nameConfirm && !name.isEmpty()) name = name.substring(0, name.length() - 1);
            else if (key == KeyEvent.VK_ESCAPE) screen = Screen.SLOTS;
            else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (nameConfirm) startGame(); else nameConfirm = true; }
        }

        private void worldKey(int key) {
            if (key == KeyEvent.VK_ESCAPE) screen = Screen.PAUSE;
            else if (key == KeyEvent.VK_F5) saveGame();
            else if (key == KeyEvent.VK_M) screen = Screen.MAP;
        }

        private void pauseKey(int key) {
            if (key == KeyEvent.VK_ESCAPE) { screen = Screen.WORLD; return; }
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) menuIndex = (menuIndex + 4) % 5;
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) menuIndex = (menuIndex + 1) % 5;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (menuIndex == 0) screen = Screen.WORLD; else if (menuIndex == 1) saveGame(); else if (menuIndex == 2) { settingsReturn = Screen.PAUSE; screen = Screen.SETTINGS; } else if (menuIndex == 3) screen = Screen.MAP; else { saveGame(); screen = Screen.MENU; } }
        }

        private void settingsKey(int key) {
            if (key == KeyEvent.VK_ESCAPE) { screen = settingsReturn; return; }
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) settingsIndex = (settingsIndex + 2) % 3;
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) settingsIndex = (settingsIndex + 1) % 3;
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (settingsIndex == 0) textScale = (textScale + 1) % 3; else if (settingsIndex == 1) contrast = !contrast; else screen = Screen.CONTROLS; }
        }

        private void dialogueKey(int key) { if (key == KeyEvent.VK_ESCAPE) screen = Screen.WORLD; else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_E) advanceDialogue(); }
        private void advanceDialogue() { if (++dialogueIndex >= dialogue.length) { screen = Screen.BATTLE; battleChoice = 0; } }
        private void battleKey(int key) { if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_4) battleChoice = key - KeyEvent.VK_1; if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (battleChoice == 1) { won = true; screen = Screen.END; saveGame(); } else showToast("A resposta correta e respeitar a floresta."); } }

        private void startGame() { if (name.trim().isEmpty()) name = "Viajante"; dialogue = new String[] { "Curupira", "A floresta esta em perigo. Colete as sementes e avance pela trilha.", "Pule os obstaculos, pise nos inimigos e chegue a clareira.", "Boa sorte, " + name + "!" }; x = 110; y = 500; vy = 0; lives = 3; coinsCollected = 0; won = false; buildLevel(); saveGame(); screen = Screen.WORLD; }
        private void drawText(Graphics2D g, String text, int x, int y, int size, Color color) { g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, size + textScale * 2))); g.setColor(color); g.drawString(text, x, y); }
        private Color ink() { return contrast ? Color.WHITE : new Color(25, 48, 51); }
        private Color panel() { return contrast ? new Color(12, 12, 12) : new Color(249, 235, 187); }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (screen == Screen.MENU) drawMenu(g); else if (screen == Screen.SLOTS) drawSlots(g); else if (screen == Screen.NAME) drawName(g); else if (screen == Screen.SETTINGS) drawSettings(g); else if (screen == Screen.CONTROLS) drawControls(g); else if (screen == Screen.EXTRAS) drawExtras(g); else { drawWorld(g); if (screen == Screen.PAUSE) drawPause(g); else if (screen == Screen.MAP) drawMap(g); else if (screen == Screen.DIALOGUE) drawDialogue(g); else if (screen == Screen.BATTLE) drawBattle(g); else if (screen == Screen.END) drawEnd(g); }
            if (!toast.isEmpty()) { g.setColor(new Color(0, 0, 0, 190)); g.fillRoundRect(350, 645, 580, 42, 10, 10); drawText(g, toast, 380, 673, 16, Color.WHITE); }
            g.dispose();
        }

        private void drawMenu(Graphics2D g) {
            drawImageCover(g, menuBackground, 0, 0, W, H);
            g.setColor(new Color(0, 0, 0, 72));
            g.fillRect(0, 0, W, H);
            if (menuTitle != null) drawImageFit(g, menuTitle, W / 2 - 250, 48, 500, 175);
            for (int i = 0; i < menuImages.length; i++) {
                BufferedImage button = menuImages[i];
                if (button == null) continue;
                int maxWidth = i == 3 ? 82 : 300;
                int maxHeight = i == 3 ? 82 : 58;
                double scale = Math.min(maxWidth / (double) button.getWidth(), maxHeight / (double) button.getHeight());
                int width = Math.max(1, (int) (button.getWidth() * scale));
                int height = Math.max(1, (int) (button.getHeight() * scale));
                int centerX = i == 3 ? 1110 : 290;
                int centerY = i == 3 ? 625 : 285 + i * 72;
                menuBounds[i] = new Rectangle(centerX - width / 2, centerY - height / 2, width, height);
                Rectangle bounds = menuBounds[i];
                if (i == menuIndex) {
                    g.setColor(new Color(255, 226, 110, 90));
                    g.fillRoundRect(bounds.x - 12, bounds.y - 8, bounds.width + 24, bounds.height + 16, 12, 12);
                }
                g.drawImage(button, bounds.x, bounds.y, bounds.width, bounds.height, this);
            }
            drawText(g, "Setas/WASD selecionam   Enter ou clique confirma", 410, 675, 15, Color.WHITE);
        }

        private int menuAt(int mouseX, int mouseY) {
            for (int i = 0; i < menuBounds.length; i++) if (menuBounds[i] != null && menuBounds[i].contains(mouseX, mouseY)) return i;
            return -1;
        }
        private void drawImageFit(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
            if (image == null) return;
            double scale = Math.min(width / (double) image.getWidth(), height / (double) image.getHeight());
            int drawWidth = (int) (image.getWidth() * scale);
            int drawHeight = (int) (image.getHeight() * scale);
            g.drawImage(image, x + (width - drawWidth) / 2, y + (height - drawHeight) / 2, drawWidth, drawHeight, this);
        }

        private void drawImageCover(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
            if (image == null) { g.setColor(new Color(25, 69, 53)); g.fillRect(x, y, width, height); return; }
            double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
            int drawWidth = (int) (image.getWidth() * scale);
            int drawHeight = (int) (image.getHeight() * scale);
            int drawX = x + (width - drawWidth) / 2;
            int drawY = y + (height - drawHeight) / 2;
            g.drawImage(image, drawX, drawY, drawWidth, drawHeight, this);
        }

        private void drawBackdrop(Graphics2D g) { g.setColor(new Color(25, 69, 53)); g.fillRect(0, 0, W, H); g.setColor(new Color(51, 120, 75)); for (int i = 0; i < 18; i++) g.fillOval(i * 85 - 40, 280 + (i % 3) * 70, 260, 260); g.setColor(new Color(35, 89, 62)); g.fillRect(0, 570, W, 150); }
        private void drawSlots(Graphics2D g) { drawBackdrop(g); drawText(g, "ESCOLHA UM SLOT", 440, 150, 36, Color.WHITE); for (int i = 0; i < 2; i++) { int xx = 250 + i * 420; g.setColor(i == slotIndex ? new Color(217, 92, 70) : new Color(24, 58, 50)); g.fillRoundRect(xx, 250, 360, 170, 12, 12); drawText(g, "SLOT " + (i + 1), xx + 34, 300, 25, Color.WHITE); drawText(g, hasSave(i) ? saveSummary(i) : "Vazio - Enter para criar", xx + 34, 345, 16, new Color(245, 230, 190)); drawText(g, "Delete apaga", xx + 34, 385, 14, new Color(196, 222, 188)); } drawText(g, "A/D escolhem   Enter abre   Esc volta", 430, 625, 15, Color.WHITE); }
        private void drawName(Graphics2D g) { drawBackdrop(g); drawText(g, "COMO TE CHAMAM?", 405, 200, 34, Color.WHITE); g.setColor(panel()); g.fillRoundRect(330, 270, 620, 90, 10, 10); drawText(g, name + (System.currentTimeMillis() / 400 % 2 == 0 ? "_" : ""), 380, 330, 28, ink()); drawText(g, nameConfirm ? "Enter para confirmar" : "Digite seu apelido e pressione Enter", 420, 430, 17, Color.WHITE); }
        private void drawSettings(Graphics2D g) { drawBackdrop(g); drawText(g, "CONFIGURACOES", 425, 165, 38, Color.WHITE); String[] options = { "Tamanho do texto: " + (textScale == 0 ? "Compacto" : textScale == 1 ? "Padrao" : "Ampliado"), "Alto contraste: " + (contrast ? "Ligado" : "Desligado"), "Controles" }; for (int i = 0; i < 3; i++) { int y = 280 + i * 70; g.setColor(i == settingsIndex ? new Color(217, 92, 70) : new Color(24, 58, 50)); g.fillRoundRect(330, y - 35, 620, 52, 8, 8); drawText(g, options[i], 375, y, 21, Color.WHITE); } drawText(g, "Setas selecionam   Direita/Enter altera   Esc volta", 390, 625, 15, Color.WHITE); }
        private void drawControls(Graphics2D g) { drawBackdrop(g); drawText(g, "CONTROLES", 470, 170, 38, Color.WHITE); drawText(g, "A/D ou SETAS: correr", 410, 280, 24, Color.WHITE); drawText(g, "W, SETA CIMA ou ESPACO: pular", 410, 330, 24, Color.WHITE); drawText(g, "M: mapa     F5: salvar     Esc: pausar", 410, 380, 24, Color.WHITE); drawText(g, "Enter ou Esc para voltar", 470, 590, 17, new Color(245, 230, 190)); }
        private void drawExtras(Graphics2D g) { drawBackdrop(g); drawText(g, "EXTRAS", 520, 165, 38, Color.WHITE); drawText(g, "Uma aventura inspirada nas lendas da floresta brasileira.", 285, 280, 22, Color.WHITE); drawText(g, "Proteja a mata, respeite os animais e escute o Curupira.", 310, 330, 20, new Color(245, 230, 190)); drawText(g, "Enter ou Esc para voltar", 475, 590, 17, Color.WHITE); }

        private void drawWorld(Graphics2D g) { g.setColor(new Color(75, 151, 83)); g.fillRect(0, 0, W, H); drawImageCover(g, gameBackground, 0, 78, 1600, 522); drawImageCover(g, gameBackground, 1600, 78, 1600, 522); g.translate(-camera, 0); for (int i = 0; i < WORLD_W; i += 190) { g.setColor(new Color(31, 102, 69, 135)); g.fillRect(i + 35, 310, 34, 290); g.fillOval(i, 220, 110, 120); } for (Rectangle p : platforms) { g.setColor(new Color(115, 72, 48)); g.fillRect(p.x, p.y, p.width, p.height); g.setColor(new Color(70, 170, 78)); g.fillRect(p.x, p.y, p.width, 9); } for (Thing coin : coins) if (!coin.hit) { g.setColor(new Color(255, 215, 55)); g.fillOval(coin.x - 11, coinY(coin), 22, 22); g.setColor(new Color(255, 246, 151)); g.fillOval(coin.x - 5, coinY(coin) + 4, 5, 7); } for (Thing enemy : enemies) if (!enemy.hit) { g.setColor(new Color(106, 55, 42)); g.fillOval(enemy.x - 20, 552, 40, 48); g.setColor(new Color(245, 177, 130)); g.fillOval(enemy.x - 13, 558, 26, 22); g.setColor(Color.WHITE); g.fillOval(enemy.x - 9, 564, 6, 6); g.fillOval(enemy.x + 3, 564, 6, 6); } drawPlayer(g); g.translate(camera, 0); g.setColor(new Color(25, 48, 51, 210)); g.fillRect(0, 0, W, 78); drawText(g, "SEMENTES " + coinsCollected + "/12", 30, 34, 19, Color.WHITE); drawText(g, "VIDAS " + "*".repeat(lives), 230, 34, 19, new Color(255, 151, 125)); drawText(g, "M mapa   F5 salvar   Esc pausa", 930, 34, 15, Color.WHITE); }
        private int coinY(Thing coin) { return coin.y == 0 ? (int)(420 + Math.sin(elapsed * 3 + coin.x) * 30) : coin.y; }
        private void drawPlayer(Graphics2D g) { int px = (int)x, py = (int)y; Image image = walking ? ((walkingFrame / 8) % 2 == 0 ? playerWalk1 : playerWalk2) : playerImage; if (image != null) { if (facingRight) g.drawImage(image, px - 8, py - 20, 58, 78, this); else g.drawImage(image, px + 50, py - 20, px - 8, py + 58, 0, 0, image.getWidth(this), image.getHeight(this), this); } else { g.setColor(new Color(217, 92, 70)); g.fillOval(px, py, 38, 38); g.setColor(new Color(242, 177, 139)); g.fillOval(px + 5, py - 17, 28, 28); } }
        private void drawPause(Graphics2D g) { g.setColor(new Color(11, 28, 27, 225)); g.fillRect(310, 130, 660, 470); drawText(g, "PAUSA", 570, 205, 42, new Color(255, 216, 92)); String[] options = { "Continuar", "Salvar", "Configuracoes", "Mapa", "Sair para o menu" }; for (int i = 0; i < options.length; i++) drawText(g, (i == menuIndex ? "> " : "  ") + options[i], 495, 290 + i * 52, 22, Color.WHITE); }
        private void drawMap(Graphics2D g) { g.setColor(new Color(13, 39, 38, 245)); g.fillRect(100, 100, 1080, 520); drawText(g, "MAPA DA TRILHA", 485, 170, 34, new Color(255, 216, 92)); g.setColor(new Color(86, 151, 80)); g.fillRoundRect(210, 330, 220, 80, 12, 12); g.fillRoundRect(530, 250, 220, 80, 12, 12); g.fillRoundRect(850, 350, 220, 80, 12, 12); drawText(g, "INICIO", 280, 378, 20, Color.WHITE); drawText(g, "FLORESTA", 580, 298, 20, Color.WHITE); drawText(g, "CLAREIRA", 915, 398, 20, Color.WHITE); drawText(g, "M ou Esc para voltar", 520, 555, 17, Color.WHITE); }
        private void drawDialogue(Graphics2D g) { g.setColor(new Color(8, 22, 23, 220)); g.fillRoundRect(100, 500, 1080, 150, 12, 12); if (curupiraImage != null) g.drawImage(curupiraImage, 135, 370, 140, 180, this); drawText(g, dialogue[dialogueIndex], 310, 545, 23, new Color(255, 216, 92)); drawText(g, dialogue[Math.min(dialogueIndex + 1, dialogue.length - 1)], 310, 590, 19, Color.WHITE); drawText(g, "Enter/E/Space para continuar", 850, 630, 14, new Color(196, 222, 188)); }
        private void drawBattle(Graphics2D g) { g.setColor(new Color(16, 37, 38, 245)); g.fillRect(170, 100, 940, 520); drawText(g, "DESAFIO DO CURUPIRA", 430, 175, 34, new Color(255, 216, 92)); drawText(g, "Qual atitude protege a floresta?", 390, 260, 25, Color.WHITE); String[] options = { "1 - Jogar lixo no rio", "2 - Respeitar os animais e plantar arvores", "3 - Queimar a mata", "4 - Retirar os ninhos" }; for (int i = 0; i < 4; i++) drawText(g, (battleChoice == i ? "> " : "  ") + options[i], 330, 340 + i * 48, 20, battleChoice == i ? new Color(255, 216, 92) : Color.WHITE); drawText(g, "1-4 selecionam   Enter confirma", 460, 565, 16, new Color(196, 222, 188)); }
        private void drawEnd(Graphics2D g) { drawBackdrop(g); drawText(g, "A FLORESTA LEMBRA", 400, 230, 42, new Color(255, 216, 92)); drawText(g, "Voce protegeu a trilha e venceu o desafio!", 380, 315, 24, Color.WHITE); drawText(g, "Enter volta ao menu", 500, 520, 18, new Color(245, 230, 190)); }

        private int firstSave() { return hasSave(0) ? 0 : hasSave(1) ? 1 : -1; }
        private File saveFile(int slot) { return new File(System.getProperty("user.home"), ".entremitoseraizes/saves/slot-" + (slot + 1) + ".properties"); }
        private boolean hasSave(int slot) { return saveFile(slot).isFile(); }
        private String saveSummary(int slot) { Properties p = readSave(slot); return p.getProperty("name", "Viajante") + " - " + p.getProperty("coins", "0") + " sementes"; }
        private Properties readSave(int slot) { Properties p = new Properties(); if (!hasSave(slot)) return p; try (FileInputStream in = new FileInputStream(saveFile(slot))) { p.load(in); } catch (IOException ignored) { } return p; }
        private void saveGame() { File dir = saveFile(slotIndex).getParentFile(); dir.mkdirs(); Properties p = new Properties(); p.setProperty("name", name); p.setProperty("coins", String.valueOf(coinsCollected)); p.setProperty("lives", String.valueOf(lives)); p.setProperty("x", String.valueOf(x)); try (FileOutputStream out = new FileOutputStream(saveFile(slotIndex))) { p.store(out, "Entre Mitos e Raizes"); showToast("Progresso salvo no Slot " + (slotIndex + 1)); } catch (IOException ignored) { showToast("Nao foi possivel salvar."); } }
        private void loadSave(int slot) { Properties p = readSave(slot); name = p.getProperty("name", "Viajante"); coinsCollected = Integer.parseInt(p.getProperty("coins", "0")); lives = Integer.parseInt(p.getProperty("lives", "3")); x = Double.parseDouble(p.getProperty("x", "110")); buildLevel(); screen = Screen.WORLD; }
        private void deleteSave(int slot) { if (hasSave(slot)) { saveFile(slot).delete(); showToast("Slot apagado."); } }
        private void showToast(String message) { toast = message; toastUntil = System.currentTimeMillis() + 2200; }
        private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    }

    private static final class Thing {
        int x, y;
        boolean hit;
        Thing(int x, int y) { this.x = x; this.y = y; }
    }
}
