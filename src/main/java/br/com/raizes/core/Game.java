package br.com.raizes.core;


import br.com.raizes.model.Phase;
import br.com.raizes.model.Question;
import br.com.raizes.model.Thing;
import br.com.raizes.model.TreeDecoration;
import br.com.raizes.assets.AssetManager;
import br.com.raizes.audio.CinematicManager;
import br.com.raizes.persistence.SaveManager;
import br.com.raizes.quiz.QuestionBank;
import static br.com.raizes.config.GameConfig.*;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class Game extends JPanel {




    private static final String[] OPENING_VIDEOS = { "Videos/cena1.mp4", "Videos/cena2.mp4", "Videos/cena3.mp4", "Videos/cena4.mp4" };
    private static final String[] OPENING_NARRATION = {
        "O fogo avançava rápido demais pelas copas das árvores. Onde antes havia o verde vivo e o som da floresta, resta apenas a fumaça e o calor sufocante.",
        "Guiado pelo dever e pela coragem, o pequeno guardião avança entre as brasas. Sem hesitar, ele alcança o filhote assustado entre os galhos em chamas e o acolhe em seus braços.",
        "De repente, a terra sob os seus pés começa a brilhar com uma luz ancestral. A fumaça dissipa-se num turbilhão de energia e o pequeno guardião é envolvido por uma força mágica que o transporta para além dos limites deste mundo.",
        "À sua espera estão as lendárias criaturas do folclore. Sob o olhar atento do Curupira, da Caipora e das entidades protetoras da natureza, ele inicia a sua jornada de aprendizagem. Ali, entre segredos antigos e rituais esquecidos, ele deixará de ser apenas um protetor comum para se tornar um verdadeiro Guardião da Floresta."
    };

    final Set<Integer> held = new HashSet<>();
    final List<Rectangle> platforms = new ArrayList<>();
    final List<Thing> coins = new ArrayList<>();
    final List<Thing> enemies = new ArrayList<>();
    final List<Thing> fires = new ArrayList<>();
    final List<Thing> traps = new ArrayList<>();
    final List<TreeDecoration> trees = new ArrayList<>();
    final List<Question> quizQuestions = new ArrayList<>();
    final Map<Integer, BufferedImage> platformArtwork = new java.util.HashMap<>();
    final Timer timer;
    final SaveManager saveManager = new SaveManager();
    final CinematicManager cinematicManager;
    final WorldRenderer worldRenderer;
    volatile GameState screen = GameState.MENU;
    private GameState settingsReturn = GameState.MENU;
    private GameState confirmReturn = GameState.SLOTS;
    private ConfirmAction confirmAction;
    BufferedImage playerImage, playerWalk1, playerWalk2, worldBackground, groundTile, menuBackground, menuTitle;
    BufferedImage platformImage;
    BufferedImage slotTitleImage;
    BufferedImage nameTitleImage;
    BufferedImage nameTitleArtwork;
    BufferedImage pauseTitleImage;
    BufferedImage pauseIndicatorImage;
    BufferedImage settingsTitleImage;
    final BufferedImage[] slotImages = new BufferedImage[2];
    final BufferedImage[] treeImages = new BufferedImage[3];
    final BufferedImage[] guardianImages = new BufferedImage[Phase.values().length];
    final BufferedImage[] menuImages = new BufferedImage[5];
    final Rectangle[] menuBounds = new Rectangle[5];
    final Rectangle[] menuOpaqueBounds = new Rectangle[5];
    final double[] menuScale = { 1, 1, 1, 1, 1 };
    private String name = "Viajante", toast = "";
    private String[] dialogue = new String[0];
    private int menuIndex, slotIndex, settingsIndex, saveOptionIndex, phaseSelectIndex, battleChoice, quizIndex;
    int lives = 3, coinsCollected;
    private int selectedSaveSlot;
    private int pendingPhase;
    int currentPhase, unlockedPhase;
    private int textScale = 1;
    private boolean contrast, nameConfirm, onGround, exitBlocked, campaignComplete, choosingContinue;
    boolean bucketFull;
    boolean facingRight = true, walking;
    int walkingFrame;
    double x = 110, y = 500, vy, camera, elapsed;
    int dialogueIndex;
    long toastUntil;
    long phaseLoadStartedAt;
    Question previousQuizFirst;
    long vegetationGeneration;

    public Game() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setLayout(null);
        setBackground(Color.BLACK);
        setOpaque(true);
        cinematicManager = new CinematicManager(OPENING_VIDEOS, OPENING_NARRATION, this::showToast, this::finishOpening);
        worldRenderer = new WorldRenderer(this);
        add(cinematicManager.panel());
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { layoutCinematicPanel(); }
        });
        layoutCinematicPanel();
        loadImages();
        buildLevel();
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { held.add(e.getKeyCode()); keyPressedGame(e.getKeyCode()); }
            @Override public void keyReleased(KeyEvent e) { held.remove(e.getKeyCode()); }
            @Override public void keyTyped(KeyEvent e) {
                if (screen == GameState.NAME && !nameConfirm && name.length() < 18) {
                    char c = e.getKeyChar();
                    if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') name += c;
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                Point point = toGameCoordinates(e.getX(), e.getY());
                updateMouseHover(point.x, point.y);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Point point = toGameCoordinates(e.getX(), e.getY());
                    handleMouseClick(point.x, point.y);
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
        for (Phase phase : Phase.values()) guardianImages[phase.ordinal()] = image(phase.portrait);
        BufferedImage backgroundSource = image("Image/Cenarios/FundoDoJogo.jpeg");
        if (backgroundSource != null) worldBackground = resizeImage(backgroundSource, WORLD_W, Math.round(WORLD_W * backgroundSource.getHeight() / (float) backgroundSource.getWidth()));
        treeImages[0] = loadTree("Image/Cenarios/Elementos/arvore01.png");
        treeImages[1] = loadTree("Image/Cenarios/Elementos/arvore02.png");
        treeImages[2] = loadTree("Image/Cenarios/Elementos/arvore03.png");
        BufferedImage groundSource = image("Image/Cenarios/Elementos/chao.png");
        if (groundSource != null) {
            int renderHeight = 120 + GROUND_OVERDRAW;
            groundTile = resizeImage(groundSource, Math.round(renderHeight * groundSource.getWidth() / (float) groundSource.getHeight()), renderHeight);
        }
        BufferedImage platformSource = image("Image/Cenarios/Elementos/plataforma.png");
        if (platformSource != null) platformImage = resizeImage(platformSource, 2400, Math.round(2400 * platformSource.getHeight() / (float) platformSource.getWidth()));
        menuBackground = image("Image/Cenarios/Menu/MENU.jpg");
        menuTitle = image("Image/Cenarios/Menu/Titulo.png");
        slotTitleImage = cropOpaqueImage(image("src/main/resources/Image/Cenarios/Menu/Slots/Escolha-Slot.png"));
        nameTitleImage = image("src/main/resources/Image/Cenarios/Menu/Slots/Como-chama.jpg");
        nameTitleArtwork = cropRegion(nameTitleImage, 0, 640,
            nameTitleImage == null ? 0 : nameTitleImage.getWidth(), 290);
        slotImages[0] = cropOpaqueImage(image("src/main/resources/Image/Cenarios/Menu/Slots/Slot-1.png"));
        slotImages[1] = cropOpaqueImage(image("src/main/resources/Image/Cenarios/Menu/Slots/Slot-2.png"));
        pauseTitleImage = cropOpaqueImage(image("src/main/resources/Image/Cenarios/Menu/Pausa/pausa.png"));
        pauseIndicatorImage = removeDarkBackground(image("src/main/resources/Image/Cenarios/Menu/Pausa/indicador.jpg"));
        settingsTitleImage = cropOpaqueImage(image("src/main/resources/Image/Cenarios/Menu/Configuracoes/configuracao.png"));
        menuImages[0] = image("Image/Cenarios/Menu/NovoGame.png");
        menuImages[1] = image("Image/Cenarios/Menu/Continuar.png");
        menuImages[2] = image("Image/Cenarios/Menu/Configuracoes.png");
        menuImages[3] = image("Image/Cenarios/Menu/Interrogacao.png");
        menuImages[4] = image("Image/Cenarios/Menu/Sair.png");
        for (int i = 0; i < menuImages.length; i++) menuOpaqueBounds[i] = opaqueBounds(menuImages[i]);
    }

    private BufferedImage resizeImage(BufferedImage source, int width, int height) {
        if (source == null || width < 1 || height < 1) return null;
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private BufferedImage loadTree(String path) {
        BufferedImage source = image(path);
        if (source == null) return null;
        int height = 360;
        int width = Math.round(height * source.getWidth() / (float) source.getHeight());
        return resizeImage(source, width, height);
    }

    private BufferedImage image(String path) {
        return AssetManager.image(path);
    }

    private BufferedImage cropRegion(BufferedImage image, int x, int y, int width, int height) {
        if (image == null || x < 0 || y < 0 || width <= 0 || height <= 0
            || x + width > image.getWidth() || y + height > image.getHeight()) return null;
        return image.getSubimage(x, y, width, height);
    }

    private Rectangle opaqueBounds(BufferedImage image) {
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

    private void buildLevel() {
        platforms.clear(); coins.clear(); enemies.clear(); fires.clear(); traps.clear(); trees.clear(); platformArtwork.clear();
        if (currentPhase == 1) {
            platforms.add(new Rectangle(0, 600, 710, 120));
            platforms.add(new Rectangle(820, 600, 610, 120));
            platforms.add(new Rectangle(1540, 600, 690, 120));
            platforms.add(new Rectangle(2370, 600, WORLD_W - 2370, 120));
        } else platforms.add(new Rectangle(0, 600, WORLD_W, 120));
        int[][] ledges = currentPhase == 1
            ? new int[][] { {300, 510, 150, 25}, {500, 455, 145, 25}, {720, 390, 150, 25}, {940, 470, 145, 25}, {1160, 405, 150, 25}, {1390, 335, 145, 25}, {1630, 420, 155, 25}, {1880, 350, 145, 25}, {2130, 450, 160, 25}, {2380, 380, 150, 25}, {2650, 315, 145, 25} }
            : new int[][] { {330, 500, 190, 28}, {640, 430, 190, 28}, {960, 520, 220, 28}, {1320, 450, 210, 28}, {1640, 370, 190, 28}, {1980, 500, 230, 28}, {2310, 420, 220, 28}, {2630, 520, 180, 28} };
        for (int[] p : ledges) platforms.add(new Rectangle(p[0], p[1], p[2], p[3]));
        if (currentPhase == 0) {
            int[] seedXs = { 360, 420, 675, 735, 1010, 1080, 1370, 1450, 1690, 1750, 2030, 2110, 2360, 2440, 2680, 2750, 2940 };
            for (int itemX : seedXs) coins.add(new Thing(itemX, 0));
            traps.add(new Thing(850, 560)); traps.add(new Thing(1770, 560)); traps.add(new Thing(2550, 560));
            enemies.add(new Thing(760, 0)); enemies.add(new Thing(1500, 0)); enemies.add(new Thing(2180, 0)); enemies.add(new Thing(2820, 0));
        } else if (currentPhase == 1) {
            enemies.add(new Thing(570, 0)); enemies.add(new Thing(1260, 0)); enemies.add(new Thing(2030, 0)); enemies.add(new Thing(2730, 0));
        } else if (currentPhase == 2) {
            fires.add(new Thing(690, 558)); fires.add(new Thing(1190, 558)); fires.add(new Thing(1800, 558)); fires.add(new Thing(2490, 558));
        } else {
            int[][] litter = { {360,552}, {610,538}, {835,560}, {1080,532}, {1320,550}, {1590,540}, {1880,562}, {2160,535}, {2440,550}, {2740,538}, {2960,558} };
            for (int[] item : litter) coins.add(new Thing(item[0], item[1]));
        }
        bucketFull = false;
        exitBlocked = false;
        preparePlatformArtwork();
        generateVegetation();
    }

    private void preparePlatformArtwork() {
        if (platformImage == null) return;
        for (Rectangle platform : platforms) {
            if (platform.y == GROUND_Y) continue;
            int height = Math.max(1, Math.round(platform.width * platformImage.getHeight() / (float) platformImage.getWidth()));
            platformArtwork.computeIfAbsent(platform.width, width -> resizeImage(platformImage, width, height));
        }
    }

    private void generateVegetation() {
        if (Arrays.stream(treeImages).allMatch(image -> image == null)) return;
        Random random = new Random(0x5EEDL + currentPhase * 997L + vegetationGeneration++ * 131L);
        int attempts = 0;
        while (trees.size() < 5 && attempts++ < 240) {
            BufferedImage source = treeImages[random.nextInt(treeImages.length)];
            if (source == null) continue;
            int height = 255 + random.nextInt(66);
            int width = Math.round(height * source.getWidth() / (float) source.getHeight());
            int centerX = 150 + random.nextInt(WORLD_W - 300);
            if (!treePositionIsClear(centerX, width)) continue;
            BufferedImage artwork = resizeImage(source, width, height);
            trees.add(new TreeDecoration(centerX - width / 2, GROUND_Y + 18 - height, artwork));
        }
    }

    private boolean treePositionIsClear(int centerX, int treeWidth) {
        int itemClearance = treeWidth / 2 + 28;
        for (List<Thing> items : List.of(coins, fires, traps, enemies)) {
            for (Thing item : items) if (Math.abs(centerX - item.x) < itemClearance) return false;
        }
        if (currentPhase == 2 && (Math.abs(centerX - 145) < itemClearance || Math.abs(centerX - 1650) < itemClearance)) return false;
        if (Math.abs(centerX - 110) < treeWidth / 2 + 70 || Math.abs(centerX - 3020) < treeWidth / 2 + 70) return false;
        for (TreeDecoration tree : trees) {
            int existingCenter = tree.x() + tree.image().getWidth() / 2;
            if (Math.abs(centerX - existingCenter) < (treeWidth + tree.image().getWidth()) / 2 + 80) return false;
        }
        return true;
    }

    private void tick() {
        double dt = 0.016;
        elapsed += dt;
        cinematicManager.setVisible(screen == GameState.CINEMATIC);
        updateMenuScale();
        if (screen == GameState.WORLD) updateWorld(dt);
        if (toastUntil < System.currentTimeMillis()) toast = "";
        repaint();
    }

    private void updateMenuScale() {
        int selected = screen == GameState.MENU ? menuIndex : -1;
        for (int i = 0; i < menuScale.length; i++) {
            double target = i == selected ? 1.08 : 1.0;
            menuScale[i] += (target - menuScale[i]) * 0.24;
            if (Math.abs(target - menuScale[i]) < 0.001) menuScale[i] = target;
        }
    }

    private void updateWorld(double dt) {
        double dx = (held.contains(KeyEvent.VK_D) || held.contains(KeyEvent.VK_RIGHT) ? 1 : 0) - (held.contains(KeyEvent.VK_A) || held.contains(KeyEvent.VK_LEFT) ? 1 : 0);
        walking = dx != 0;
        if (walking) { walkingFrame++; if (dx > 0) facingRight = true; else facingRight = false; }
        else walkingFrame = 0;
        x = clamp(x + dx * 260 * dt, 0, WORLD_W - PLAYER_WIDTH - 2);
        if ((held.contains(KeyEvent.VK_SPACE) || held.contains(KeyEvent.VK_W) || held.contains(KeyEvent.VK_UP)) && onGround) { vy = -590; onGround = false; }
        vy += 1350 * dt;
        double oldY = y;
        y += vy * dt;
        onGround = false;
        Rectangle body = new Rectangle((int)x + 2, (int)y, PLAYER_WIDTH, PLAYER_HEIGHT);
        for (Rectangle platform : platforms) {
            int surfaceY = platform.y == GROUND_Y ? GROUND_Y : platform.y + PLATFORM_SURFACE_OFFSET;
            boolean overlapsPlatform = body.x < platform.x + platform.width && body.x + body.width > platform.x;
            if (overlapsPlatform && oldY + PLAYER_HEIGHT <= surfaceY && y + PLAYER_HEIGHT >= surfaceY && vy >= 0) {
                y = surfaceY - PLAYER_HEIGHT; vy = 0; onGround = true; body.y = (int)y;
            }
        }
        if (y > H + 100) loseLife();
        for (Thing coin : coins) if (!coin.hit && body.intersects(new Rectangle(coin.x - 12, coinY(coin), 24, 24))) { coin.hit = true; coinsCollected++; }
        for (Thing enemy : enemies) {
            if (enemy.hit) continue;
            enemy.x += (int) Math.round(Math.sin(elapsed + enemy.x) * ENEMY_SPEED);
            Rectangle enemyRect = new Rectangle(enemy.x - 20, 552, 40, 48);
            if (body.intersects(enemyRect)) {
                if (vy > 0 && y + PLAYER_HEIGHT < 575) { enemy.hit = true; vy = -360; }
                else loseLife();
            }
        }
        camera = clamp(x - 340, 0, WORLD_W - W);
        if (x < 2980) exitBlocked = false;
        if (x > 3020 && !exitBlocked) {
            if (phaseObjectiveComplete()) beginGuardianDialogue();
            else {
                exitBlocked = true;
                showToast(exitHint());
                x = 3020;
            }
        }
    }

    private boolean phaseObjectiveComplete() {
        return switch (currentPhase) {
            case 0 -> coinsCollected >= 12 && traps.stream().allMatch(trap -> trap.hit);
            case 1 -> true;
            case 2 -> fires.stream().allMatch(fire -> fire.hit);
            case 3 -> coinsCollected >= 10;
            default -> false;
        };
    }

    private String exitHint() {
        return switch (currentPhase) {
            case 0 -> "Encontre 12 sementes e desative as 3 armadilhas com E.";
            case 2 -> "Apague todos os focos antes de seguir.";
            case 3 -> "Recolha 10 residuos antes de seguir.";
            default -> "Continue pela trilha.";
        };
    }

    private void beginGuardianDialogue() {
        Phase phase = Phase.values()[currentPhase];
        dialogue = switch (currentPhase) {
            case 0 -> new String[] { phase.guardian, "Você protegeu os rastros da fauna. A caça ilegal ameaça toda a floresta.", "Animais silvestres pertencem ao seu habitat. Denuncie a caça e nunca arme armadilhas.", "Agora, siga a travessia do Saci." };
            case 1 -> new String[] { phase.guardian, "Você venceu o percurso! Reduzir resíduos e respeitar os caminhos protege a vida.", "Cada escolha responsável ajuda a floresta a continuar de pé.", "A Caipora precisa de ajuda entre as cinzas." };
            case 2 -> new String[] { phase.guardian, "Os focos foram apagados. Queimadas destroem habitats e colocam vidas em risco.", "Nunca queime lixo ou vegetação. Avise os serviços de emergência ao ver fogo.", "O Negrinho procura os rastros do lixo." };
            default -> new String[] { phase.guardian, "A trilha está limpa. Resíduos na natureza ferem animais e contaminam água e solo.", "Reduza, reutilize e descarte corretamente. O cuidado de cada pessoa faz diferença.", "A campanha foi concluída!" };
        };
        dialogueIndex = 0;
        screen = GameState.DIALOGUE;
    }

    private void interactPhase() {
        int centerX = (int) x + 19;
        int centerY = (int) y + 29;
        if (currentPhase == 0) {
            for (Thing trap : traps) {
                if (!trap.hit && Math.abs(centerX - trap.x) < 76 && Math.abs(centerY - trap.y) < 90) {
                    trap.hit = true; showToast("Armadilha desativada. A trilha esta protegida."); return;
                }
            }
            showToast("Procure os sinais de armadilha na trilha."); return;
        }
        if (currentPhase != 2) return;
        boolean nearWater = Math.abs(centerX - 145) < 75 || Math.abs(centerX - 1650) < 75;
        if (nearWater) {
            bucketFull = true;
            showToast("Balde cheio. Leve a agua ate um foco ativo.");
            return;
        }
        for (Thing fire : fires) {
            if (!fire.hit && Math.abs(centerX - fire.x) < 75 && Math.abs(centerY - fire.y) < 90) {
                if (!bucketFull) showToast("O balde esta vazio. Volte a uma fonte de agua.");
                else { fire.hit = true; bucketFull = false; showToast("Foco apagado!"); }
                return;
            }
        }
        showToast("Procure uma fonte ou aproxime-se de um foco de incendio.");
    }

    private void loseLife() {
        lives--; x = 110; y = 500; vy = 0;
        if (lives <= 0) { lives = 3; coinsCollected = 0; buildLevel(); showToast("A floresta lhe deu outra chance."); }
    }

    private void keyPressedGame(int key) {
        if (screen == GameState.MENU) menuKey(key);
        else if (screen == GameState.SLOTS) slotsKey(key);
        else if (screen == GameState.SAVE_OPTIONS) saveOptionsKey(key);
        else if (screen == GameState.PHASE_SELECT) phaseSelectKey(key);
        else if (screen == GameState.SAVE_CONFIRM) saveConfirmKey(key);
        else if (screen == GameState.NAME) nameKey(key);
        else if (screen == GameState.CINEMATIC) { if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) cinematicManager.advance(); else if (key == KeyEvent.VK_ESCAPE) cinematicManager.skip(); }
        else if (screen == GameState.PHASE_INTRO) { if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) screen = GameState.WORLD; else if (key == KeyEvent.VK_ESCAPE) returnToMenu(); }
        else if (screen == GameState.WORLD) worldKey(key);
        else if (screen == GameState.PAUSE) pauseKey(key);
        else if (screen == GameState.SETTINGS) settingsKey(key);
        else if (screen == GameState.CONTROLS) { if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) screen = GameState.SETTINGS; }
        else if (screen == GameState.EXTRAS) { if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) returnToMenu(); }
        else if (screen == GameState.DIALOGUE) dialogueKey(key);
        else if (screen == GameState.BATTLE) battleKey(key);
        else if (screen == GameState.END) { if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) returnToMenu(); }
        else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_M) screen = screen == GameState.MAP ? GameState.WORLD : GameState.MENU;
    }

    private void menuKey(int key) {
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) menuIndex = nextMenuIndex(menuIndex, -1);
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) menuIndex = nextMenuIndex(menuIndex, 1);
        if ((key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) && menuIndex >= 0) {
            if (menuIndex == 1 && firstSave() < 0) return;
            if (menuIndex == 0 || menuIndex == 1) { choosingContinue = menuIndex == 1; slotIndex = choosingContinue && firstSave() >= 0 ? firstSave() : 0; screen = GameState.SLOTS; }
            else if (menuIndex == 2) { settingsReturn = GameState.MENU; screen = GameState.SETTINGS; }
            else if (menuIndex == 3) screen = GameState.EXTRAS;
            else System.exit(0);
        }
    }

    private int nextMenuIndex(int current, int direction) {
        int next = current < 0 ? (direction > 0 ? 0 : menuImages.length - 1)
            : Math.floorMod(current + direction, menuImages.length);
        if (!hasAnySave() && next == 1) next = Math.floorMod(next + direction, menuImages.length);
        return next;
    }

    private boolean hasAnySave() { return firstSave() >= 0; }

    private void slotsKey(int key) {
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) slotIndex = 1 - slotIndex;
        if (key == KeyEvent.VK_DELETE && saveManager.exists(slotIndex)) { confirmAction = ConfirmAction.DELETE_SAVE; confirmReturn = GameState.SLOTS; settingsIndex = 0; screen = GameState.SAVE_CONFIRM; }
        if (key == KeyEvent.VK_R && hasSave(slotIndex)) { confirmAction = ConfirmAction.NEW_CAMPAIGN; confirmReturn = GameState.SLOTS; settingsIndex = 0; screen = GameState.SAVE_CONFIRM; }
        if (key == KeyEvent.VK_ESCAPE) returnToMenu();
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            if (choosingContinue) {
                if (hasSave(slotIndex)) { selectedSaveSlot = slotIndex; saveOptionIndex = 0; screen = GameState.SAVE_OPTIONS; }
                else showToast("Este slot ainda nao possui uma campanha salva.");
            } else if (hasSave(slotIndex)) { confirmAction = ConfirmAction.NEW_CAMPAIGN; confirmReturn = GameState.SLOTS; settingsIndex = 0; screen = GameState.SAVE_CONFIRM; }
            else { name = ""; nameConfirm = false; screen = GameState.NAME; }
        }
    }

    private void saveOptionsKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = GameState.SLOTS; return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_W || key == KeyEvent.VK_A || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_S || key == KeyEvent.VK_D) saveOptionIndex = 1 - saveOptionIndex;
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            if (saveOptionIndex == 0) loadSave(selectedSaveSlot);
            else { Properties save = readSave(selectedSaveSlot); unlockedPhase = Math.max(0, Math.min(Phase.values().length - 1, intValue(save, "unlockedPhase", intValue(save, "phase", 0)))); phaseSelectIndex = 0; screen = GameState.PHASE_SELECT; }
        }
    }

    private void phaseSelectKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = GameState.SAVE_OPTIONS; return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_W || key == KeyEvent.VK_A) phaseSelectIndex = (phaseSelectIndex + Phase.values().length - 1) % Phase.values().length;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_S || key == KeyEvent.VK_D) phaseSelectIndex = (phaseSelectIndex + 1) % Phase.values().length;
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) selectSavedPhase(phaseSelectIndex);
    }

    private void selectSavedPhase(int phase) {
        Properties save = readSave(selectedSaveSlot);
        unlockedPhase = Math.max(0, Math.min(Phase.values().length - 1, intValue(save, "unlockedPhase", intValue(save, "phase", 0))));
        if (phase > unlockedPhase) { showToast("Essa fase ainda esta bloqueada."); return; }
        pendingPhase = phase; confirmAction = ConfirmAction.RESTART_PHASE; confirmReturn = GameState.PHASE_SELECT; settingsIndex = 0; screen = GameState.SAVE_CONFIRM;
    }

    private void startSavedPhase(int phase) {
        loadSave(selectedSaveSlot);
        currentPhase = phase;
        x = 110; y = 500; vy = 0; lives = 3; coinsCollected = 0; bucketFull = false; campaignComplete = false;
        buildLevel(); camera = 0; saveGame(); openPhaseIntro();
    }

    private void saveConfirmKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = confirmReturn; return; }
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_A || key == KeyEvent.VK_D) settingsIndex = 1 - settingsIndex;
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            if (settingsIndex == 0) {
                if (confirmAction == ConfirmAction.DELETE_SAVE) deleteSave(slotIndex);
                else if (confirmAction == ConfirmAction.RESTART_PHASE) startSavedPhase(pendingPhase);
                else { name = ""; nameConfirm = false; screen = GameState.NAME; }
            } else screen = confirmReturn;
            settingsIndex = 0;
        }
    }

    private void nameKey(int key) {
        if (key == KeyEvent.VK_BACK_SPACE && !nameConfirm && !name.isEmpty()) name = name.substring(0, name.length() - 1);
        else if (key == KeyEvent.VK_ESCAPE) screen = GameState.SLOTS;
        else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (nameConfirm) startGame(); else nameConfirm = true; }
    }

    private void worldKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { menuIndex = 0; screen = GameState.PAUSE; }
        else if (key == KeyEvent.VK_F5) saveGame();
        else if (key == KeyEvent.VK_M) screen = GameState.MAP;
        else if (key == KeyEvent.VK_E) interactPhase();
    }

    private void pauseKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = GameState.WORLD; return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) menuIndex = (menuIndex + 4) % 5;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) menuIndex = (menuIndex + 1) % 5;
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (menuIndex == 0) screen = GameState.WORLD; else if (menuIndex == 1) saveGame(); else if (menuIndex == 2) { settingsReturn = GameState.PAUSE; screen = GameState.SETTINGS; } else if (menuIndex == 3) screen = GameState.MAP; else { saveGame(); returnToMenu(); } }
    }

    private void returnToMenu() { screen = GameState.MENU; menuIndex = 0; choosingContinue = false; }

    private double viewportScale() {
        if (getWidth() <= 0 || getHeight() <= 0) return 1.0;
        return Math.min(getWidth() / (double) W, getHeight() / (double) H);
    }

    private int viewportX(double scale) { return (int) Math.round((getWidth() - W * scale) / 2.0); }
    private int viewportY(double scale) { return (int) Math.round((getHeight() - H * scale) / 2.0); }

    private Point toGameCoordinates(int x, int y) {
        double scale = viewportScale();
        return new Point((int) Math.floor((x - viewportX(scale)) / scale),
            (int) Math.floor((y - viewportY(scale)) / scale));
    }

    private void layoutCinematicPanel() {
        double scale = viewportScale();
        int x = viewportX(scale);
        int y = viewportY(scale);
        int width = (int) Math.round(W * scale);
        int height = (int) Math.round(H * scale);
        cinematicManager.panel().setBounds(x, y, width, height);
    }

    private boolean inside(int x, int y, int bx, int by, int bw, int bh) { return new Rectangle(bx, by, bw, bh).contains(x, y); }

    private void updateMouseHover(int x, int y) {
        int cursor = java.awt.Cursor.DEFAULT_CURSOR;
        if (screen == GameState.MENU) { int index = menuAt(x, y); if (index == 1 && !hasAnySave()) index = -1; menuIndex = index; if (index >= 0) cursor = java.awt.Cursor.HAND_CURSOR; }
        else if (screen == GameState.SLOTS) { if (slotCardAt(x, y) >= 0 || slotActionAt(x, y) >= 0) { int slot = slotCardAt(x, y); if (slot >= 0) slotIndex = slot; cursor=java.awt.Cursor.HAND_CURSOR; } }
        else if (screen == GameState.SAVE_OPTIONS) { saveOptionIndex = inside(x,y,300,275,680,72) ? 0 : inside(x,y,300,367,680,72) ? 1 : saveOptionIndex; if (inside(x,y,300,275,680,72)||inside(x,y,300,367,680,72)) cursor=java.awt.Cursor.HAND_CURSOR; }
        else if (screen == GameState.PHASE_SELECT) { for(int i=0;i<Phase.values().length;i++) if(inside(x,y,215,205+i*88,850,70)){phaseSelectIndex=i;cursor=java.awt.Cursor.HAND_CURSOR;break;} }
        else if (screen == GameState.SAVE_CONFIRM) { if(inside(x,y,370,395,230,64)){settingsIndex=0;cursor=java.awt.Cursor.HAND_CURSOR;}else if(inside(x,y,680,395,230,64)){settingsIndex=1;cursor=java.awt.Cursor.HAND_CURSOR;} }
        else if (screen == GameState.SETTINGS) { if (settingsRowAt(x, y) >= 0 || inside(x,y,440,575,400,45)) { int row=settingsRowAt(x,y); if(row>=0)settingsIndex=row; cursor=java.awt.Cursor.HAND_CURSOR; } }
        else if (screen == GameState.PAUSE) { int option = pauseOptionAt(x, y); if (option >= 0) { menuIndex = option; cursor=java.awt.Cursor.HAND_CURSOR; } }
        else if (screen == GameState.BATTLE) { int option = battleChoiceAt(x, y); if (option >= 0) { battleChoice = option; cursor=java.awt.Cursor.HAND_CURSOR; } }
        else if (screen == GameState.DIALOGUE || screen == GameState.PHASE_INTRO || screen == GameState.END || screen == GameState.EXTRAS || screen == GameState.CONTROLS) cursor=java.awt.Cursor.HAND_CURSOR;
        else if (screen == GameState.MAP) cursor=java.awt.Cursor.HAND_CURSOR;
        else if (screen == GameState.WORLD) { double worldX=x+camera; if(currentPhase==0&&traps.stream().anyMatch(t->!t.hit&&Math.abs(worldX-t.x)<42)||currentPhase==2&&(Math.abs(worldX-145)<38||Math.abs(worldX-1650)<38||fires.stream().anyMatch(f->!f.hit&&Math.abs(worldX-f.x)<42)))cursor=java.awt.Cursor.HAND_CURSOR; }
        setCursor(java.awt.Cursor.getPredefinedCursor(cursor)); repaint();
    }

    private void handleMouseClick(int x, int y) {
        switch (screen) {
            case MENU -> { int i=menuAt(x,y); if(i==1&&!hasAnySave()){menuIndex=-1;repaint();return;} if(i>=0){menuIndex=i;menuKey(KeyEvent.VK_ENTER);} }
            case SLOTS -> {
                int action = slotActionAt(x, y);
                if (action >= 0) {
                    slotIndex = action / 2;
                    boolean restart = action % 2 == 1;
                    if ((!restart && saveManager.exists(slotIndex)) || (restart && hasSave(slotIndex))) {
                        confirmAction = action % 2 == 0 ? ConfirmAction.DELETE_SAVE : ConfirmAction.NEW_CAMPAIGN;
                        confirmReturn = GameState.SLOTS; settingsIndex = 0; screen = GameState.SAVE_CONFIRM;
                    }
                } else {
                    int clickedSlot = slotCardAt(x, y);
                    if (clickedSlot < 0) return;
                    slotIndex = clickedSlot;
                    slotsKey(KeyEvent.VK_ENTER);
                }
            }
            case WORLD -> { double worldX=x+camera; boolean target=y>=510&&y<=610&&(currentPhase==0&&traps.stream().anyMatch(t->!t.hit&&Math.abs(worldX-t.x)<42)||currentPhase==2&&(Math.abs(worldX-145)<38||Math.abs(worldX-1650)<38||fires.stream().anyMatch(f->!f.hit&&Math.abs(worldX-f.x)<42)));if(target&&Math.abs(worldX-this.x)<110)interactPhase();else if(target)showToast("Aproxime-se do elemento para interagir com E ou clique."); }
            case SAVE_OPTIONS -> { if(inside(x,y,300,275,680,72)){saveOptionIndex=0;saveOptionsKey(KeyEvent.VK_ENTER);}else if(inside(x,y,300,367,680,72)){saveOptionIndex=1;saveOptionsKey(KeyEvent.VK_ENTER);}else if(inside(x,y,300,480,680,55))screen=GameState.SLOTS; }
            case PHASE_SELECT -> { for(int i=0;i<Phase.values().length;i++)if(inside(x,y,215,205+i*88,850,70)){phaseSelectIndex=i;selectSavedPhase(i);return;} if(inside(x,y,420,570,440,55))screen=GameState.SAVE_OPTIONS; }
            case SAVE_CONFIRM -> { if(inside(x,y,370,395,230,64)){settingsIndex=0;saveConfirmKey(KeyEvent.VK_ENTER);}else if(inside(x,y,680,395,230,64)){settingsIndex=1;saveConfirmKey(KeyEvent.VK_ENTER);} }
            case SETTINGS -> {
                int row = settingsRowAt(x, y);
                if (row >= 0) {
                    settingsIndex = row;
                    int arrow = settingsArrowAt(x, y);
                    if (row < 2 && arrow != 0) settingsKey(arrow < 0 ? KeyEvent.VK_LEFT : KeyEvent.VK_RIGHT);
                    else settingsKey(KeyEvent.VK_ENTER);
                } else if (inside(x,y,440,575,400,45)) screen=settingsReturn;
            }
            case CONTROLS -> screen=GameState.SETTINGS;
            case EXTRAS -> returnToMenu();
            case PAUSE -> { int option = pauseOptionAt(x, y); if (option >= 0) { menuIndex = option; pauseKey(KeyEvent.VK_ENTER); } }
            case MAP -> { if(inside(x,y,400,520,480,65))screen=GameState.WORLD; }
            case DIALOGUE -> { if(inside(x,y,100,490,1080,165))advanceDialogue(); }
            case BATTLE -> { int option = battleChoiceAt(x, y); if (option >= 0) { battleChoice = option; battleKey(KeyEvent.VK_ENTER); } }
            case CINEMATIC -> cinematicManager.advance();
            case PHASE_INTRO -> { if(inside(x,y,480,575,600,90))screen=GameState.WORLD; }
            case END -> { if(inside(x,y,430,475,600,100))returnToMenu(); }
            case NAME -> { if(inside(x,y,320,390,650,100)){nameConfirm=true;startGame();} }
            default -> { }
        }
        repaint();
    }

    private void settingsKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = settingsReturn; return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) settingsIndex = (settingsIndex + 2) % 3;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) settingsIndex = (settingsIndex + 1) % 3;
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            if (settingsIndex == 0) textScale = (textScale + (key == KeyEvent.VK_LEFT ? 2 : 1)) % 3;
            else if (settingsIndex == 1) contrast = !contrast;
            else if (key != KeyEvent.VK_LEFT) screen = GameState.CONTROLS;
        }
    }

    private int settingsRowAt(int x, int y) {
        if (x < 260 || x > 1020) return -1;
        for (int i = 0; i < 3; i++) {
            int rowY = 225 + i * 90;
            if (y >= rowY && y <= rowY + 62) return i;
        }
        return -1;
    }

    private int settingsArrowAt(int x, int y) {
        if (x >= 770 && x <= 820) return -1;
        if (x >= 920 && x <= 970) return 1;
        return 0;
    }

    private void dialogueKey(int key) { if (key == KeyEvent.VK_ESCAPE) screen = GameState.WORLD; else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_E) advanceDialogue(); }
    private void advanceDialogue() { if (++dialogueIndex >= dialogue.length) { startQuizRound(); screen = GameState.BATTLE; } }

    private void startQuizRound() {
        List<Question> shuffled = new ArrayList<>(Arrays.asList(QuestionBank.QUESTIONS[currentPhase]));
        Collections.shuffle(shuffled);
        if (shuffled.size() > 1 && shuffled.get(0) == previousQuizFirst) Collections.swap(shuffled, 0, 1);
        quizQuestions.clear();
        quizQuestions.addAll(shuffled.subList(0, Math.min(QUESTIONS_PER_ROUND, shuffled.size())));
        previousQuizFirst = quizQuestions.get(0);
        quizIndex = 0;
        battleChoice = 0;
    }

    private void battleKey(int key) {
        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_4) battleChoice = key - KeyEvent.VK_1;
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            Question question = quizQuestions.get(quizIndex);
            if (battleChoice == question.correctOption()) {
                if (++quizIndex >= quizQuestions.size()) completePhase();
                else { battleChoice = 0; showToast("Resposta correta!"); }
            } else {
                startQuizRound();
                showToast("Resposta incorreta! Questionario reiniciado.");
            }
        }
    }

    private void completePhase() {
        if (currentPhase < Phase.values().length - 1) {
            unlockedPhase = Math.max(unlockedPhase, currentPhase + 1);
            currentPhase++;
            x = 110; y = 500; vy = 0; lives = 3; coinsCollected = 0;
            buildLevel();
            openPhaseIntro();
        } else {
            unlockedPhase = Phase.values().length - 1;
            campaignComplete = true;
            screen = GameState.END;
        }
        saveGame();
    }

    private void startGame() {
        if (name.trim().isEmpty()) name = "Viajante";
        resetTransientMovement();
        currentPhase = 0; unlockedPhase = 0; campaignComplete = false;
        x = 110; y = 500; lives = 3; coinsCollected = 0;
        buildLevel(); saveGame(); startOpening();
    }

    private void resetTransientMovement() {
        held.clear();
        vy = 0;
        onGround = false;
        walking = false;
        walkingFrame = 0;
        exitBlocked = false;
    }
    private void startOpening() { screen = GameState.CINEMATIC; cinematicManager.start(); }

    private void finishOpening() {
        openPhaseIntro();
        repaint();
    }
    void drawText(Graphics2D g, String text, int x, int y, int size, Color color) { g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, size + textScale * 2))); g.setColor(color); g.drawString(text, x, y); }
    private Color ink() { return contrast ? Color.WHITE : new Color(25, 48, 51); }
    private Color panel() { return contrast ? new Color(12, 12, 12) : new Color(249, 235, 187); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = viewportScale();
        g.translate(viewportX(scale), viewportY(scale));
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (screen == GameState.SAVE_CONFIRM) { drawSaveConfirmationScreen(g); if (!toast.isEmpty()) { g.setColor(new Color(0, 0, 0, 190)); g.fillRoundRect(350, 645, 580, 42, 10, 10); drawText(g, toast, 380, 673, 16, Color.WHITE); } g.dispose(); return; }
        if (screen == GameState.MENU) drawMenu(g); else if (screen == GameState.SLOTS) drawSlots(g); else if (screen == GameState.SAVE_OPTIONS) { drawMenu(g); drawPopupDim(g); drawSaveOptions(g); } else if (screen == GameState.PHASE_SELECT) { drawMenu(g); drawPopupDim(g); drawPhaseSelect(g); } else if (screen == GameState.SAVE_CONFIRM) { if(confirmReturn==GameState.PHASE_SELECT){drawMenu(g);drawPopupDim(g);drawPhaseSelect(g);}else drawSlots(g); drawPopupDim(g); drawSaveConfirmation(g); } else if (screen == GameState.NAME) drawName(g); else if (screen == GameState.PHASE_INTRO) drawPhaseIntro(g); else if (screen == GameState.BATTLE) { drawImageCover(g, menuBackground, 0, 0, W, H); g.setColor(new Color(0, 0, 0, 82)); g.fillRect(0, 0, W, H); drawBattle(g); } else if (screen == GameState.SETTINGS) { drawSettingsUnderlay(g); drawPopupDim(g); drawSettings(g); } else if (screen == GameState.CONTROLS) { drawSettingsUnderlay(g); drawPopupDim(g); drawControls(g); } else if (screen == GameState.EXTRAS) { drawMenu(g); drawPopupDim(g); drawExtras(g); } else { drawWorld(g); if (screen == GameState.PAUSE) drawPause(g); else if (screen == GameState.MAP) drawMap(g); else if (screen == GameState.DIALOGUE) drawDialogue(g); else if (screen == GameState.END) drawEnd(g); }
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
            int width = i == 3 ? 150 : 300;
            int height = width / 2;
            int drawX = i == 3 ? 1080 : 38;
            int row = i == 4 ? 3 : i;
            int drawY = i == 3 ? 590 : 215 + row * 72;
            double scale = width / (double) button.getWidth();
            Rectangle opaque = menuOpaqueBounds[i];
            menuBounds[i] = new Rectangle(drawX + (int) (opaque.x * scale), drawY + (int) (opaque.y * scale),
                Math.max(1, (int) (opaque.width * scale)), Math.max(1, (int) (opaque.height * scale)));
            int selectedWidth = (int) Math.round(width * menuScale[i]);
            int selectedHeight = (int) Math.round(height * menuScale[i]);
            int selectedX = drawX - (selectedWidth - width) / 2;
            int selectedY = drawY - (selectedHeight - height) / 2;
            Composite previousComposite = g.getComposite();
            if (i == 1 && !hasAnySave()) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            }
            g.drawImage(button, selectedX, selectedY, selectedWidth, selectedHeight, this);
            g.setComposite(previousComposite);
        }
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
    private void drawSlots(Graphics2D g) {
        drawImageCover(g, menuBackground, 0, 0, W, H);
        g.setColor(new Color(0, 0, 0, 86)); g.fillRect(0, 0, W, H);
        if (slotTitleImage != null) drawImageContentFit(g, slotTitleImage, 260, 30, 760, 120);
        else drawText(g, "ESCOLHA UM SLOT", 440, 145, 36, Color.WHITE);
        for (int i = 0; i < 2; i++) {
            int cardX = slotCardX(i), cardY = 220, cardW = 450, cardH = 340;
            g.setColor(new Color(0, 0, 0, i == slotIndex ? 194 : 166));
            g.fillRoundRect(cardX, cardY, cardW, cardH, 20, 20);
            g.setColor(i == slotIndex ? new Color(255, 216, 92, 220) : new Color(224, 203, 150, 115));
            g.setStroke(new java.awt.BasicStroke(i == slotIndex ? 3 : 1));
            g.drawRoundRect(cardX, cardY, cardW, cardH, 20, 20);
            g.setStroke(new java.awt.BasicStroke(1));
            if (slotImages[i] != null) drawImageContentFit(g, slotImages[i], cardX + 120, cardY + 8, 210, 105);
            boolean saved = hasSave(i);
            boolean saveFileExists = saveManager.exists(i);
            Properties save = saved ? readSave(i) : new Properties();
            String playerName = saved ? save.getProperty("name", "Viajante") : saveFileExists ? "Save inválido" : "Slot vazio";
            String phaseName = saved ? savePhaseTitle(save) : saveFileExists ? "Exclua o arquivo inválido" : choosingContinue ? "Sem campanha salva" : "Novo jogo";
            drawCenteredText(g, playerName, cardX + cardW / 2, cardY + 165, 22, Color.WHITE);
            drawCenteredText(g, phaseName, cardX + cardW / 2, cardY + 198, 14, new Color(245, 230, 190));
            drawSlotAction(g, i, false, cardX + 30, cardY + 238, saveFileExists);
            drawSlotAction(g, i, true, cardX + 245, cardY + 238, saved);
        }
        drawText(g, "A/D escolhem   Enter abre   Delete exclui   R reinicia   Esc volta", 335, 665, 15, Color.WHITE);
    }

    private void drawSlotAction(Graphics2D g, int slot, boolean restart, int x, int y, boolean enabled) {
        g.setColor(enabled ? new Color(255, 255, 255, 34) : new Color(255, 255, 255, 16));
        g.fillRoundRect(x, y, 175, 62, 10, 10);
        g.setColor(enabled ? new Color(245, 230, 190) : new Color(155, 155, 145));
        if (restart) {
            g.setStroke(new java.awt.BasicStroke(3));
            g.drawArc(x + 13, y + 17, 24, 24, 35, 285);
            g.drawLine(x + 32, y + 14, x + 40, y + 15);
            g.drawLine(x + 39, y + 15, x + 37, y + 23);
            g.setStroke(new java.awt.BasicStroke(1));
            drawText(g, "Reiniciar", x + 48, y + 39, 14, enabled ? Color.WHITE : new Color(155, 155, 145));
        } else {
            g.setStroke(new java.awt.BasicStroke(3));
            g.drawLine(x + 14, y + 20, x + 36, y + 20);
            g.drawLine(x + 19, y + 16, x + 32, y + 16);
            g.drawRoundRect(x + 18, y + 22, 17, 23, 3, 3);
            g.drawLine(x + 23, y + 26, x + 23, y + 41);
            g.drawLine(x + 30, y + 26, x + 30, y + 41);
            g.setStroke(new java.awt.BasicStroke(1));
            drawText(g, "Excluir", x + 48, y + 39, 14, enabled ? Color.WHITE : new Color(155, 155, 145));
        }
    }

    private void drawCenteredText(Graphics2D g, String text, int centerX, int y, int size, Color color) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, size + textScale * 2)));
        g.setColor(color);
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, y);
    }

    private int slotCardX(int slot) { return slot == 0 ? 130 : 700; }
    private int slotCardAt(int x, int y) {
        if (y < 220 || y > 560) return -1;
        for (int slot = 0; slot < 2; slot++) if (x >= slotCardX(slot) && x <= slotCardX(slot) + 450) return slot;
        return -1;
    }
    private int slotActionAt(int x, int y) {
        if (y < 458 || y > 520) return -1;
        for (int slot = 0; slot < 2; slot++) {
            int cardX = slotCardX(slot);
            if (x >= cardX + 30 && x <= cardX + 205) return slot * 2;
            if (x >= cardX + 245 && x <= cardX + 420) return slot * 2 + 1;
        }
        return -1;
    }

    private String savePhaseTitle(Properties save) {
        int phase = Math.max(0, Math.min(Phase.values().length - 1, intValue(save, "phase", 0)));
        return Phase.values()[phase].title;
    }
    private void drawName(Graphics2D g) {
        drawImageCover(g, menuBackground, 0, 0, W, H);
        g.setColor(new Color(0, 0, 0, 92)); g.fillRect(0, 0, W, H);
        int panelX = 300, panelY = 155, panelW = 680, panelH = 430;
        g.setColor(new Color(0, 0, 0, 218)); g.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        if (nameTitleArtwork != null) {
            drawImageFit(g, nameTitleArtwork, panelX + 42, panelY + 38, panelW - 84, 115);
        } else drawCenteredText(g, "COMO TE CHAMAM?", W / 2, panelY + 112, 34, new Color(255, 216, 92));
        int fieldX = panelX + 85, fieldY = panelY + 220, fieldW = panelW - 170, fieldH = 68;
        g.setColor(Color.WHITE); g.fillRoundRect(fieldX, fieldY, fieldW, fieldH, 9, 9);
        g.setColor(nameConfirm ? new Color(93, 145, 86) : new Color(213, 179, 105));
        g.setStroke(new java.awt.BasicStroke(3)); g.drawRoundRect(fieldX, fieldY, fieldW, fieldH, 9, 9); g.setStroke(new java.awt.BasicStroke(1));
        String shownName = name + (System.currentTimeMillis() / 400 % 2 == 0 ? "_" : "");
        drawCenteredText(g, shownName, W / 2, fieldY + 44, 25, new Color(25, 48, 51));
        drawCenteredText(g, "Digite seu apelido e precione enter", W / 2, panelY + 345, 17, Color.WHITE);
    }

    private void drawImageContentFit(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
        drawImageFit(g, image, x, y, width, height);
    }

    private BufferedImage removeDarkBackground(BufferedImage source) {
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

    private BufferedImage cropOpaqueImage(BufferedImage image) {
        if (image == null) return null;
        Rectangle content = opaqueBounds(image);
        if (content == null || content.isEmpty()) return image;
        return image.getSubimage(content.x, content.y, content.width, content.height);
    }
    private void openPhaseIntro() {
        phaseLoadStartedAt = System.currentTimeMillis();
        screen = GameState.PHASE_INTRO;
    }

    private void drawPhaseIntro(Graphics2D g) {
        drawImageCover(g, menuBackground, 0, 0, W, H);
        g.setColor(new Color(0, 0, 0, 88)); g.fillRect(0, 0, W, H);
        int panelX = 70, panelY = 55, panelW = 1140, panelH = 610;
        g.setColor(new Color(0, 0, 0, 170)); g.fillRoundRect(panelX, panelY, panelW, panelH, 18, 18);
        g.setColor(new Color(214, 177, 91, 180)); g.setStroke(new java.awt.BasicStroke(2));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 18, 18); g.setStroke(new java.awt.BasicStroke(1));

        Phase phase = Phase.values()[currentPhase];
        drawText(g, "CARREGAMENTO  •  GUARDIÃO " + (currentPhase + 1) + " DE " + Phase.values().length,
            530, 105, 15, new Color(202, 220, 194));
        drawText(g, phase.title, 530, 158, 27, new Color(255, 216, 92));
        drawText(g, phase.guardian + "  |  " + phase.theme, 530, 205, 22, Color.WHITE);

        g.setColor(new Color(42, 83, 65, 155)); g.fillOval(115, 155, 355, 390);
        BufferedImage portrait = guardianImages[currentPhase];
        if (portrait != null) drawImageFit(g, portrait, 120, 155, 350, 400);
        drawCenteredText(g, phase.guardian.toUpperCase(), 292, 585, 18, new Color(255, 226, 125));

        drawParagraph(g, phase.legend, 530, 260, 610, 28, new Color(227, 238, 219), 19);
        g.setColor(new Color(214, 177, 91, 130)); g.drawLine(530, 430, 1140, 430);
        drawText(g, "OBJETIVO DA JORNADA", 530, 467, 16, new Color(255, 216, 92));
        drawParagraph(g, phaseObjectiveText(currentPhase), 530, 502, 610, 26, new Color(255, 242, 202), 18);

        int progress = (int) Math.min(100, (System.currentTimeMillis() - phaseLoadStartedAt) / 24);
        g.setColor(new Color(255, 255, 255, 45)); g.fillRoundRect(530, 558, 610, 12, 8, 8);
        g.setColor(new Color(255, 216, 92)); g.fillRoundRect(530, 558, 610 * progress / 100, 12, 8, 8);
        drawText(g, progress < 100 ? "Preparando a trilha... " + progress + "%" : "Trilha pronta!",
            530, 601, 15, new Color(220, 237, 213));
        drawText(g, "Enter para iniciar a fase", 870, 620, 17, Color.WHITE);
    }

    private String phaseObjectiveText(int phase) {
        return switch (phase) {
            case 0 -> "Investigue a trilha: encontre 12 sementes e aproxime-se das 3 armadilhas; pressione E para desativá-las.";
            case 1 -> "Supere plataformas e obstáculos até encontrar o Saci.";
            case 2 -> "Use E nas fontes e nos focos. Encha o balde e apague todos os incêndios.";
            default -> "Recolha 10 resíduos e leve-os até o fim da trilha.";
        };
    }

    private void drawParagraph(Graphics2D g, String text, int x, int y, int width, int lineHeight, Color color, int fontSize) {
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize + textScale));
        g.setColor(color);
        String line = "";
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > width && !line.isEmpty()) {
                g.drawString(line, x, y); y += lineHeight; line = word;
            } else line = candidate;
        }
        if (!line.isEmpty()) g.drawString(line, x, y);
    }
    private void drawSettingsUnderlay(Graphics2D g) {
        if (settingsReturn == GameState.PAUSE) { drawWorld(g); drawPause(g); }
        else drawMenu(g);
    }
    private void drawPopupDim(Graphics2D g) { g.setColor(new Color(0, 0, 0, 145)); g.fillRect(0, 0, W, H); }
    private void drawSaveConfirmationScreen(Graphics2D g) { if(confirmReturn==GameState.PHASE_SELECT){drawMenu(g);drawPopupDim(g);drawPhaseSelect(g);}else{drawSlots(g);drawPopupDim(g);}drawSaveConfirmation(g); }
    private void drawSettings(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 178)); g.fillRoundRect(170, 65, 940, 585, 18, 18);
        g.setColor(new Color(214, 177, 91, 190)); g.setStroke(new java.awt.BasicStroke(2));
        g.drawRoundRect(170, 65, 940, 585, 18, 18); g.setStroke(new java.awt.BasicStroke(1));
        if (settingsTitleImage != null) drawImageContentFit(g, settingsTitleImage, 370, 88, 540, 90);
        else drawCenteredText(g, "CONFIGURACOES", W / 2, 150, 34, new Color(255, 216, 92));

        String[] labels = { "Tamanho do texto", "Alto contraste", "Controles" };
        String[] values = { textScale == 0 ? "Compacto" : textScale == 1 ? "Padrão" : "Ampliado", contrast ? "Ligado" : "Desligado", "Abrir" };
        for (int i = 0; i < labels.length; i++) {
            int rowY = 225 + i * 90;
            g.setColor(i == settingsIndex ? new Color(76, 55, 35, 218) : new Color(8, 30, 27, 205));
            g.fillRoundRect(260, rowY, 760, 62, 10, 10);
            g.setColor(i == settingsIndex ? new Color(255, 216, 92, 185) : new Color(210, 181, 112, 90));
            g.drawRoundRect(260, rowY, 760, 62, 10, 10);
            drawText(g, labels[i], 295, rowY + 39, 19, Color.WHITE);
            drawSettingArrow(g, 785, rowY + 17, 28, 30, true);
            drawCenteredText(g, values[i], 870, rowY + 39, 18, new Color(255, 239, 194));
            drawSettingArrow(g, 945, rowY + 17, 28, 30, false);
        }
        drawCenteredText(g, "Esc fecha", W / 2, 605, 16, new Color(231, 224, 194));
    }

    private void drawSettingArrow(Graphics2D g, int x, int y, int width, int height, boolean left) {
        if (pauseIndicatorImage == null) {
            drawText(g, left ? "<" : ">", x, y + height - 3, 24, new Color(255, 216, 92));
        } else if (left) {
            g.drawImage(pauseIndicatorImage, x + width, y, x, y + height,
                0, 0, pauseIndicatorImage.getWidth(), pauseIndicatorImage.getHeight(), this);
        } else {
            drawImageFit(g, pauseIndicatorImage, x, y, width, height);
        }
    }
    private void drawControls(Graphics2D g) { g.setColor(new Color(0, 0, 0, 178)); g.fillRoundRect(250, 130, 780, 470, 18, 18); g.setColor(new Color(214,177,91,190)); g.drawRoundRect(250,130,780,470,18,18); drawText(g,"CONTROLES",520,205,29,new Color(255,216,92)); drawText(g,"A/D ou SETAS: correr",390,300,21,Color.WHITE); drawText(g,"W, SETA CIMA ou ESPACO: pular",390,355,21,Color.WHITE); drawText(g,"M: mapa   F5: salvar   Esc: pausar",390,410,20,Color.WHITE); drawCenteredText(g,"Clique ou pressione Esc para voltar",W/2,535,16,new Color(245,230,190)); }
    private void drawExtras(Graphics2D g) { g.setColor(new Color(18, 43, 38, 238)); g.fillRoundRect(270, 190, 740, 350, 16, 16); g.setColor(new Color(213,179,105));g.drawRoundRect(270,190,740,350,16,16);drawText(g, "AJUDA E INFORMACOES", 420, 250, 30, new Color(255,216,92));drawParagraph(g, "Uma aventura inspirada nas lendas da floresta brasileira. Proteja a mata, respeite os animais e siga as orientacoes de cada guardiao.", 350, 320, 580, 30, Color.WHITE, 20);drawText(g, "Clique fora ou pressione Esc para voltar", 410, 480, 16, new Color(245,230,190)); }

    private void drawSaveOptions(Graphics2D g) {
        drawStandardPanel(g, 250, 165, 780, 390);
        drawCenteredText(g, "CONTINUAR CAMPANHA", W / 2, 235, 30, new Color(255, 216, 92));
        String[] labels = { "Continuar de onde parou", "Escolher fase desbloqueada" };
        for (int i = 0; i < labels.length; i++) {
            int rowY = 275 + i * 92;
            drawStandardOption(g, 300, rowY, 680, 72, i == saveOptionIndex);
            drawText(g, labels[i], 345, rowY + 45, 21, Color.WHITE);
        }
        drawCenteredText(g, "Esc volta aos saves", W / 2, 510, 16, new Color(205, 225, 195));
    }
    private void drawPhaseSelect(Graphics2D g) { g.setColor(new Color(18,43,38,220));g.fillRoundRect(190,105,900,520,16,16);drawText(g,"SELECIONAR FASE",480,155,29,new Color(255,216,92));for(int i=0;i<Phase.values().length;i++){int y=205+i*88;boolean available=i<=unlockedPhase;g.setColor(i==phaseSelectIndex?new Color(184,91,57):new Color(31,69,56));if(!available)g.setColor(new Color(55,65,58));g.fillRoundRect(215,y,850,70,10,10);drawText(g,Phase.values()[i].title,250,y+31,17,available?Color.WHITE:new Color(175,175,160));drawText(g,available?"DESBLOQUEADA":"BLOQUEADA",805,y+53,13,new Color(255,222,153));}drawText(g,"Voltar",590,604,17,Color.WHITE);}
    private void drawSaveConfirmation(Graphics2D g) {
        drawStandardPanel(g, 275, 190, 730, 340);
        String title = confirmAction == ConfirmAction.DELETE_SAVE ? "APAGAR ESTE SAVE?"
            : confirmAction == ConfirmAction.RESTART_PHASE ? "REINICIAR NESTA FASE?" : "COMEÇAR NOVA CAMPANHA?";
        drawCenteredText(g, title, W / 2, 260, 28, new Color(255, 216, 92));
        String message = confirmAction == ConfirmAction.DELETE_SAVE ? "O progresso deste slot será removido."
            : confirmAction == ConfirmAction.RESTART_PHASE ? "O progresso atual será substituído pelo início da fase escolhida."
            : "O progresso salvo neste slot será substituído.";
        drawParagraph(g, message, 365, 315, 550, 28, Color.WHITE, 19);
        String[] labels = confirmAction == ConfirmAction.DELETE_SAVE ? new String[] { "Apagar", "Cancelar" }
            : confirmAction == ConfirmAction.RESTART_PHASE ? new String[] { "Reiniciar", "Cancelar" }
            : new String[] { "Continuar", "Cancelar" };
        for (int i = 0; i < labels.length; i++) {
            int x = 370 + i * 310;
            drawStandardOption(g, x, 395, 230, 64, settingsIndex == i);
            drawCenteredText(g, labels[i], x + 115, 435, 18, Color.WHITE);
        }
    }

    private void drawStandardPanel(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(0, 0, 0, 178));
        g.fillRoundRect(x, y, width, height, 18, 18);
        g.setColor(new Color(214, 177, 91, 190));
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawRoundRect(x, y, width, height, 18, 18);
        g.setStroke(new java.awt.BasicStroke(1));
    }

    private void drawStandardOption(Graphics2D g, int x, int y, int width, int height, boolean selected) {
        g.setColor(selected ? new Color(184, 91, 57, 235) : new Color(13, 45, 39, 225));
        g.fillRoundRect(x, y, width, height, 12, 12);
        g.setColor(selected ? new Color(255, 216, 92, 210) : new Color(214, 177, 91, 105));
        g.drawRoundRect(x, y, width, height, 12, 12);
    }

    private void drawWorld(Graphics2D g) {
        worldRenderer.draw(g);
    }

    private void drawGround(Graphics2D g, Rectangle collision) {
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
            g.drawImage(groundTile, tileX, artY, this);
        g.setClip(previousClip);
    }

    private void drawPlatform(Graphics2D g, Rectangle collision) {
        BufferedImage artwork = platformArtwork.get(collision.width);
        if (artwork != null) g.drawImage(artwork, collision.x, collision.y, this);
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
        int flicker = (int) (Math.sin(elapsed * 12 + fireX) * 5);
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
        int itemY = coinY(item);
        if (currentPhase == 3) {
            g.setColor(new Color(74, 98, 57)); g.fillRoundRect(item.x - 11, itemY, 22, 18, 4, 4);
            g.setColor(new Color(218, 199, 151)); g.fillRect(item.x - 8, itemY + 4, 16, 10);
            g.setColor(new Color(129, 180, 91)); g.drawRect(item.x - 11, itemY, 22, 18);
        } else {
            g.setColor(new Color(255, 215, 55)); g.fillOval(item.x - 11, itemY, 22, 22);
            g.setColor(new Color(255, 246, 151)); g.fillOval(item.x - 5, itemY + 4, 5, 7);
        }
    }

    private int coinY(Thing coin) {
        return currentPhase == 3 ? coin.y : (int) (420 + Math.sin(elapsed * 3 + coin.x) * 30);
    }
    private void drawPlayer(Graphics2D g) { int px = (int)x, py = (int)y + 5; Image image = walking ? ((walkingFrame / 8) % 2 == 0 ? playerWalk1 : playerWalk2) : playerImage; if (image != null) { if (facingRight) g.drawImage(image, px - 8, py - 20, 58, 78, this); else g.drawImage(image, px + 50, py - 20, px - 8, py + 58, 0, 0, image.getWidth(this), image.getHeight(this), this); } else { g.setColor(new Color(217, 92, 70)); g.fillOval(px, py, 38, 38); g.setColor(new Color(242, 177, 139)); g.fillOval(px + 5, py - 17, 28, 28); } }
    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 120)); g.fillRect(0, 0, W, H);
        g.setColor(new Color(0, 0, 0, 145)); g.fillRoundRect(310, 130, 660, 470, 18, 18);
        if (pauseTitleImage != null) drawImageFit(g, pauseTitleImage, 490, 158, 300, 76);
        String[] options = { "Continuar", "Salvar", "Configuracoes", "Mapa", "Sair para o menu" };
        for (int i = 0; i < options.length; i++) {
            int rowY = 275 + i * 58;
            if (i == menuIndex) {
                g.setColor(new Color(255, 255, 255, 24));
                g.fillRoundRect(405, rowY, 470, 50, 10, 10);
                if (pauseIndicatorImage != null) drawImageFit(g, pauseIndicatorImage, 423, rowY + 5, 42, 40);
            }
            drawText(g, options[i], 485, rowY + 33, 21, Color.WHITE);
        }
    }

    private int pauseOptionAt(int x, int y) {
        if (x < 405 || x > 875) return -1;
        for (int i = 0; i < 5; i++) {
            int rowY = 275 + i * 58;
            if (y >= rowY && y <= rowY + 50) return i;
        }
        return -1;
    }
    private void drawMap(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 62)); g.fillRect(0, 0, W, H);
        drawStandardPanel(g, 70, 95, 1140, 530);
        drawCenteredText(g, "JORNADA DOS GUARDIÕES", W / 2, 160, 32, new Color(255, 216, 92));
        for (int i = 0; i < Phase.values().length; i++) {
            int cardX = 105 + i * 275;
            boolean complete = i < currentPhase || campaignComplete;
            boolean available = i <= unlockedPhase;
            g.setColor(complete ? new Color(39, 105, 65, 235) : available ? new Color(147, 91, 39, 235) : new Color(34, 53, 50, 235));
            g.fillRoundRect(cardX, 280, 240, 145, 12, 12);
            g.setColor(complete || available ? new Color(255, 216, 92, 190) : new Color(214, 177, 91, 90));
            g.drawRoundRect(cardX, 280, 240, 145, 12, 12);
            BufferedImage portrait = guardianImages[i];
            if (portrait != null) drawImageFit(g, portrait, cardX + 78, 188, 84, 82);
            drawText(g, "FASE " + (i + 1), cardX + 78, 318, 15, Color.WHITE);
            drawText(g, Phase.values()[i].guardian, cardX + 20, 357, i == 3 ? 17 : 20, Color.WHITE);
            drawText(g, complete ? "CONCLUÍDA" : available ? "DESBLOQUEADA" : "BLOQUEADA", cardX + 30, 397, 13, new Color(255, 230, 160));
            if (i < Phase.values().length - 1) { g.setColor(new Color(255, 216, 92)); g.fillRect(cardX + 240, 347, 35, 4); }
        }
        drawCenteredText(g, "M ou Esc para voltar", W / 2, 570, 17, new Color(205, 225, 195));
    }
    private void drawDialogue(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 48)); g.fillRect(0, 0, W, H);
        drawStandardPanel(g, 60, 455, 1160, 205);
        BufferedImage portrait = guardianImages[currentPhase];
        if (portrait != null) drawImageFit(g, portrait, 88, 410, 165, 190);
        drawText(g, Phase.values()[currentPhase].guardian, 275, 510, 23, new Color(255, 216, 92));
        drawParagraph(g, dialogue[dialogueIndex], 275, 552, 865, 27, Color.WHITE, 19);
        drawText(g, "Enter / E / Espaço para continuar", 820, 632, 15, new Color(205, 225, 195));
    }
    private void drawBattle(Graphics2D g) {
        Question question = quizQuestions.get(quizIndex);
        g.setColor(new Color(0, 0, 0, 174)); g.fillRoundRect(75, 52, 1130, 616, 18, 18);
        g.setColor(new Color(214, 177, 91, 175)); g.setStroke(new java.awt.BasicStroke(2));
        g.drawRoundRect(75, 52, 1130, 616, 18, 18); g.setStroke(new java.awt.BasicStroke(1));
        drawText(g, "DESAFIO  •  " + Phase.values()[currentPhase].guardian.toUpperCase(), 130, 120, 27, new Color(255, 216, 92));
        drawText(g, "PERGUNTA " + (quizIndex + 1) + "/" + quizQuestions.size(), 970, 120, 17, new Color(255, 230, 160));
        g.setColor(new Color(214, 177, 91, 120)); g.drawLine(130, 150, 1150, 150);
        drawParagraph(g, question.prompt(), 135, 210, 1010, 34, Color.WHITE, 22);
        for (int i = 0; i < question.options().length; i++) {
            int rowY = 310 + i * 66;
            if (i == battleChoice) {
                g.setColor(new Color(255, 255, 255, 28)); g.fillRoundRect(135, rowY, 1010, 54, 10, 10);
                g.setColor(new Color(255, 216, 92, 190)); g.drawRoundRect(135, rowY, 1010, 54, 10, 10);
                if (pauseIndicatorImage != null) drawImageFit(g, pauseIndicatorImage, 151, rowY + 7, 40, 40);
            }
            drawText(g, (i + 1) + "  |  " + question.options()[i], 210, rowY + 35, 18,
                i == battleChoice ? new Color(255, 226, 125) : Color.WHITE);
        }
        drawText(g, "Setas ou 1-4 selecionam   •   Enter confirma", 390, 630, 16, new Color(205, 225, 195));
    }

    private int battleChoiceAt(int x, int y) {
        if (x < 135 || x > 1145) return -1;
        for (int i = 0; i < 4; i++) {
            int rowY = 310 + i * 66;
            if (y >= rowY && y <= rowY + 54) return i;
        }
        return -1;
    }
    private void drawEnd(Graphics2D g) { drawBackdrop(g); BufferedImage portrait = guardianImages[Phase.NEGRINHO.ordinal()]; if (portrait != null) drawImageFit(g, portrait, 170, 220, 270, 330); drawText(g, "CAMPANHA CONCLUIDA", 500, 230, 38, new Color(255, 216, 92)); drawParagraph(g, "Voce acompanhou Curupira, Saci, Caipora e Negrinho do Pastoreio e ajudou a proteger a floresta.", 500, 300, 570, 30, Color.WHITE, 22); drawText(g, "Enter volta ao menu", 500, 520, 18, new Color(245, 230, 190)); }

    private int firstSave() { return hasSave(0) ? 0 : hasSave(1) ? 1 : -1; }
    private boolean hasSave(int slot) { return saveManager.hasSave(slot); }
    private String saveSummary(int slot) { Properties p = readSave(slot); int phase = Math.max(0, Math.min(Phase.values().length - 1, intValue(p, "phase", 0))); return p.getProperty("name", "Viajante") + " - " + Phase.values()[phase].title; }
    private Properties readSave(int slot) { return saveManager.read(slot); }
    private void saveGame() {
        Properties p = new Properties();
        p.setProperty("name", name); p.setProperty("coins", String.valueOf(coinsCollected));
        p.setProperty("lives", String.valueOf(lives)); p.setProperty("x", String.valueOf(x)); p.setProperty("y", String.valueOf(y));
        p.setProperty("phase", String.valueOf(currentPhase)); p.setProperty("unlockedPhase", String.valueOf(unlockedPhase));
        p.setProperty("coinMask", Long.toHexString(mask(coins))); p.setProperty("fireMask", Long.toHexString(mask(fires))); p.setProperty("trapMask", Long.toHexString(mask(traps)));
        p.setProperty("bucketFull", String.valueOf(bucketFull)); p.setProperty("campaignComplete", String.valueOf(campaignComplete));
        if (!saveManager.write(slotIndex, p)) showToast("Nao foi possivel salvar a campanha.");
    }

    private long mask(List<Thing> items) {
        long result = 0;
        for (int i = 0; i < items.size() && i < Long.SIZE - 1; i++) if (items.get(i).hit) result |= 1L << i;
        return result;
    }

    private void restoreMask(List<Thing> items, long mask, int legacyCount) {
        for (int i = 0; i < items.size(); i++) items.get(i).hit = i < Long.SIZE - 1 ? (mask & (1L << i)) != 0 : i < legacyCount;
        if (mask == 0) for (int i = 0; i < Math.min(legacyCount, items.size()); i++) items.get(i).hit = true;
    }

    private void loadSave(int slot) {
        Properties p = readSave(slot);
        resetTransientMovement();
        name = p.getProperty("name", "Viajante");
        currentPhase = Math.max(0, Math.min(Phase.values().length - 1, intValue(p, "phase", 0)));
        unlockedPhase = Math.max(currentPhase, Math.min(Phase.values().length - 1, intValue(p, "unlockedPhase", currentPhase)));
        campaignComplete = Boolean.parseBoolean(p.getProperty("campaignComplete", "false"));
        lives = Math.max(1, Math.min(3, intValue(p, "lives", 3)));
        x = clamp(doubleValue(p, "x", 110), 0, WORLD_W - PLAYER_WIDTH - 2); y = clamp(doubleValue(p, "y", 500), 100, 650);
        boolean savedBucketFull = Boolean.parseBoolean(p.getProperty("bucketFull", "false"));
        int savedCoins = intValue(p, "coins", 0);
        buildLevel();
        bucketFull = savedBucketFull;
        restoreMask(coins, longValue(p, "coinMask", 0), savedCoins);
        restoreMask(fires, longValue(p, "fireMask", 0), 0);
        restoreMask(traps, longValue(p, "trapMask", 0), 0);
        coinsCollected = (int) coins.stream().filter(item -> item.hit).count();
        camera = clamp(x - 340, 0, WORLD_W - W);
        if (campaignComplete) screen = GameState.END;
        else openPhaseIntro();
    }

    private int intValue(Properties properties, String key, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private double doubleValue(Properties properties, String key, double fallback) {
        try { return Double.parseDouble(properties.getProperty(key, String.valueOf(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private long longValue(Properties properties, String key, long fallback) {
        try { return Long.parseUnsignedLong(properties.getProperty(key, Long.toHexString(fallback)), 16); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private void deleteSave(int slot) { if (saveManager.exists(slot)) { if (saveManager.delete(slot)) showToast("Slot apagado."); else showToast("Nao foi possivel apagar este slot."); } screen = GameState.SLOTS; }
    private void showToast(String message) { toast = message; toastUntil = System.currentTimeMillis() + 2200; }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }


}
