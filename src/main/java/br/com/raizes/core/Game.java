package br.com.raizes.core;


import br.com.raizes.model.Phase;
import br.com.raizes.model.Question;
import br.com.raizes.model.Thing;
import br.com.raizes.model.TreeDecoration;
import br.com.raizes.model.GameCredits;
import br.com.raizes.audio.AmbientAudio;
import static br.com.raizes.core.GameAssets.resizeImage;
import br.com.raizes.audio.CinematicManager;
import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.audio.CinematicScene;
import br.com.raizes.character.AnimationState;
import br.com.raizes.character.CharacterDefinition;
import br.com.raizes.character.PlayerAnimator;
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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class Game extends JPanel {
    final Set<Integer> held = new HashSet<>();
    final List<Rectangle> platforms = new ArrayList<>();
    final List<Thing> coins = new ArrayList<>();
    final List<Thing> enemies = new ArrayList<>();
    final List<Thing> fires = new ArrayList<>();
    final List<Thing> traps = new ArrayList<>();
    final List<TreeDecoration> trees = new ArrayList<>();
    final List<Question> quizQuestions = new ArrayList<>();
    final Map<Long, BufferedImage> platformArtwork = new java.util.HashMap<>();
    static final int[] CELEBRATION_GUARDIANS = {480, 1210, 1940, 2670};
    private enum GuardianConversation { CHALLENGE, REMINDER, THANKS, CELEBRATION }
    private GuardianConversation conversation = GuardianConversation.CHALLENGE;
    private int dialogueGuardian;
    boolean celebrationActive;
    boolean simulationMode;
    static final Rectangle SIMULATION_CONTROL = new Rectangle(20, H - 28, W - 40, 24);
    private int simulationGuardian;
    private final boolean lowPower = Boolean.getBoolean("raizes.lowPower")
        || Runtime.getRuntime().maxMemory() <= 192L * 1024 * 1024
        || Runtime.getRuntime().availableProcessors() <= 2;
    private static final long LOW_POWER_FRAME_NANOS = 1_000_000_000L / 30;
    private long nextWorldPaint;
    private final Rectangle playerBody = new Rectangle(0, 0, PLAYER_WIDTH, PLAYER_HEIGHT);
    private boolean phaseQuizComplete, traversalComplete;
    private final Map<Integer, Font> fonts = new java.util.HashMap<>();
    final Timer timer;
    final SaveManager saveManager;
    final CinematicCatalog cinematicCatalog;
    final CinematicManager cinematicManager;
    final WorldRenderer worldRenderer;
    volatile GameState screen = GameState.MENU;
    private GameState settingsReturn = GameState.MENU;
    private GameState confirmReturn = GameState.SLOTS;
    private ConfirmAction confirmAction;
    BufferedImage worldBackground, groundTile, menuBackground;
    private final GameAssets assets;
    final AmbientAudio ambientAudio = new AmbientAudio();
    private boolean audioAttached;
    private CharacterDefinition selectedCharacter = CharacterDefinition.MALE;
    private CharacterDefinition characterChoice = CharacterDefinition.MALE;
    final PlayerAnimator characterPreview = new PlayerAnimator(CharacterDefinition.MALE);
    PlayerAnimator playerAnimator = new PlayerAnimator(CharacterDefinition.MALE);
    BufferedImage platformImage;
    BufferedImage seedImage, trashImage, trapImage, lakeImage, fireImage;
    BufferedImage slotTitleImage;
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
    private Thing activeTrap;
    private int trapChallengeStage;
    private double trapBallPosition, trapBallDirection, trapChallengeSpeed, trapTargetCenter, trapTargetHalfWidth;
    private String trapChallengeMessage = "";
    private long trapChallengeMessageUntil;
    private int textScale = 1;
    private boolean contrast, nameConfirm, onGround, exitBlocked, campaignComplete, choosingContinue;
    private boolean pendingPrologue, pendingChapter;
    boolean bucketFull;
    boolean facingRight = true, walking;
    double x = 110, y = 500, vy, camera, elapsed;
    int dialogueIndex;
    long toastUntil;
    long phaseLoadStartedAt;
    Question previousQuizFirst;
    long vegetationGeneration;
    private long lastTickNanos = System.nanoTime();
    private long cinematicEpoch;
    private GameState cinematicReturn = GameState.MENU;

    public Game() {
        this(new CinematicCatalog(), new SaveManager());
    }

    Game(CinematicCatalog cinematicCatalog, SaveManager saveManager) {
        this(cinematicCatalog, saveManager, GameAssets.load(ignored -> { }));
    }

    public Game(GameAssets assets) {
        this(new CinematicCatalog(), new SaveManager(), assets);
    }

    Game(CinematicCatalog cinematicCatalog, SaveManager saveManager, GameAssets assets) {
        this.assets = Objects.requireNonNull(assets);
        this.cinematicCatalog = Objects.requireNonNull(cinematicCatalog);
        this.saveManager = Objects.requireNonNull(saveManager);
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setLayout(null);
        setBackground(Color.BLACK);
        setOpaque(true);
        cinematicManager = new CinematicManager(this::showToast, this::cancelCinematic);
        worldRenderer = new WorldRenderer(this);
        add(cinematicManager.panel());
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { layoutCinematicPanel(); }
        });
        layoutCinematicPanel();
        groundTile = assets.groundTile;
        menuBackground = assets.menuBackground;
        platformImage = assets.platformImage;
        seedImage = assets.seedImage;
        trashImage = assets.trashImage;
        trapImage = assets.trapImage;
        lakeImage = assets.lakeImage;
        fireImage = assets.fireImage;
        slotTitleImage = assets.slotTitleImage;
        nameTitleArtwork = assets.nameTitleArtwork;
        pauseTitleImage = assets.pauseTitleImage;
        pauseIndicatorImage = assets.pauseIndicatorImage;
        settingsTitleImage = assets.settingsTitleImage;
        System.arraycopy(assets.slotImages, 0, slotImages, 0, slotImages.length);
        System.arraycopy(assets.treeImages, 0, treeImages, 0, treeImages.length);
        System.arraycopy(assets.guardianImages, 0, guardianImages, 0, guardianImages.length);
        System.arraycopy(assets.menuImages, 0, menuImages, 0, menuImages.length);
        System.arraycopy(assets.menuOpaqueBounds, 0, menuOpaqueBounds, 0, menuOpaqueBounds.length);
        buildLevel();
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                boolean firstPress = held.add(e.getKeyCode());
                if ((screen != GameState.CINEMATIC && screen != GameState.TRAP_CHALLENGE) || firstPress) {
                    keyPressedGame(e.getKeyCode());
                    if (screen != GameState.WORLD) repaint();
                }
            }
            @Override public void keyReleased(KeyEvent e) { held.remove(e.getKeyCode()); }
            @Override public void keyTyped(KeyEvent e) {
                if (screen == GameState.NAME && !nameConfirm && name.length() < 18) {
                    char c = e.getKeyChar();
                    if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') name += c;
                    repaint();
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
                    repaint();
                }
            }
        });
        timer = new Timer(16, e -> tick());
        timer.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    private void buildLevel() {
        worldBackground = celebrationActive ? assets.celebrationBackground : assets.backgrounds[currentPhase];
        platforms.clear(); coins.clear(); enemies.clear(); fires.clear(); traps.clear(); trees.clear();
        if (celebrationActive) {
            platforms.add(new Rectangle(0, GROUND_Y, WORLD_W, 120));
            worldRenderer.prepare();
            return;
        }
        if (currentPhase == 1) {
            platforms.add(new Rectangle(0, GROUND_Y, 710, 120));
            platforms.add(new Rectangle(820, GROUND_Y, 610, 120));
            platforms.add(new Rectangle(1540, GROUND_Y, 690, 120));
            platforms.add(new Rectangle(2370, GROUND_Y, WORLD_W - 2370, 120));
        } else platforms.add(new Rectangle(0, GROUND_Y, WORLD_W, 120));
        int[][] ledges = switch (currentPhase) {
            case 0 -> new int[][] {
                {260, 500, 175, 22}, {500, 445, 130, 28}, {760, 390, 190, 24},
                {1040, 470, 135, 34}, {1280, 410, 180, 26}, {1570, 345, 150, 30},
                {1840, 425, 210, 24}, {2170, 370, 160, 32}, {2440, 455, 220, 22},
                {2780, 390, 180, 30}
            };
            case 1 -> new int[][] {
                {300, 510, 160, 25}, {520, 450, 210, 32}, {800, 385, 135, 23},
                {1010, 470, 210, 34}, {1300, 410, 145, 27}, {1540, 330, 195, 28},
                {1810, 420, 150, 34}, {2050, 350, 220, 26}, {2340, 445, 150, 30},
                {2590, 375, 210, 24}, {2880, 500, 140, 32}
            };
            case 2 -> new int[][] {
                {240, 480, 210, 26}, {590, 415, 150, 34}, {850, 505, 220, 22},
                {1210, 450, 140, 30}, {1490, 360, 220, 28}, {1850, 430, 165, 24},
                {2140, 505, 210, 32}, {2500, 420, 150, 26}, {2780, 340, 200, 30}
            };
            default -> new int[][] {
                {210, 520, 240, 34}, {570, 455, 140, 22}, {830, 390, 210, 30},
                {1170, 500, 150, 26}, {1450, 430, 200, 34}, {1780, 350, 170, 24},
                {2070, 475, 240, 32}, {2460, 405, 150, 22}, {2730, 520, 200, 30}
            };
        };
        for (int[] p : ledges) platforms.add(new Rectangle(p[0], p[1] + WORLD_VERTICAL_SHIFT, p[2], p[3]));
        if (currentPhase == 0) {
            int[] seedXs = { 360, 420, 675, 735, 1010, 1080, 1370, 1450, 1690, 1750, 2030, 2110, 2360, 2440, 2680, 2750, 2940 };
            for (int itemX : seedXs) coins.add(new Thing(itemX, 0));
            traps.add(new Thing(850, GROUND_Y - 40)); traps.add(new Thing(1770, GROUND_Y - 40)); traps.add(new Thing(2550, GROUND_Y - 40));
            enemies.add(new Thing(760, 0)); enemies.add(new Thing(1500, 0)); enemies.add(new Thing(2180, 0)); enemies.add(new Thing(2820, 0));
        } else if (currentPhase == 1) {
            enemies.add(new Thing(570, 0)); enemies.add(new Thing(1260, 0)); enemies.add(new Thing(2030, 0)); enemies.add(new Thing(2730, 0));
        } else if (currentPhase == 2) {
            // Keep the original four first so previously saved fire progress keeps its meaning.
            int[][] fireLocations = {
                {690, GROUND_Y - 42}, {1190, GROUND_Y - 42},
                {1800, GROUND_Y - 42}, {2490, GROUND_Y - 42},
                {350, 480 + WORLD_VERTICAL_SHIFT + PLATFORM_SURFACE_OFFSET - 42},
                {950, 505 + WORLD_VERTICAL_SHIFT + PLATFORM_SURFACE_OFFSET - 42},
                {1500, 360 + WORLD_VERTICAL_SHIFT + PLATFORM_SURFACE_OFFSET - 42},
                {2200, 505 + WORLD_VERTICAL_SHIFT + PLATFORM_SURFACE_OFFSET - 42},
                {2900, 340 + WORLD_VERTICAL_SHIFT + PLATFORM_SURFACE_OFFSET - 42}
            };
            for (int[] fire : fireLocations) fires.add(new Thing(fire[0], fire[1]));
        } else {
            int[][] litter = {
                {300, 570}, {640, 505}, {930, 440}, {1120, GROUND_Y - 24},
                {1240, 550}, {1530, 480}, {1830, 400}, {2200, 525},
                {2530, 455}, {2800, 570}, {3100, GROUND_Y - 24}
            };
            for (int[] item : litter) coins.add(new Thing(item[0], item[1]));
        }
        bucketFull = false;
        exitBlocked = false;
        preparePlatformArtwork();
        generateVegetation();
        worldRenderer.prepare();
    }

    private void preparePlatformArtwork() {
        if (platformImage == null) return;
        for (Rectangle platform : platforms) {
            if (platform.y == GROUND_Y) continue;
            platformArtwork.computeIfAbsent(platformArtKey(platform), ignored -> resizeImage(platformImage, platform.width, platform.height));
        }
    }

    static long platformArtKey(Rectangle platform) { return ((long)platform.width << 32) | (platform.height & 0xffffffffL); }

    BufferedImage guardianWorldImage(int guardian) { return assets.guardianWorldImages[guardian]; }

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
        long now = System.nanoTime();
        double dt = Math.max(0, (now - lastTickNanos) / 1_000_000_000.0);
        lastTickNanos = now;
        updateAmbientAudio();
        boolean menuVisible = screen == GameState.MENU || screen == GameState.EXTRAS
            || screen == GameState.SAVE_OPTIONS || screen == GameState.PHASE_SELECT
            || screen == GameState.SAVE_CONFIRM || settingsReturn == GameState.MENU
                && (screen == GameState.SETTINGS || screen == GameState.CONTROLS);
        boolean menuChanged = menuVisible && updateMenuScale();
        if (screen == GameState.WORLD) updateWorld(dt);
        else if (screen == GameState.TRAP_CHALLENGE) updateTrapChallenge(dt);
        boolean toastChanged = !toast.isEmpty() && toastUntil < System.currentTimeMillis();
        if (toastChanged) toast = "";
        boolean animated = screen == GameState.WORLD || screen == GameState.TRAP_CHALLENGE
            || screen == GameState.CHARACTER_SELECT;
        if (animated && (!lowPower || now >= nextWorldPaint)) {
            // Carry the deadline forward so 16 ms timer quantization does not turn 30 fps into 20.
            nextWorldPaint = (now - nextWorldPaint > LOW_POWER_FRAME_NANOS ? now : nextWorldPaint)
                + LOW_POWER_FRAME_NANOS;
            repaint();
        } else if (menuChanged || toastChanged) repaint();
    }

    private boolean updateMenuScale() {
        boolean changed = false;
        int selected = screen == GameState.MENU ? menuIndex : -1;
        for (int i = 0; i < menuScale.length; i++) {
            double target = i == selected ? 1.08 : 1.0;
            changed |= menuScale[i] != target;
            menuScale[i] += (target - menuScale[i]) * 0.24;
            if (Math.abs(target - menuScale[i]) < 0.001) menuScale[i] = target;
        }
        return changed;
    }

    private void updateWorld(double dt) {
        if (screen != GameState.WORLD) return;
        double animationSeconds = dt;
        elapsed += dt;
        dt = Math.min(dt, 0.05);
        double dx = (held.contains(KeyEvent.VK_D) || held.contains(KeyEvent.VK_RIGHT) ? 1 : 0) - (held.contains(KeyEvent.VK_A) || held.contains(KeyEvent.VK_LEFT) ? 1 : 0);
        walking = dx != 0;
        if (walking) facingRight = dx > 0;
        boolean walkSlowly = held.contains(KeyEvent.VK_SHIFT);
        x = clamp(x + dx * (walkSlowly ? 160 : 260) * dt, 0, WORLD_W - PLAYER_WIDTH - 2);
        if ((held.contains(KeyEvent.VK_SPACE) || held.contains(KeyEvent.VK_W) || held.contains(KeyEvent.VK_UP)) && onGround) { vy = -590; onGround = false; }
        vy += 1350 * dt;
        double oldY = y;
        y += vy * dt;
        onGround = false;
        Rectangle body = playerBody;
        body.setLocation((int)x + 2, (int)y);
        for (Rectangle platform : platforms) {
            int surfaceY = platform.y == GROUND_Y ? GROUND_Y : platform.y + PLATFORM_SURFACE_OFFSET;
            boolean overlapsPlatform = body.x < platform.x + platform.width && body.x + body.width > platform.x;
            if (overlapsPlatform && oldY + PLAYER_HEIGHT <= surfaceY && y + PLAYER_HEIGHT >= surfaceY && vy >= 0) {
                y = surfaceY - PLAYER_HEIGHT; vy = 0; onGround = true; body.y = (int)y;
            }
        }
        if (y > H + 100) { loseLife(); return; }
        for (Thing coin : coins) if (!coin.hit && body.intersects(coin.x - 12, coinY(coin), 24, 24)) { coin.hit = true; coinsCollected++; }
        if (currentPhase == 2) {
            for (Thing fire : fires) {
                if (fire.hit) continue;
                if (body.intersects(fire.x - 30, fire.y - 30, 60, 72)) {
                    loseLife();
                    showToast("As chamas te atingiram! Tente apagá-las de uma distância segura.");
                    return;
                }
            }
        }
        for (Thing enemy : enemies) {
            if (enemy.hit) continue;
            enemy.x += (int) Math.round(Math.sin(elapsed + enemy.x) * ENEMY_SPEED);
            int enemyY = GROUND_Y - 48;
            if (body.intersects(enemy.x - 20, enemyY, 40, 48)) {
                boolean stompedFromAbove = vy >= 0 && oldY + PLAYER_HEIGHT <= enemyY + 14;
                if (stompedFromAbove) {
                    enemy.hit = true;
                    y = enemyY - PLAYER_HEIGHT;
                    vy = -390;
                    onGround = false;
                    body.y = (int) y;
                } else {
                    loseLife();
                    return;
                }
            }
        }
        AnimationState animation = !onGround ? (vy < 0 ? AnimationState.JUMP : AnimationState.FALL)
            : walking ? (walkSlowly ? AnimationState.WALK : AnimationState.RUN) : AnimationState.IDLE;
        playerAnimator.update(animation, animationSeconds);
        camera = clamp(x - 340, 0, WORLD_W - W);
        if (x < 2980) exitBlocked = false;
        if (!celebrationActive && currentPhase == 1 && x >= 3020) traversalComplete = true;
        if (!celebrationActive && x > 3020 && !exitBlocked) {
            exitBlocked = true;
            if (phaseObjectiveComplete()) beginGuardianDialogue();
            else {
                showGuardianMessage(GuardianConversation.REMINDER, currentPhase,
                    "Ainda precisamos da sua ajuda. " + exitHint());
                x = 3020;
            }
        }
    }

    private boolean phaseObjectiveComplete() {
        return switch (currentPhase) {
            case 0 -> coinsCollected == coins.size() && traps.stream().allMatch(trap -> trap.hit);
            case 1 -> traversalComplete;
            case 2 -> fires.stream().allMatch(fire -> fire.hit);
            case 3 -> coinsCollected == coins.size();
            default -> false;
        };
    }

    private String exitHint() {
        return switch (currentPhase) {
            case 0 -> "Faltam " + (coins.size()-coinsCollected) + " sementes e " + remaining(traps) + " armadilhas.";
            case 2 -> "Faltam " + remaining(fires) + " focos de incêndio. Use as fontes para encher o balde.";
            case 3 -> "Faltam " + (coins.size()-coinsCollected) + " resíduos para recolher.";
            default -> "Conclua a travessia até o fim da trilha.";
        };
    }

    private static long remaining(List<Thing> items) { return items.stream().filter(item -> !item.hit).count(); }

    String objectiveStatus() {
        if (celebrationActive) return "Obrigado por jogar! Converse com os quatro guardiões.";
        return switch(currentPhase) {
            case 0 -> "Sementes: " + coinsCollected + "/" + coins.size() + "   Armadilhas: " + (traps.size()-remaining(traps)) + "/" + traps.size();
            case 1 -> "Travessia: " + (traversalComplete ? 100 : Math.min(99, (int)(x*100/3020))) + "%";
            case 2 -> "Focos: " + (fires.size()-remaining(fires)) + "/" + fires.size() + "   Balde: " + (bucketFull ? "cheio" : "vazio");
            default -> "Resíduos: " + coinsCollected + "/" + coins.size();
        };
    }

    private void showGuardianMessage(GuardianConversation kind, int guardian, String... lines) {
        conversation = kind;
        dialogueGuardian = guardian;
        dialogue = lines;
        dialogueIndex = 0;
        held.clear();
        screen = GameState.DIALOGUE;
    }

    private void beginGuardianDialogue() {
        if (phaseQuizComplete) { showGuardianThanks(); return; }
        if (!phaseObjectiveComplete()) {
            showGuardianMessage(GuardianConversation.REMINDER, currentPhase, "Ainda há objetivos na trilha. " + exitHint());
            return;
        }
        dialogue = switch (currentPhase) {
            case 0 -> new String[] { "Você protegeu os rastros da fauna. A caça ilegal ameaça toda a floresta.", "Animais silvestres pertencem ao seu habitat. Denuncie a caça e nunca arme armadilhas.", "Antes de seguir, responda ao desafio sobre a proteção da fauna." };
            case 1 -> new String[] { "Você venceu o percurso! Reduzir resíduos e respeitar os caminhos protege a vida.", "Cada escolha responsável ajuda a floresta a continuar de pé.", "Vamos conferir o que aprendemos sobre escolhas responsáveis." };
            case 2 -> new String[] { "Os focos foram apagados. Queimadas destroem habitats e colocam vidas em risco.", "Nunca queime lixo ou vegetação. Avise os serviços de emergência ao ver fogo.", "Antes de seguir, responda ao desafio das queimadas." };
            default -> new String[] { "A trilha está limpa. Resíduos na natureza ferem animais e contaminam água e solo.", "Reduza, reutilize e descarte corretamente. O cuidado de cada pessoa faz diferença.", "Vamos conversar sobre o descarte e a redução de resíduos." };
        };
        dialogueIndex = 0;
        dialogueGuardian = currentPhase;
        conversation = GuardianConversation.CHALLENGE;
        held.clear();
        screen = GameState.DIALOGUE;
    }

    private void interactPhase() {
        int centerX = (int) x + 19;
        int centerY = (int) y + 29;
        if (celebrationActive) {
            for (int guardian=0; guardian<CELEBRATION_GUARDIANS.length; guardian++) {
                if (Math.abs(centerX-CELEBRATION_GUARDIANS[guardian]) < 120 && Math.abs(y+PLAYER_HEIGHT-GROUND_Y) < 100) {
                    showGuardianMessage(GuardianConversation.CELEBRATION, guardian, guardianThanks(guardian),
                        "Nossa floresta celebra com você. Leve esse cuidado para todos os lugares!" );
                    return;
                }
            }
            showToast("Aproxime-se de um guardião e pressione E para conversar.");
            return;
        }
        if (centerX > 2900) { beginGuardianDialogue(); return; }
        if (currentPhase == 0) {
            for (Thing trap : traps) {
                if (!trap.hit && Math.abs(centerX - trap.x) < 76 && Math.abs(centerY - trap.y) < 90) {
                    startTrapChallenge(trap);
                    return;
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

    private void startTrapChallenge(Thing trap) {
        activeTrap = trap;
        trapChallengeStage = 1;
        trapBallPosition = 0.02;
        trapBallDirection = 1;
        trapChallengeMessage = "Acerte a bolinha dourada na faixa verde.";
        trapChallengeMessageUntil = System.currentTimeMillis() + 2000;
        setTrapChallengeDifficulty();
        held.clear();
        screen = GameState.TRAP_CHALLENGE;
    }

    private void setTrapChallengeDifficulty() {
        trapChallengeSpeed = switch (trapChallengeStage) {
            case 1 -> 0.72;
            case 2 -> 1.02;
            default -> 1.38;
        };
        trapTargetHalfWidth = switch (trapChallengeStage) {
            case 1 -> 0.15;
            case 2 -> 0.105;
            default -> 0.07;
        };
        trapTargetCenter = switch (trapChallengeStage) {
            case 1 -> 0.38;
            case 2 -> 0.68;
            default -> 0.45;
        };
    }

    private void updateTrapChallenge(double dt) {
        trapBallPosition += trapBallDirection * trapChallengeSpeed * Math.min(dt, 0.05);
        if (trapBallPosition >= 1) { trapBallPosition = 1; trapBallDirection = -1; }
        else if (trapBallPosition <= 0) { trapBallPosition = 0; trapBallDirection = 1; }
        if (trapChallengeMessageUntil < System.currentTimeMillis()) trapChallengeMessage = "";
    }

    private void confirmTrapChallengeHit() {
        if (screen != GameState.TRAP_CHALLENGE || activeTrap == null) return;
        if (Math.abs(trapBallPosition - trapTargetCenter) > trapTargetHalfWidth + 0.018) {
            trapChallengeMessage = "Quase! Tente novamente quando a bolinha passar pelo verde.";
            trapChallengeMessageUntil = System.currentTimeMillis() + 1000;
            return;
        }
        if (trapChallengeStage == 3) {
            activeTrap.hit = true;
            activeTrap = null;
            screen = GameState.WORLD;
            showToast("Armadilha desativada. A trilha esta protegida.");
            return;
        }
        trapChallengeStage++;
        setTrapChallengeDifficulty();
        trapChallengeMessage = "Acertou! A bolinha acelerou e a faixa verde diminuiu.";
        trapChallengeMessageUntil = System.currentTimeMillis() + 1300;
    }

    private void cancelTrapChallenge() {
        activeTrap = null;
        trapChallengeStage = 0;
        trapChallengeMessage = "";
        screen = GameState.WORLD;
    }

    private void loseLife() {
        lives--; x = 110; y = 500; vy = 0;
        if (lives <= 0) {
            lives = 3;
            // Leaving the thanks dialogue must not erase an already earned chapter completion.
            if (!phaseQuizComplete) { coinsCollected = 0; buildLevel(); }
            showToast("A floresta lhe deu outra chance.");
        }
    }

    private void keyPressedGame(int key) {
        if (key == KeyEvent.VK_F8 && simulationMode) { advanceSimulation(); return; }
        if (screen == GameState.MENU) menuKey(key);
        else if (screen == GameState.SLOTS) slotsKey(key);
        else if (screen == GameState.SAVE_OPTIONS) saveOptionsKey(key);
        else if (screen == GameState.PHASE_SELECT) phaseSelectKey(key);
        else if (screen == GameState.SAVE_CONFIRM) saveConfirmKey(key);
        else if (screen == GameState.NAME) nameKey(key);
        else if (screen == GameState.CHARACTER_SELECT) characterKey(key);
        else if (screen == GameState.CINEMATIC) {
            if (key == KeyEvent.VK_ESCAPE) cancelCinematic();
            else if (key == KeyEvent.VK_P) cinematicManager.skipScene();
            else if (key == KeyEvent.VK_ENTER && cinematicManager.state() == CinematicManager.PlaybackState.FAILED) cinematicManager.retry();
        }
        else if (screen == GameState.PHASE_INTRO) { if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) screen = GameState.WORLD; else if (key == KeyEvent.VK_ESCAPE) returnToMenu(); }
        else if (screen == GameState.TRAP_CHALLENGE) {
            if (key == KeyEvent.VK_ESCAPE) cancelTrapChallenge();
            else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_E)
                confirmTrapChallengeHit();
        }
        else if (screen == GameState.WORLD) worldKey(key);
        else if (screen == GameState.PAUSE) pauseKey(key);
        else if (screen == GameState.SETTINGS) settingsKey(key);
        else if (screen == GameState.CONTROLS) { if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) screen = GameState.SETTINGS; }
        else if (screen == GameState.EXTRAS) {
            if (key == KeyEvent.VK_T) startSimulation();
            else if (key == KeyEvent.VK_C) openCredits();
            else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) returnToMenu();
        }
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
            if (menuIndex == 0 || menuIndex == 1) { simulationMode = false; cinematicManager.setSimulationMode(false); choosingContinue = menuIndex == 1; slotIndex = choosingContinue && firstSave() >= 0 ? firstSave() : 0; screen = GameState.SLOTS; }
            else if (menuIndex == 2) { settingsReturn = GameState.MENU; screen = GameState.SETTINGS; }
            else if (menuIndex == 3) screen = GameState.EXTRAS;
            else { ambientAudio.close(); cinematicManager.dispose(); System.exit(0); }
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
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_W || key == KeyEvent.VK_A) phaseSelectIndex = Math.floorMod(phaseSelectIndex - 1, Phase.values().length + 1);
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_S || key == KeyEvent.VK_D) phaseSelectIndex = (phaseSelectIndex + 1) % (Phase.values().length + 1);
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) selectSavedPhase(phaseSelectIndex);
    }

    private void selectSavedPhase(int phase) {
        if (phase < 0 || phase > Phase.values().length) { showToast("Fase inválida."); return; }
        Properties save = readSave(selectedSaveSlot);
        if (phase == Phase.values().length) {
            if (!Boolean.parseBoolean(save.getProperty("campaignComplete", "false"))) { showToast("Conclua os quatro capítulos para entrar na celebração."); return; }
            restoreSave(selectedSaveSlot);
            openCelebration(false);
            saveGame();
            return;
        }
        unlockedPhase = Math.max(0, Math.min(Phase.values().length - 1, intValue(save, "unlockedPhase", intValue(save, "phase", 0))));
        if (phase > unlockedPhase) { showToast("Essa fase ainda esta bloqueada."); return; }
        pendingPhase = phase; confirmAction = ConfirmAction.RESTART_PHASE; confirmReturn = GameState.PHASE_SELECT; settingsIndex = 0; screen = GameState.SAVE_CONFIRM;
    }

    private void startSavedPhase(int phase) {
        restoreSave(selectedSaveSlot);
        resetPhaseProgress(phase);
        pendingChapter = true;
        saveGame();
        if (pendingPrologue) startOpening(GameState.MENU);
        else startChapter();
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
        else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (nameConfirm) openCharacterSelection(); else nameConfirm = true; }
    }

    private void openCharacterSelection() {
        characterChoice = CharacterDefinition.MALE;
        held.clear();
        screen = GameState.CHARACTER_SELECT;
    }

    private void characterKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = GameState.NAME; return; }
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_A || key == KeyEvent.VK_D)
            characterChoice = characterChoice == CharacterDefinition.MALE ? CharacterDefinition.FEMALE : CharacterDefinition.MALE;
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) confirmCharacter(characterChoice);
    }

    boolean confirmCharacter(CharacterDefinition choice) {
        if (screen != GameState.CHARACTER_SELECT || choice == null || !choice.selectable()) {
            showToast("Personagem indisponível. Feminino: Em breve.");
            return false;
        }
        selectedCharacter = choice;
        playerAnimator = new PlayerAnimator(choice);
        startGame();
        return true;
    }

    CharacterDefinition character() { return selectedCharacter; }

    private int guardianAt(double worldX, int screenY) {
        if (screenY < GROUND_Y-160 || screenY > GROUND_Y+5) return -1;
        if (!celebrationActive) return Math.abs(worldX-3110)<60 ? currentPhase : -1;
        for (int guardian=0;guardian<CELEBRATION_GUARDIANS.length;guardian++)
            if (Math.abs(worldX-CELEBRATION_GUARDIANS[guardian])<60) return guardian;
        return -1;
    }

    private void worldKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { menuIndex = 0; screen = GameState.PAUSE; }
        else if (key == KeyEvent.VK_F5) saveGame();
        else if (key == KeyEvent.VK_M) screen = GameState.MAP;
        else if (key == KeyEvent.VK_E) interactPhase();
        else if (key == KeyEvent.VK_ENTER && celebrationActive) finishCampaign();
    }

    private void pauseKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = GameState.WORLD; return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) menuIndex = (menuIndex + 4) % 5;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) menuIndex = (menuIndex + 1) % 5;
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (menuIndex == 0) screen = GameState.WORLD; else if (menuIndex == 1) saveGame(); else if (menuIndex == 2) { settingsReturn = GameState.PAUSE; screen = GameState.SETTINGS; } else if (menuIndex == 3) screen = GameState.MAP; else { saveGame(); returnToMenu(); } }
    }

    private void returnToMenu() {
        cinematicEpoch++;
        cinematicManager.dispose();
        held.clear();
        screen = GameState.MENU;
        menuIndex = 0;
        choosingContinue = false;
        simulationMode = false;
        cinematicManager.setSimulationMode(false);
    }

    private void cancelCinematic() {
        if (cinematicReturn == GameState.MENU) {
            returnToMenu();
            requestFocusInWindow();
            repaint();
            return;
        }
        cinematicEpoch++;
        cinematicManager.dispose();
        held.clear();
        screen = cinematicReturn;
        requestFocusInWindow();
        repaint();
    }

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

    private boolean inside(int x, int y, int bx, int by, int bw, int bh) {
        return x >= bx && y >= by && x < bx + bw && y < by + bh;
    }

    private void updateMouseHover(int x, int y) {
        int previousSelection = hoveredOption();
        int cursor = java.awt.Cursor.DEFAULT_CURSOR;
        if (screen == GameState.MENU) { int index = menuAt(x, y); if (index == 1 && !hasAnySave()) index = -1; menuIndex = index; if (index >= 0) cursor = java.awt.Cursor.HAND_CURSOR; }
        else if (screen == GameState.SLOTS) { if (slotCardAt(x, y) >= 0 || slotActionAt(x, y) >= 0) { int slot = slotCardAt(x, y); if (slot >= 0) slotIndex = slot; cursor=java.awt.Cursor.HAND_CURSOR; } }
        else if (screen == GameState.CHARACTER_SELECT) {
            int card = characterCardAt(x, y);
            if (card >= 0) characterChoice = CharacterDefinition.values()[card];
            if ((card >= 0 && characterChoice.selectable()) || inside(x,y,205,570,200,40)
                    || (characterChoice.selectable() && inside(x,y,470,550,340,56))) cursor=java.awt.Cursor.HAND_CURSOR;
        }
        else if (screen == GameState.SAVE_OPTIONS) { saveOptionIndex = inside(x,y,300,275,680,72) ? 0 : inside(x,y,300,367,680,72) ? 1 : saveOptionIndex; if (inside(x,y,300,275,680,72)||inside(x,y,300,367,680,72)) cursor=java.awt.Cursor.HAND_CURSOR; }
        else if (screen == GameState.PHASE_SELECT) { for(int i=0;i<=Phase.values().length;i++) if(inside(x,y,215,205+i*75,850,60)){phaseSelectIndex=i;cursor=java.awt.Cursor.HAND_CURSOR;break;} }
        else if (screen == GameState.SAVE_CONFIRM) { if(inside(x,y,310,395,300,70)){settingsIndex=0;cursor=java.awt.Cursor.HAND_CURSOR;}else if(inside(x,y,670,395,300,70)){settingsIndex=1;cursor=java.awt.Cursor.HAND_CURSOR;} }
        else if (screen == GameState.SETTINGS) { if (settingsRowAt(x, y) >= 0 || inside(x,y,440,575,400,45)) { int row=settingsRowAt(x,y); if(row>=0)settingsIndex=row; cursor=java.awt.Cursor.HAND_CURSOR; } }
        else if (screen == GameState.PAUSE) { int option = pauseOptionAt(x, y); if (option >= 0) { menuIndex = option; cursor=java.awt.Cursor.HAND_CURSOR; } }
        else if (screen == GameState.TRAP_CHALLENGE && inside(x, y, 250, 330, 780, 100)) cursor=java.awt.Cursor.HAND_CURSOR;
        else if (screen == GameState.BATTLE) { int option = battleChoiceAt(x, y); if (option >= 0) { battleChoice = option; cursor=java.awt.Cursor.HAND_CURSOR; } }
        else if (screen == GameState.DIALOGUE || screen == GameState.PHASE_INTRO || screen == GameState.END || screen == GameState.EXTRAS || screen == GameState.CONTROLS) cursor=java.awt.Cursor.HAND_CURSOR;
        else if (screen == GameState.MAP) cursor=java.awt.Cursor.HAND_CURSOR;
        else if (screen == GameState.WORLD) { double worldX=x+camera; if(guardianAt(worldX,y)>=0||currentPhase==0&&traps.stream().anyMatch(t->!t.hit&&Math.abs(worldX-t.x)<42)||currentPhase==2&&(Math.abs(worldX-145)<38||Math.abs(worldX-1650)<38||fires.stream().anyMatch(f->!f.hit&&Math.abs(worldX-f.x)<42)))cursor=java.awt.Cursor.HAND_CURSOR; }
        if (simulationMode && SIMULATION_CONTROL.contains(x,y)) cursor=java.awt.Cursor.HAND_CURSOR;
        if (celebrationActive && screen == GameState.WORLD && inside(x,y,1040,12,220,50)) cursor=java.awt.Cursor.HAND_CURSOR;
        setCursor(java.awt.Cursor.getPredefinedCursor(cursor));
        if (hoveredOption() != previousSelection) repaint();
    }

    private int hoveredOption() {
        return switch (screen) {
            case MENU, PAUSE -> menuIndex;
            case SLOTS -> slotIndex;
            case CHARACTER_SELECT -> characterChoice.ordinal();
            case SAVE_OPTIONS -> saveOptionIndex;
            case PHASE_SELECT -> phaseSelectIndex;
            case SAVE_CONFIRM, SETTINGS -> settingsIndex;
            case BATTLE -> battleChoice;
            default -> -1;
        };
    }

    private void handleMouseClick(int x, int y) {
        if (simulationMode && screen != GameState.END && SIMULATION_CONTROL.contains(x,y)) {
            advanceSimulation(); return;
        }
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
            case WORLD -> {
                if (celebrationActive && inside(x,y,1040,12,220,50)) { finishCampaign(); return; }
                double worldX = x + camera;
                int guardian = guardianAt(worldX, y);
                if (guardian >= 0) {
                    int guardianX = celebrationActive ? CELEBRATION_GUARDIANS[guardian] : 3110;
                    if (Math.abs(this.x + PLAYER_WIDTH / 2.0 - guardianX) < 120) interactPhase();
                    else showToast("Aproxime-se do guardião para conversar.");
                    return;
                }
                boolean targetY = y >= GROUND_Y - 110 && y <= GROUND_Y + 10;
                boolean trapTarget = currentPhase == 0
                    && traps.stream().anyMatch(t -> !t.hit && Math.abs(worldX - t.x) < 42);
                boolean fireTarget = currentPhase == 2
                    && (Math.abs(worldX - 145) < 38 || Math.abs(worldX - 1650) < 38
                        || fires.stream().anyMatch(f -> !f.hit && Math.abs(worldX - f.x) < 42));
                if (targetY && (trapTarget || fireTarget) && Math.abs(worldX - this.x) < 110) interactPhase();
                else if (targetY && (trapTarget || fireTarget)) showToast("Aproxime-se do elemento para interagir com E ou clique.");
            }
            case TRAP_CHALLENGE -> { if (inside(x, y, 250, 330, 780, 100)) confirmTrapChallengeHit(); }
            case SAVE_OPTIONS -> { if(inside(x,y,300,275,680,72)){saveOptionIndex=0;saveOptionsKey(KeyEvent.VK_ENTER);}else if(inside(x,y,300,367,680,72)){saveOptionIndex=1;saveOptionsKey(KeyEvent.VK_ENTER);}else if(inside(x,y,300,480,680,55))screen=GameState.SLOTS; }
            case PHASE_SELECT -> { for(int i=0;i<=Phase.values().length;i++)if(inside(x,y,215,205+i*75,850,60)){phaseSelectIndex=i;selectSavedPhase(i);return;} if(inside(x,y,420,570,440,55))screen=GameState.SAVE_OPTIONS; }
            case SAVE_CONFIRM -> { if(inside(x,y,310,395,300,70)){settingsIndex=0;saveConfirmKey(KeyEvent.VK_ENTER);}else if(inside(x,y,670,395,300,70)){settingsIndex=1;saveConfirmKey(KeyEvent.VK_ENTER);} }
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
            case EXTRAS -> {
                if (inside(x,y,315,440,650,55)) startSimulation();
                else if (inside(x,y,315,505,650,50)) openCredits();
                else returnToMenu();
            }
            case PAUSE -> { int option = pauseOptionAt(x, y); if (option >= 0) { menuIndex = option; pauseKey(KeyEvent.VK_ENTER); } }
            case MAP -> { if(inside(x,y,400,520,480,65))screen=GameState.WORLD; }
            case DIALOGUE -> { if(inside(x,y,270,605,330,42))leaveGuardian(); else if(inside(x,y,100,490,1080,165))advanceDialogue(); }
            case BATTLE -> { if(inside(x,y,125,605,310,42)){battleKey(KeyEvent.VK_ESCAPE);return;} int option = battleChoiceAt(x, y); if (option >= 0) { battleChoice = option; battleKey(KeyEvent.VK_ENTER); } }
            case CINEMATIC -> { /* Playback, skipping and recovery belong to JavaFX. */ }
            case PHASE_INTRO -> { if(inside(x,y,480,575,600,90))screen=GameState.WORLD; }
            case END -> { if(inside(x,y,480,535,320,64))returnToMenu(); }
            case NAME -> { if(inside(x,y,320,390,650,100)){nameConfirm=true;openCharacterSelection();} }
            case CHARACTER_SELECT -> {
                int card = characterCardAt(x, y);
                if (card >= 0) {
                    characterChoice = CharacterDefinition.values()[card];
                    if (!characterChoice.selectable()) showToast("Feminino: Em breve.");
                } else if (inside(x,y,470,550,340,56)) confirmCharacter(characterChoice);
                else if (inside(x,y,205,570,200,40)) screen = GameState.NAME;
            }
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

    private void dialogueKey(int key) { if (key == KeyEvent.VK_ESCAPE) leaveGuardian(); else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_E) advanceDialogue(); }
    private void leaveGuardian() {
        held.clear();
        exitBlocked = true;
        if (!celebrationActive) x = Math.min(x, 3000);
        screen = GameState.WORLD;
    }

    private void advanceDialogue() {
        if (++dialogueIndex < dialogue.length) return;
        switch (conversation) {
            case CHALLENGE -> { startQuizRound(); screen = GameState.BATTLE; }
            case THANKS -> advanceAfterThanks();
            default -> leaveGuardian();
        }
    }

    private void startQuizRound() {
        List<Question> shuffled = new ArrayList<>(Arrays.asList(QuestionBank.QUESTIONS[currentPhase]));
        Collections.shuffle(shuffled);
        if (shuffled.size() > 1 && shuffled.get(0) == previousQuizFirst) Collections.swap(shuffled, 0, 1);
        quizQuestions.clear();
        previousQuizFirst = shuffled.get(0);
        Random random = new Random();
        for (Question question : shuffled.subList(0, Math.min(QUESTIONS_PER_ROUND, shuffled.size())))
            quizQuestions.add(question.shuffled(random));
        quizIndex = 0;
        battleChoice = 0;
    }

    private void battleKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { quizIndex = 0; leaveGuardian(); return; }
        int optionCount = quizQuestions.get(quizIndex).options().length;
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT) battleChoice = Math.floorMod(battleChoice - 1, optionCount);
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_RIGHT) battleChoice = (battleChoice + 1) % optionCount;
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
        phaseQuizComplete = true;
        if (currentPhase == Phase.values().length - 1) campaignComplete = true;
        saveGame();
        showGuardianThanks();
    }

    private static String guardianThanks(int guardian) {
        return switch(guardian) {
            case 0 -> "Obrigado por reunir as sementes e libertar nossa trilha das armadilhas. Os animais têm uma nova chance!";
            case 1 -> "Você atravessou a mata com coragem e aprendeu a cuidar dos caminhos. Obrigado por caminhar com responsabilidade!";
            case 2 -> "Cada foco apagado protegeu vidas e ajudou a floresta a renascer. Obrigada por cuidar da nossa casa!";
            default -> "A água e o solo respiram melhor com os resíduos recolhidos. Obrigado por devolver esperança a esta trilha!";
        };
    }

    private void showGuardianThanks() {
        toast = "";
        showGuardianMessage(GuardianConversation.THANKS, currentPhase, guardianThanks(currentPhase),
            "Você concluiu os objetivos e acertou as cinco questões. Nosso conhecimento segue com você.",
            currentPhase < 3 ? "Vamos levar esse cuidado para a próxima trilha?" : "Todos os guardiões esperam você na celebração da floresta!");
    }

    private void advanceAfterThanks() {
        if (currentPhase < Phase.values().length - 1) {
            unlockedPhase = Math.max(unlockedPhase, currentPhase + 1);
            resetPhaseProgress(currentPhase + 1);
            pendingChapter = true;
            saveGame();
            startChapter();
        } else {
            openCelebration(false);
        }
        saveGame();
    }

    private void startGame() {
        if (!selectedCharacter.selectable()) { openCharacterSelection(); return; }
        if (name.trim().isEmpty()) name = "Viajante";
        unlockedPhase = 0;
        resetPhaseProgress(0);
        pendingPrologue = true;
        pendingChapter = true;
        saveGame();
        startOpening();
    }

    void startSimulation() {
        cinematicEpoch++;
        cinematicManager.dispose();
        simulationMode = true;
        cinematicManager.setSimulationMode(true);
        simulationGuardian = 0;
        selectedCharacter = CharacterDefinition.MALE;
        playerAnimator = new PlayerAnimator(selectedCharacter);
        name = "Simulação";
        startGame();
        repaint();
    }

    /** Exercises the production transitions; never writes a campaign save. */
    boolean advanceSimulation() {
        if (!simulationMode) return false;
        switch (screen) {
            case CINEMATIC -> cinematicManager.skipScene();
            case PHASE_INTRO -> screen = GameState.WORLD;
            case WORLD, TRAP_CHALLENGE -> {
                resetTransientMovement();
                y = GROUND_Y - PLAYER_HEIGHT;
                if (celebrationActive) {
                    if (simulationGuardian == CELEBRATION_GUARDIANS.length) finishCampaign();
                    else {
                        x = CELEBRATION_GUARDIANS[simulationGuardian++] - PLAYER_WIDTH / 2.0;
                        camera = clamp(x - 340, 0, WORLD_W - W);
                        interactPhase();
                    }
                } else {
                    coins.forEach(item -> item.hit = true);
                    traps.forEach(item -> item.hit = true);
                    fires.forEach(item -> item.hit = true);
                    coinsCollected = coins.size();
                    traversalComplete = true;
                    x = 3020;
                    camera = WORLD_W - W;
                    beginGuardianDialogue();
                }
            }
            case DIALOGUE -> {
                int remaining = dialogue.length + 1;
                while (screen == GameState.DIALOGUE && remaining-- > 0) advanceDialogue();
            }
            case BATTLE -> {
                for (int i = 0; i < QUESTIONS_PER_ROUND && screen == GameState.BATTLE; i++) {
                    battleChoice = quizQuestions.get(quizIndex).correctOption();
                    battleKey(KeyEvent.VK_ENTER);
                }
            }
            case END -> returnToMenu();
            case PAUSE, MAP -> screen = GameState.WORLD;
            default -> { return false; }
        }
        held.clear();
        repaint();
        return true;
    }

    private void finishCampaign() {
        if (!celebrationActive || !campaignComplete || screen != GameState.WORLD) return;
        saveGame();
        resetTransientMovement();
        cinematicReturn = GameState.WORLD;
        playCinematic(cinematicCatalog.ending(), "A FLORESTA SEGUE VIVA", this::openCredits);
    }

    private void openCredits() {
        held.clear();
        screen = GameState.END;
        toast = "";
        repaint();
    }

    private void resetPhaseProgress(int phase) {
        celebrationActive = false;
        phaseQuizComplete = false;
        traversalComplete = false;
        currentPhase = phase;
        resetTransientMovement();
        x = 110;
        y = 500;
        camera = 0;
        elapsed = 0;
        lives = 3;
        coinsCollected = 0;
        bucketFull = false;
        campaignComplete = false;
        buildLevel();
    }

    private void openCelebration(boolean restorePosition) {
        toast = "";
        double savedX = x, savedY = y;
        celebrationActive = true;
        campaignComplete = true;
        phaseQuizComplete = false;
        currentPhase = Phase.values().length - 1;
        unlockedPhase = currentPhase;
        pendingPrologue = false;
        pendingChapter = false;
        resetTransientMovement();
        buildLevel();
        x = restorePosition ? savedX : 110;
        y = restorePosition ? savedY : GROUND_Y - PLAYER_HEIGHT;
        camera = clamp(x - 340, 0, WORLD_W - W);
        screen = GameState.WORLD;
    }

    private void resetTransientMovement() {
        held.clear();
        vy = 0;
        onGround = false;
        walking = false;
        playerAnimator.reset();
        exitBlocked = false;
    }
    private void startOpening() {
        startOpening(GameState.CHARACTER_SELECT);
    }

    private void startOpening(GameState returnScreen) {
        cinematicReturn = returnScreen;
        playCinematic(cinematicCatalog.prologue(), "PRÓLOGO", () -> {
            pendingPrologue = false;
            saveGame();
            startChapter();
        });
    }

    private void startChapter() {
        cinematicReturn = GameState.MENU;
        playCinematic(cinematicCatalog.beforeChapter(currentPhase + 1),
            "CAPÍTULO " + (currentPhase + 1) + " • " + Phase.values()[currentPhase].guardian,
            () -> {
                pendingChapter = false;
                saveGame();
                openPhaseIntro();
            });
    }

    private void playCinematic(List<CinematicScene> playlist, String heading, Runnable next) {
        if (playlist.isEmpty()) { next.run(); return; }
        held.clear();
        screen = GameState.CINEMATIC;
        long epoch = ++cinematicEpoch;
        cinematicManager.start(playlist, heading, () -> {
            if (epoch != cinematicEpoch || screen != GameState.CINEMATIC) return;
            cinematicEpoch++;
            held.clear();
            next.run();
            if (screen != GameState.CINEMATIC) requestFocusInWindow();
            repaint();
        });
    }

    @Override public void addNotify() {
        super.addNotify();
        audioAttached = true;
        updateAmbientAudio();
    }

    public java.util.concurrent.CompletableFuture<Void> ready() { return cinematicManager.ready(); }

    public void close() {
        audioAttached = false;
        ambientAudio.close();
        cinematicEpoch++;
        timer.stop();
        cinematicManager.dispose();
    }

    void updateAmbientAudio() {
        if (!audioAttached) return;
        ambientAudio.play(ambientTrack());
    }

    AmbientAudio.Track ambientTrack() {
        boolean worldScreen = switch (screen) {
            case WORLD, PHASE_INTRO, TRAP_CHALLENGE, PAUSE, MAP, DIALOGUE, BATTLE, END -> true;
            case SETTINGS, CONTROLS -> settingsReturn == GameState.PAUSE;
            default -> false;
        };
        return screen == GameState.CINEMATIC ? AmbientAudio.Track.SILENCE
            : worldScreen ? AmbientAudio.Track.FOREST : AmbientAudio.Track.MENU;
    }

    @Override public void removeNotify() {
        close();
        super.removeNotify();
    }
    void drawText(Graphics2D g, String text, int x, int y, int size, Color color) {
        int actualSize = Math.max(12, size + textScale * 2);
        g.setFont(fonts.computeIfAbsent(actualSize, value -> new Font(Font.SANS_SERIF, Font.BOLD, value)));
        g.setColor(color); g.drawString(text, x, y);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            double scale = viewportScale();
            g.translate(viewportX(scale), viewportY(scale));
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawScreen(g);
            drawToast(g);
            if (simulationMode && screen != GameState.CINEMATIC && screen != GameState.END) {
                Rectangle control = SIMULATION_CONTROL;
                drawStandardPanel(g, control.x, control.y, control.width, control.height);
                drawCenteredText(g, "SIMULAÇÃO • F8 ou clique para avançar etapa • Progresso não será salvo",
                    W / 2, control.y + 17, 12, new Color(255,216,92));
            }
        } finally {
            g.dispose();
        }
    }

    private void drawScreen(Graphics2D g) {
        switch (screen) {
            case MENU -> drawMenu(g);
            case SLOTS -> drawSlots(g);
            case SAVE_OPTIONS -> { drawMenu(g); drawPopupDim(g); drawSaveOptions(g); }
            case PHASE_SELECT -> { drawMenu(g); drawPopupDim(g); drawPhaseSelect(g); }
            case SAVE_CONFIRM -> drawSaveConfirmationScreen(g);
            case NAME -> drawName(g);
            case CHARACTER_SELECT -> drawCharacterSelection(g);
            case PHASE_INTRO -> drawPhaseIntro(g);
            case BATTLE -> {
                drawImageCover(g, menuBackground, 0, 0, W, H);
                g.setColor(new Color(0, 0, 0, 82));
                g.fillRect(0, 0, W, H);
                drawBattle(g);
            }
            case SETTINGS, CONTROLS -> {
                drawSettingsUnderlay(g);
                drawPopupDim(g);
                if (screen == GameState.SETTINGS) drawSettings(g);
                else drawControls(g);
            }
            case EXTRAS -> { drawMenu(g); drawPopupDim(g); drawExtras(g); }
            case WORLD -> drawWorld(g);
            case TRAP_CHALLENGE -> { drawWorld(g); drawTrapChallenge(g); }
            case PAUSE -> { drawWorld(g); drawPause(g); }
            case MAP -> { drawWorld(g); drawMap(g); }
            case DIALOGUE -> { drawWorld(g); drawDialogue(g); }
            case END -> drawEnd(g);
            case CINEMATIC -> { /* The embedded JavaFX panel owns this screen. */ }
        }
    }

    private void drawToast(Graphics2D g) {
        if (toast.isEmpty()) return;
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRoundRect(350, 645, 580, 42, 10, 10);
        drawText(g, toast, 380, 673, 16, Color.WHITE);
    }

    private void drawMenu(Graphics2D g) {
        drawImageCover(g, menuBackground, 0, 0, W, H);
        for (int i = 0; i < menuImages.length; i++) {
            BufferedImage button = menuImages[i];
            if (button == null) continue;
            int width = button.getWidth(), height = button.getHeight();
            int drawX = i == 3 ? 50 : 1091 - width / 2;
            int row = i == 4 ? 3 : i;
            int drawY = i == 3 ? 600 : 260 + row * 92 + (64 - height) / 2;
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
        if (Boolean.parseBoolean(save.getProperty("campaignComplete", "false"))) return "FASE FINAL - CELEBRAÇÃO DA FLORESTA";
        int phase = Math.max(0, Math.min(Phase.values().length - 1, intValue(save, "phase", 0)));
        return Phase.values()[phase].title;
    }
    private int characterCardAt(int x, int y) {
        if (inside(x, y, 280, 210, 320, 300)) return 0;
        if (inside(x, y, 680, 210, 320, 300)) return 1;
        return -1;
    }

    private void drawCharacterSelection(Graphics2D g) {
        drawImageCover(g, menuBackground, 0, 0, W, H);
        drawPopupDim(g);
        drawStandardPanel(g, 180, 80, 920, 560);
        drawCenteredText(g, "ESCOLHA SEU PERSONAGEM", W / 2, 145, 30, new Color(255, 216, 92));
        for (CharacterDefinition character : CharacterDefinition.values()) {
            int cardX = character == CharacterDefinition.MALE ? 280 : 680;
            int centerX = cardX + 160;
            boolean selected = characterChoice == character;
            g.setColor(character.selectable() ? new Color(32, 66, 54, 230) : new Color(35, 39, 40, 240));
            g.fillRoundRect(cardX, 210, 320, 300, 18, 18);
            g.setColor(selected ? new Color(255, 216, 92) : new Color(114, 125, 115));
            g.setStroke(new java.awt.BasicStroke(selected ? 3 : 1));
            g.drawRoundRect(cardX, 210, 320, 300, 18, 18);
            g.setStroke(new java.awt.BasicStroke(1));
            if (character.selectable()) {
                characterPreview.draw(g, centerX, 415, 112, 160, true);
                drawCenteredText(g, "Disponível", centerX, 485, 16, new Color(159, 220, 166));
            } else {
                // A neutral lock illustration, never a male sprite standing in for unavailable art.
                g.setColor(new Color(102, 110, 110));
                g.fillOval(centerX - 30, 265, 60, 60);
                g.fillRoundRect(centerX - 46, 330, 92, 82, 30, 30);
                g.setColor(new Color(255, 216, 92));
                g.setStroke(new java.awt.BasicStroke(5));
                g.drawArc(centerX - 16, 341, 32, 39, 0, 180);
                g.fillRoundRect(centerX - 24, 360, 48, 36, 8, 8);
                g.setColor(new Color(55, 54, 40));
                g.fillOval(centerX - 4, 369, 8, 8);
                g.fillRect(centerX - 2, 375, 4, 10);
                g.setStroke(new java.awt.BasicStroke(1));
                drawCenteredText(g, "Em breve", centerX, 485, 18, new Color(255, 216, 92));
            }
            drawCenteredText(g, character.label(), centerX, 450, 25,
                character.selectable() ? Color.WHITE : new Color(170, 178, 175));
        }
        g.setColor(characterChoice.selectable() ? new Color(255, 216, 92) : new Color(60, 64, 62));
        g.fillRoundRect(470, 550, 340, 56, 12, 12);
        g.setColor(characterChoice.selectable() ? new Color(255, 236, 154) : new Color(110, 115, 110));
        g.drawRoundRect(470, 550, 340, 56, 12, 12);
        drawCenteredText(g, characterChoice.selectable() ? "Confirmar personagem" : "Personagem bloqueada",
            W / 2, 586, 20, characterChoice.selectable() ? new Color(25, 48, 51) : new Color(165, 170, 165));
        drawText(g, "< Voltar", 220, 596, 18, Color.WHITE);
        drawCenteredText(g, "A/D ou setas escolhem  •  Enter confirma  •  Esc volta", W / 2, 630, 16, Color.WHITE);
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

        int progress = 100; // Resources are already prepared; no simulated loading delay.
        g.setColor(new Color(255, 255, 255, 45)); g.fillRoundRect(530, 558, 610, 12, 8, 8);
        g.setColor(new Color(255, 216, 92)); g.fillRoundRect(530, 558, 610 * progress / 100, 12, 8, 8);
        drawText(g, progress < 100 ? "Preparando a trilha... " + progress + "%" : "Trilha pronta!",
            530, 601, 15, new Color(220, 237, 213));
        drawText(g, "Enter para iniciar a fase", 870, 620, 17, Color.WHITE);
    }

    private String phaseObjectiveText(int phase) {
        return switch (phase) {
            case 0 -> "Encontre as " + coins.size() + " sementes e desative as " + traps.size() + " armadilhas com E.";
            case 1 -> "Supere plataformas e obstáculos até encontrar o Saci.";
            case 2 -> "Use E nas fontes e nos focos. Encha o balde e apague os " + fires.size() + " incêndios.";
            default -> "Recolha os " + coins.size() + " resíduos e leve-os até o fim da trilha.";
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
            boolean selected = i == settingsIndex;
            drawGoldSelectableOption(g, 260, rowY, 760, 62, selected);
            Color rowText = selected ? new Color(25, 48, 51) : Color.WHITE;
            drawText(g, labels[i], 295, rowY + 39, 19, rowText);
            drawSettingArrow(g, 785, rowY + 17, 28, 30, true, selected);
            drawCenteredText(g, values[i], 870, rowY + 39, 18, selected ? new Color(60, 48, 22) : new Color(255, 239, 194));
            drawSettingArrow(g, 945, rowY + 17, 28, 30, false, selected);
        }
        drawCenteredText(g, "Esc fecha", W / 2, 605, 16, new Color(231, 224, 194));
    }

    private void drawSettingArrow(Graphics2D g, int x, int y, int width, int height, boolean left, boolean selected) {
        if (pauseIndicatorImage == null) {
            g.setColor(new Color(255, 216, 92));
            int direction = left ? -1 : 1;
            int centerX = x + width / 2, centerY = y + height / 2;
            g.setStroke(new java.awt.BasicStroke(3, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g.drawLine(centerX - direction * 5, centerY - 7, centerX + direction * 3, centerY);
            g.drawLine(centerX + direction * 3, centerY, centerX - direction * 5, centerY + 7);
            g.setStroke(new java.awt.BasicStroke(1));
        } else if (left) {
            g.drawImage(pauseIndicatorImage, x + width, y, x, y + height,
                0, 0, pauseIndicatorImage.getWidth(), pauseIndicatorImage.getHeight(), this);
        } else {
            drawImageFit(g, pauseIndicatorImage, x, y, width, height);
        }
    }
    private void drawControls(Graphics2D g) { g.setColor(new Color(0, 0, 0, 178)); g.fillRoundRect(250, 130, 780, 470, 18, 18); g.setColor(new Color(214,177,91,190)); g.drawRoundRect(250,130,780,470,18,18); drawText(g,"CONTROLES",520,205,29,new Color(255,216,92)); drawText(g,"A/D ou SETAS: correr",390,300,21,Color.WHITE); drawText(g,"W, SETA CIMA ou ESPACO: pular",390,355,21,Color.WHITE); drawText(g,"M: mapa   F5: salvar   Esc: pausar",390,410,20,Color.WHITE); drawCenteredText(g,"Clique ou pressione Esc para voltar",W/2,535,16,new Color(245,230,190)); }
    private void drawExtras(Graphics2D g) {
        drawStandardPanel(g, 250, 145, 780, 470);
        drawCenteredText(g, "AJUDA E INFORMAÇÕES", W / 2, 238, 30, new Color(255, 216, 92));
        g.setColor(new Color(214, 177, 91, 120));
        g.drawLine(330, 265, 950, 265);
        drawParagraph(g,
            "Uma aventura inspirada nas lendas da floresta brasileira. Proteja a mata, respeite os animais e siga as orientações de cada guardião.",
            330, 325, 620, 32, Color.WHITE, 20);
        drawGoldSelectableOption(g, 315, 440, 650, 55, false);
        drawCenteredText(g, "Simular campanha • T • não altera seus saves", W/2, 475, 18, Color.WHITE);
        drawGoldSelectableOption(g, 315, 505, 650, 50, false);
        drawCenteredText(g, "Créditos • C", W/2, 538, 18, Color.WHITE);
        drawCenteredText(g, "Esc para voltar", W / 2, 587, 16, new Color(245, 230, 190));
    }

    private void drawSaveOptions(Graphics2D g) {
        drawStandardPanel(g, 250, 165, 780, 390);
        drawCenteredText(g, "CONTINUAR CAMPANHA", W / 2, 235, 30, new Color(255, 216, 92));
        String[] labels = { "Continuar de onde parou", "Escolher fase desbloqueada" };
        for (int i = 0; i < labels.length; i++) {
            int rowY = 275 + i * 92;
            drawGoldSelectableOption(g, 300, rowY, 680, 72, i == saveOptionIndex);
            drawText(g, labels[i], 345, rowY + 45, 21,
                i == saveOptionIndex ? new Color(25, 48, 51) : Color.WHITE);
        }
        drawCenteredText(g, "Esc volta aos saves", W / 2, 510, 16, new Color(205, 225, 195));
    }
    private void drawPhaseSelect(Graphics2D g) {
        drawStandardPanel(g, 190, 105, 900, 520);
        drawCenteredText(g, "SELECIONAR FASE", W / 2, 155, 29, new Color(255, 216, 92));
        for (int i = 0; i <= Phase.values().length; i++) {
            int rowY = 205 + i * 75;
            boolean available = i < Phase.values().length ? i <= unlockedPhase : Boolean.parseBoolean(readSave(selectedSaveSlot).getProperty("campaignComplete", "false"));
            boolean selected = i == phaseSelectIndex && available;
            if (available) drawGoldSelectableOption(g, 215, rowY, 850, 60, selected);
            else {
                g.setColor(new Color(55, 65, 58, 190));
                g.fillRoundRect(215, rowY, 850, 60, 10, 10);
            }
            Color textColor = selected ? new Color(25, 48, 51)
                : available ? Color.WHITE : new Color(175, 175, 160);
            drawText(g, i == Phase.values().length ? "FASE FINAL - CELEBRA\u00c7\u00c3O" : Phase.values()[i].title, 250, rowY + 31, 17, textColor);
            drawText(g, available ? "DESBLOQUEADA" : "BLOQUEADA", 805, rowY + 51, 13,
                selected ? new Color(60, 48, 22) : new Color(255, 222, 153));
        }
        drawCenteredText(g, "Voltar", W / 2, 604, 17, Color.WHITE);
    }
    private void drawSaveConfirmation(Graphics2D g) {
        drawStandardPanel(g, 275, 180, 730, 360);
        String title = confirmAction == ConfirmAction.DELETE_SAVE ? "APAGAR ESTE SAVE?"
            : confirmAction == ConfirmAction.RESTART_PHASE ? "REINICIAR NESTA FASE?" : "COMEÇAR NOVA CAMPANHA?";
        drawCenteredText(g, title, W / 2, 245, 28, new Color(255, 216, 92));
        String message = confirmAction == ConfirmAction.DELETE_SAVE ? "O progresso deste slot será removido."
            : confirmAction == ConfirmAction.RESTART_PHASE ? "O progresso atual será substituído pelo início da fase escolhida."
            : "O progresso salvo neste slot será substituído.";
        drawParagraph(g, message, 365, 310, 550, 28, Color.WHITE, 19);
        String[] labels = confirmAction == ConfirmAction.DELETE_SAVE ? new String[] { "Apagar", "Cancelar" }
            : confirmAction == ConfirmAction.RESTART_PHASE ? new String[] { "Reiniciar", "Cancelar" }
            : new String[] { "Continuar", "Cancelar" };
        for (int i = 0; i < labels.length; i++) {
            int x = 310 + i * 360;
            drawStandardOption(g, x, 395, 300, 70, settingsIndex == i);
            drawCenteredText(g, labels[i], x + 150, 438, 19,
                settingsIndex == i ? new Color(25, 48, 51) : Color.WHITE);
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
        drawGoldSelectableOption(g, x, y, width, height, selected);
    }

    private void drawGoldSelectableOption(Graphics2D g, int x, int y, int width, int height, boolean selected) {
        g.setColor(selected ? new Color(255, 216, 92) : new Color(0, 0, 0, 28));
        g.fillRoundRect(x, y, width, height, 12, 12);
        g.setColor(selected ? new Color(255, 236, 154) : new Color(214, 177, 91, 105));
        g.setStroke(new java.awt.BasicStroke(selected ? 2 : 1));
        g.drawRoundRect(x, y, width, height, 12, 12);
        g.setStroke(new java.awt.BasicStroke(1));
    }

    private void drawWorld(Graphics2D g) {
        worldRenderer.draw(g);
        if (celebrationActive) {
            drawGoldSelectableOption(g,1040,12,220,50,false);
            drawCenteredText(g,"Encerrar • Enter",1150,44,15,Color.WHITE);
        }
    }

    int coinY(Thing coin) {
        return currentPhase == 3 ? coin.y : (int) (420 + Math.sin(elapsed * 3 + coin.x) * 30);
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 120)); g.fillRect(0, 0, W, H);
        g.setColor(new Color(0, 0, 0, 145)); g.fillRoundRect(310, 130, 660, 470, 18, 18);
        if (pauseTitleImage != null) drawImageFit(g, pauseTitleImage, 490, 158, 300, 76);
        String[] options = { "Continuar", "Salvar", "Configuracoes", "Mapa", "Sair para o menu" };
        for (int i = 0; i < options.length; i++) {
            int rowY = 275 + i * 58;
            if (i == menuIndex) {
                g.setColor(new Color(255, 216, 92));
                g.fillRoundRect(405, rowY, 470, 50, 10, 10);
                g.setColor(new Color(255, 236, 154));
                g.drawRoundRect(405, rowY, 470, 50, 10, 10);
                if (pauseIndicatorImage != null) drawImageFit(g, pauseIndicatorImage, 429, rowY + 11, 22, 28);
            }
            drawText(g, options[i], 485, rowY + 33, 21,
                i == menuIndex ? new Color(25, 48, 51) : Color.WHITE);
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

    private void drawTrapChallenge(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 125));
        g.fillRect(0, 0, W, H);
        drawStandardPanel(g, 180, 185, 920, 385);
        drawCenteredText(g, "DESATIVE A ARMADILHA", W / 2, 242, 30, new Color(255, 216, 92));
        drawCenteredText(g, "Acerte a bolinha dourada quando ela passar pelo campo verde",
            W / 2, 284, 18, Color.WHITE);
        drawCenteredText(g, "ETAPA " + trapChallengeStage + " DE 3", W / 2, 326, 17,
            new Color(255, 226, 125));

        int trackX = 280, trackY = 355, trackWidth = 720, trackHeight = 34;
        g.setColor(new Color(7, 19, 18, 235));
        g.fillRoundRect(trackX, trackY, trackWidth, trackHeight, 17, 17);
        int targetX = trackX + (int) Math.round((trapTargetCenter - trapTargetHalfWidth) * trackWidth);
        int targetWidth = (int) Math.round(trapTargetHalfWidth * 2 * trackWidth);
        g.setColor(new Color(62, 166, 92));
        g.fillRoundRect(targetX, trackY + 3, targetWidth, trackHeight - 6, 13, 13);
        g.setColor(new Color(145, 230, 155, 180));
        g.drawRoundRect(targetX, trackY + 3, targetWidth, trackHeight - 6, 13, 13);

        int ballX = trackX + (int) Math.round(trapBallPosition * trackWidth);
        int ballY = trackY + trackHeight / 2;
        g.setColor(new Color(255, 205, 55, 90));
        g.fillOval(ballX - 17, ballY - 17, 34, 34);
        g.setColor(new Color(255, 216, 92));
        g.fillOval(ballX - 11, ballY - 11, 22, 22);
        g.setColor(new Color(255, 246, 183));
        g.fillOval(ballX - 6, ballY - 7, 7, 7);

        for (int stage = 1; stage <= 3; stage++) {
            int centerX = W / 2 + (stage - 2) * 66;
            g.setColor(stage <= trapChallengeStage ? new Color(255, 216, 92) : new Color(82, 96, 83));
            g.fillOval(centerX - 10, 418, 20, 20);
            if (stage < 3) {
                g.setColor(stage < trapChallengeStage ? new Color(255, 216, 92) : new Color(82, 96, 83));
                g.fillRoundRect(centerX + 11, 426, 44, 4, 4, 4);
            }
        }
        if (!trapChallengeMessage.isEmpty())
            drawCenteredText(g, trapChallengeMessage, W / 2, 482, 16, new Color(205, 230, 192));
        drawCenteredText(g, "Enter, E, Espaço ou clique na faixa confirma   •   Esc cancela",
            W / 2, 535, 15, new Color(238, 229, 199));
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
        drawText(g, campaignComplete ? "CELEBRAÇÃO DA FLORESTA • DESBLOQUEADA" : "CELEBRAÇÃO • CONCLUA OS QUATRO CAPÍTULOS", 350, 485, 18, new Color(255,216,92));
    }
    private void drawDialogue(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 48)); g.fillRect(0, 0, W, H);
        drawStandardPanel(g, 60, 455, 1160, 205);
        BufferedImage portrait = guardianImages[dialogueGuardian];
        if (portrait != null) drawImageFit(g, portrait, 88, 410, 165, 190);
        drawText(g, Phase.values()[dialogueGuardian].guardian, 275, 510, 23, new Color(255, 216, 92));
        drawParagraph(g, dialogue[dialogueIndex], 275, 552, 865, 27, Color.WHITE, 19);
        drawText(g, "Enter / E / Espaço continua", 875, 632, 14, new Color(205, 225, 195));
        drawText(g, "Esc / Voltar à trilha", 280, 632, 15, new Color(255, 216, 92));
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
                g.setColor(new Color(255, 216, 92)); g.fillRoundRect(135, rowY, 1010, 54, 10, 10);
                g.setColor(new Color(255, 236, 154)); g.drawRoundRect(135, rowY, 1010, 54, 10, 10);
                if (pauseIndicatorImage != null) drawImageFit(g, pauseIndicatorImage, 155, rowY + 12, 22, 30);
            }
            drawText(g, (i + 1) + "  |  " + question.options()[i], 210, rowY + 35, 18,
                i == battleChoice ? new Color(25, 48, 51) : Color.WHITE);
        }
        drawText(g, "Esc / Voltar à trilha", 135, 630, 15, new Color(255, 216, 92));
        drawText(g, "Setas ou 1-4 selecionam   •   Enter confirma", 640, 630, 15, new Color(205, 225, 195));
    }

    private int battleChoiceAt(int x, int y) {
        if (x < 135 || x > 1145) return -1;
        for (int i = 0; i < 4; i++) {
            int rowY = 310 + i * 66;
            if (y >= rowY && y <= rowY + 54) return i;
        }
        return -1;
    }
    private void drawEnd(Graphics2D g) {
        drawImageCover(g, menuBackground, 0, 0, W, H);
        g.setColor(new Color(0, 0, 0, 112));
        g.fillRect(0, 0, W, H);
        drawStandardPanel(g, 130, 105, 1020, 510);

        drawCenteredText(g, "ENTRE MITOS E RAÍZES", W/2, 190, 34, new Color(255,216,92));
        drawCenteredText(g, "CRIADORES", W/2, 265, 23, new Color(255,226,125));
        for (int i=0; i<GameCredits.CREATORS.size(); i++)
            drawCenteredText(g, GameCredits.CREATORS.get(i), W/2, 330+i*62, 25, Color.WHITE);
        drawCenteredText(g, "Obrigado por caminhar ao lado da natureza.", W/2, 485, 20, new Color(227,238,219));

        g.setColor(new Color(255, 216, 92));
        g.fillRoundRect(480, 535, 320, 64, 12, 12);
        g.setColor(new Color(255, 216, 92, 235));
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawRoundRect(480, 535, 320, 64, 12, 12);
        g.setStroke(new java.awt.BasicStroke(1));
        drawCenteredText(g, "Voltar ao menu", 640, 575, 18, new Color(25, 48, 51));
    }

    private int firstSave() { return hasSave(0) ? 0 : hasSave(1) ? 1 : -1; }
    private boolean hasSave(int slot) { return saveManager.hasSave(slot); }
    private Properties readSave(int slot) { return saveManager.read(slot); }
    private void saveGame() {
        if (simulationMode) return;
        Properties p = new Properties();
        p.setProperty("character", selectedCharacter.id());
        p.setProperty("celebration", String.valueOf(celebrationActive));
        p.setProperty("quizComplete", String.valueOf(phaseQuizComplete));
        p.setProperty("traversalComplete", String.valueOf(traversalComplete));
        p.setProperty("pendingPrologue", String.valueOf(pendingPrologue));
        p.setProperty("pendingChapter", String.valueOf(pendingChapter));
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
        restoreSave(slot);
        if (campaignComplete) openCelebration(Boolean.parseBoolean(readSave(slot).getProperty("celebration", "false")));
        else if (phaseQuizComplete) showGuardianThanks();
        else if (pendingPrologue) startOpening(GameState.MENU);
        else if (pendingChapter) startChapter();
        else openPhaseIntro();
    }

    private void restoreSave(int slot) {
        Properties p = readSave(slot);
        slotIndex = slot;
        pendingPrologue = Boolean.parseBoolean(p.getProperty("pendingPrologue", "false"));
        pendingChapter = Boolean.parseBoolean(p.getProperty("pendingChapter", "false"));
        selectedCharacter = CharacterDefinition.fromSave(p.getProperty("character"));
        playerAnimator = new PlayerAnimator(selectedCharacter);
        resetTransientMovement();
        name = p.getProperty("name", "Viajante");
        currentPhase = Math.max(0, Math.min(Phase.values().length - 1, intValue(p, "phase", 0)));
        unlockedPhase = Math.max(currentPhase, Math.min(Phase.values().length - 1, intValue(p, "unlockedPhase", currentPhase)));
        campaignComplete = Boolean.parseBoolean(p.getProperty("campaignComplete", "false"));
        celebrationActive = campaignComplete && Boolean.parseBoolean(p.getProperty("celebration", "false"));
        phaseQuizComplete = Boolean.parseBoolean(p.getProperty("quizComplete", "false"));
        traversalComplete = Boolean.parseBoolean(p.getProperty("traversalComplete", "false")) || currentPhase == 1 && doubleValue(p,"x",110) >= 3020;
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
    }

    private int intValue(Properties properties, String key, int fallback) {
        try { return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private double doubleValue(Properties properties, String key, double fallback) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, String.valueOf(fallback)));
            return Double.isFinite(value) ? value : fallback;
        }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private long longValue(Properties properties, String key, long fallback) {
        try { return Long.parseUnsignedLong(properties.getProperty(key, Long.toHexString(fallback)), 16); }
        catch (NumberFormatException ignored) { return fallback; }
    }
    private void deleteSave(int slot) { if (saveManager.exists(slot)) { if (saveManager.delete(slot)) showToast("Slot apagado."); else showToast("Nao foi possivel apagar este slot."); } screen = GameState.SLOTS; }
    private void showToast(String message) { toast = message; toastUntil = System.currentTimeMillis() + 2200; repaint(); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }


}
