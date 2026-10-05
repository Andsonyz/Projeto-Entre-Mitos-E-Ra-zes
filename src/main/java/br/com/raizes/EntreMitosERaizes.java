package br.com.raizes;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
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

    private enum Screen { MENU, SLOTS, SAVE_OPTIONS, PHASE_SELECT, SAVE_CONFIRM, NAME, PHASE_INTRO, WORLD, PAUSE, SETTINGS, CONTROLS, EXTRAS, MAP, DIALOGUE, BATTLE, PHASE_RESULT, END }
    private enum ConfirmAction { NEW_CAMPAIGN, DELETE_SAVE, RESTART_PHASE }

    private record Question(String prompt, String[] options, int correctOption) { }

    private enum Phase {
        CURUPIRA("FASE 1 - OS RASTROS DA CAÇA", "Curupira", "Caça ilegal", "Guardião de cabelos de fogo e pés invertidos, protege os animais e confunde quem ameaça a mata.", "Image/Personagens/guardioes/CURUPIRA.PNG"),
        SACI("FASE 2 - A TRAVESSIA DO SACI", "Saci", "Responsabilidade ambiental", "Menino de uma perna só, senhor dos redemoinhos. Sua travessura lembra que nossas escolhas deixam marcas na natureza.", "Image/Personagens/guardioes/SACI.png"),
        CAIPORA("FASE 3 - ENTRE CINZAS E RAÍZES", "Caipora", "Queimadas", "Guardiã dos animais e da mata, percorre a floresta para proteger a vida. Ajude-a a apagar os focos de incêndio.", "Image/Personagens/guardioes/CAIPORA.png"),
        NEGRINHO("FASE 4 - OS RASTROS DO LIXO", "Negrinho do Pastoreio", "Poluição e descarte", "Figura do folclore gaúcho, ligada às estrelas e à busca do que se perdeu. Vamos reencontrar uma floresta sufocada por resíduos.", "Image/Personagens/guardioes/NEGRINHODOPASTOREIO.png");

        final String title, guardian, theme, legend, portrait;
        Phase(String title, String guardian, String theme, String legend, String portrait) {
            this.title = title; this.guardian = guardian; this.theme = theme; this.legend = legend; this.portrait = portrait;
        }
    }

    private static final class Game extends JPanel {
        private static final int W = 1280, H = 720, WORLD_W = 3200;
        private static final int GROUND_Y = 600, GROUND_OVERDRAW = 12, QUESTIONS_PER_ROUND = 5;
        private static final double ENEMY_SPEED = 1.4;
        private static final Color[] PHASE_FILTERS = {
            new Color(42, 72, 35, 28), new Color(40, 62, 112, 38),
            new Color(116, 45, 28, 78), new Color(70, 74, 63, 58)
        };
        private static final Question[][] QUESTION_BANK = {
            {
                new Question("O que fazer ao encontrar uma armadilha na mata?", new String[] { "Desarmar sem cuidado", "Afastar-se e avisar as autoridades", "Levar para casa", "Esconder a armadilha" }, 1),
                new Question("Por que a caça ilegal prejudica a floresta?", new String[] { "Reduz as populacoes e desequilibra os ecossistemas", "Ajuda os animais a migrar", "Aumenta a variedade de especies", "Nao causa impacto" }, 0),
                new Question("Encontrou um animal silvestre ferido. Qual a atitude adequada?", new String[] { "Tentar domestica-lo", "Oferecer qualquer alimento", "Manter distancia e procurar ajuda especializada", "Solta-lo em outro bairro" }, 2),
                new Question("Animais silvestres devem viver principalmente...", new String[] { "Em casas", "Em seu habitat natural", "Em lojas", "Em qualquer cidade" }, 1),
                new Question("Ao suspeitar de comercio de animais silvestres, voce deve...", new String[] { "Comprar para salvar o animal", "Confrontar o suspeito sozinho", "Divulgar o endereco do suspeito", "Registrar a denuncia aos orgaos competentes" }, 3),
                new Question("Qual atitude ajuda a proteger a fauna?", new String[] { "Preservar habitats e respeitar os animais", "Alimentar animais com doces", "Retirar filhotes da mata", "Manter animais silvestres como pets" }, 0),
                new Question("Por que nao devemos tocar em uma armadilha encontrada?", new String[] { "Ela pode ser perigosa e apagar evidencias", "Ela pode ficar mais colorida", "Animais nao percebem", "Apenas por causa do barulho" }, 0),
                new Question("O que a biodiversidade representa?", new String[] { "A quantidade de trilhas", "A variedade de seres vivos e suas relacoes", "Somente o numero de arvores", "Apenas os animais grandes" }, 1)
            },
            {
                new Question("Qual atitude reduz o impacto de um passeio na natureza?", new String[] { "Sair das trilhas demarcadas", "Levar o lixo de volta e respeitar a sinalizacao", "Retirar plantas como lembranca", "Alimentar os animais" }, 1),
                new Question("Como economizar agua em casa?", new String[] { "Deixar a torneira aberta", "Lavar a calcada diariamente", "Fechar a torneira enquanto escova os dentes", "Tomar banhos mais longos" }, 2),
                new Question("Ao ver lixo na trilha, o que fazer?", new String[] { "Levar e descartar corretamente, se for seguro", "Empurrar para o mato", "Queimar no local", "Jogar no rio" }, 0),
                new Question("Qual escolha ajuda a reduzir residuos?", new String[] { "Usar descartaveis sempre", "Comprar apenas pelo excesso", "Reutilizar objetos quando possivel", "Deixar embalagens na rua" }, 2),
                new Question("Como devemos observar animais silvestres?", new String[] { "De perto e fazendo barulho", "A distancia, sem alimentar nem perseguir", "Carregando-os no colo", "Usando flash bem proximo" }, 1),
                new Question("Qual e uma forma responsavel de descartar reciclaveis?", new String[] { "Separar conforme a coleta local", "Enterrar no parque", "Jogar no bueiro", "Misturar no rio" }, 0),
                new Question("Por que apagar as luzes que nao estao sendo usadas?", new String[] { "Para aumentar o desperdicio", "Para economizar energia", "Para aquecer a floresta", "Nao faz diferenca" }, 1),
                new Question("Como incentivar o cuidado ambiental na comunidade?", new String[] { "Compartilhar informacao confiavel e dar exemplo", "Espalhar boatos", "Ignorar problemas locais", "Destruir placas educativas" }, 0)
            },
            {
                new Question("Ao perceber um incendio florestal, qual deve ser a prioridade?", new String[] { "Aproximar-se para filmar", "Afastar-se para um local seguro e chamar emergencia", "Tentar atravessar a fumaca", "Esconder o fogo" }, 1),
                new Question("Por que nao se deve queimar folhas ou lixo?", new String[] { "O fogo pode se espalhar e gerar fumaça nociva", "Isso protege o solo", "A fumaça limpa o ar", "So e perigoso a noite" }, 0),
                new Question("O que pode aumentar o risco de queimadas?", new String[] { "Vegetacao seca e vento", "Coleta seletiva", "Plantio de mudas", "Chuva leve" }, 0),
                new Question("Como ajudar a prevenir incendios na mata?", new String[] { "Jogar bitucas no chao", "Fazer fogueiras sem supervisao", "Nao deixar fontes de ignicao e seguir orientacoes locais", "Queimar entulho" }, 2),
                new Question("Qual impacto uma queimada pode causar?", new String[] { "Perda de habitat e morte de animais", "Aumento permanente de agua limpa", "Protecao de ninhos", "Nenhum efeito no solo" }, 0),
                new Question("Se o fogo estiver proximo, para onde ir?", new String[] { "Para a direcao da fumaca", "Para uma area segura, longe do fogo e da fumaca", "Para dentro da vegetacao fechada", "Para perto das chamas" }, 1),
                new Question("Por que a vegetacao nativa e importante?", new String[] { "Abriga especies e protege o solo e a agua", "Serve apenas como decoracao", "Impede toda a chuva", "Nao tem funcao ecologica" }, 0),
                new Question("Quem deve combater um grande incendio florestal?", new String[] { "Qualquer pessoa sem equipamento", "Equipes treinadas e servicos de emergencia", "Criancas desacompanhadas", "Animais da mata" }, 1)
            },
            {
                new Question("Onde descartar uma garrafa reciclavel?", new String[] { "Em um rio", "Na coleta seletiva apropriada", "No meio da trilha", "Enterrada na mata" }, 1),
                new Question("O que fazer com pilhas e baterias usadas?", new String[] { "Levar a um ponto de coleta apropriado", "Jogar no lixo organico", "Deixar na praia", "Quebrar e enterrar" }, 0),
                new Question("Como reduzir o uso de embalagens descartaveis?", new String[] { "Usar sacolas reutilizaveis", "Pedir embalagem extra", "Jogar embalagens no chao", "Comprar itens duplos" }, 0),
                new Question("Por que o plastico no ambiente e perigoso?", new String[] { "Pode ferir animais e contaminar ecossistemas", "Alimenta todos os animais", "Desaparece imediatamente", "Purifica a agua" }, 0),
                new Question("Qual e a ordem mais conhecida dos 3Rs?", new String[] { "Recolher, rasgar, remover", "Reduzir, reutilizar, reciclar", "Repor, regar, remover", "Repetir, reter, raspar" }, 1),
                new Question("Restos de alimentos podem ser destinados a...", new String[] { "Compostagem, quando disponivel", "Bueiros", "Rios", "Trilhas" }, 0),
                new Question("O que fazer se encontrar lixo perigoso na trilha?", new String[] { "Tocar para identificar", "Deixar no local e avisar a equipe responsavel", "Chutar para longe", "Queimar" }, 1),
                new Question("Qual escolha ajuda a manter rios limpos?", new String[] { "Descartar oleo na pia", "Nao jogar residuos e usar descarte correto", "Lavar embalagens no rio", "Jogar papel na agua" }, 1)
            }
        };
        private final Set<Integer> held = new HashSet<>();
        private final List<Rectangle> platforms = new ArrayList<>();
        private final List<Thing> coins = new ArrayList<>();
        private final List<Thing> enemies = new ArrayList<>();
        private final List<Thing> fires = new ArrayList<>();
        private final List<Thing> traps = new ArrayList<>();
        private final List<TreeDecoration> trees = new ArrayList<>();
        private final List<Question> quizQuestions = new ArrayList<>();
        private final Map<Integer, BufferedImage> platformArtwork = new java.util.HashMap<>();
        private final Timer timer;
        private Screen screen = Screen.MENU;
        private Screen settingsReturn = Screen.MENU;
        private Screen confirmReturn = Screen.SLOTS;
        private ConfirmAction confirmAction;
        private BufferedImage playerImage, playerWalk1, playerWalk2, worldBackground, groundTile, menuBackground, menuTitle;
        private BufferedImage platformImage;
        private final BufferedImage[] treeImages = new BufferedImage[3];
        private final BufferedImage[] guardianImages = new BufferedImage[Phase.values().length];
        private final BufferedImage[] menuImages = new BufferedImage[5];
        private final Rectangle[] menuBounds = new Rectangle[5];
        private final Rectangle[] menuOpaqueBounds = new Rectangle[5];
        private final double[] menuScale = { 1, 1, 1, 1, 1 };
        private String name = "Viajante", toast = "";
        private String[] dialogue = new String[0];
        private int menuIndex, slotIndex, settingsIndex, saveOptionIndex, phaseSelectIndex, lives = 3, coinsCollected, battleChoice, quizIndex;
        private int selectedSaveSlot;
        private int pendingPhase;
        private int currentPhase, unlockedPhase, completedPhase = -1;
        private int textScale = 1;
        private boolean contrast, nameConfirm, won, onGround, bucketFull, exitBlocked, campaignComplete, choosingContinue;
        private boolean facingRight = true, walking;
        private int walkingFrame;
        private double x = 110, y = 500, vy, camera, elapsed;
        private int dialogueIndex;
        private long toastUntil;
        private Question previousQuizFirst;
        private long vegetationGeneration;

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
                @Override public void mouseMoved(MouseEvent e) { updateMouseHover(e.getX(), e.getY()); }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { if (e.getButton() == MouseEvent.BUTTON1) handleMouseClick(e.getX(), e.getY()); }
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
            try { return ImageIO.read(new File(path)); }
            catch (IOException ignored) { return null; }
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
                int existingCenter = tree.x + tree.image.getWidth() / 2;
                if (Math.abs(centerX - existingCenter) < (treeWidth + tree.image.getWidth()) / 2 + 80) return false;
            }
            return true;
        }

        private void tick() {
            double dt = 0.016;
            elapsed += dt;
            updateMenuScale();
            if (screen == Screen.WORLD) updateWorld(dt);
            if (toastUntil < System.currentTimeMillis()) toast = "";
            repaint();
        }

        private void updateMenuScale() {
            int selected = screen == Screen.MENU ? menuIndex : -1;
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
                enemy.x += (int) Math.round(Math.sin(elapsed + enemy.x) * ENEMY_SPEED);
                Rectangle enemyRect = new Rectangle(enemy.x - 20, 552, 40, 48);
                if (body.intersects(enemyRect)) {
                    if (vy > 0 && y + 58 < 575) { enemy.hit = true; vy = -360; }
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
            screen = Screen.DIALOGUE;
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
            if (screen == Screen.MENU) menuKey(key);
            else if (screen == Screen.SLOTS) slotsKey(key);
            else if (screen == Screen.SAVE_OPTIONS) saveOptionsKey(key);
            else if (screen == Screen.PHASE_SELECT) phaseSelectKey(key);
            else if (screen == Screen.SAVE_CONFIRM) saveConfirmKey(key);
            else if (screen == Screen.NAME) nameKey(key);
            else if (screen == Screen.PHASE_INTRO) { if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) screen = Screen.WORLD; else if (key == KeyEvent.VK_ESCAPE) returnToMenu(); }
            else if (screen == Screen.WORLD) worldKey(key);
            else if (screen == Screen.PAUSE) pauseKey(key);
            else if (screen == Screen.SETTINGS) settingsKey(key);
            else if (screen == Screen.CONTROLS) { if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) screen = Screen.SETTINGS; }
            else if (screen == Screen.EXTRAS) { if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) returnToMenu(); }
            else if (screen == Screen.DIALOGUE) dialogueKey(key);
            else if (screen == Screen.BATTLE) battleKey(key);
            else if (screen == Screen.PHASE_RESULT) { if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) screen = Screen.PHASE_INTRO; else if (key == KeyEvent.VK_ESCAPE) returnToMenu(); }
            else if (screen == Screen.END) { if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) returnToMenu(); }
            else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_M) screen = screen == Screen.MAP ? Screen.WORLD : Screen.MENU;
        }

        private void menuKey(int key) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) menuIndex = menuIndex < 0 ? 4 : (menuIndex + 4) % 5;
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) menuIndex = menuIndex < 0 ? 0 : (menuIndex + 1) % 5;
            if ((key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) && menuIndex >= 0) {
                if (menuIndex == 0 || menuIndex == 1) { choosingContinue = menuIndex == 1; slotIndex = choosingContinue && firstSave() >= 0 ? firstSave() : 0; screen = Screen.SLOTS; }
                else if (menuIndex == 2) { settingsReturn = Screen.MENU; screen = Screen.SETTINGS; }
                else if (menuIndex == 3) screen = Screen.EXTRAS;
                else System.exit(0);
            }
        }

        private void slotsKey(int key) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) slotIndex = 1 - slotIndex;
            if (key == KeyEvent.VK_DELETE && hasSave(slotIndex)) { confirmAction = ConfirmAction.DELETE_SAVE; confirmReturn = Screen.SLOTS; settingsIndex = 0; screen = Screen.SAVE_CONFIRM; }
            if (key == KeyEvent.VK_ESCAPE) returnToMenu();
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                if (choosingContinue) {
                    if (hasSave(slotIndex)) { selectedSaveSlot = slotIndex; saveOptionIndex = 0; screen = Screen.SAVE_OPTIONS; }
                    else showToast("Este slot ainda nao possui uma campanha salva.");
                } else if (hasSave(slotIndex)) { confirmAction = ConfirmAction.NEW_CAMPAIGN; confirmReturn = Screen.SLOTS; settingsIndex = 0; screen = Screen.SAVE_CONFIRM; }
                else { name = ""; nameConfirm = false; screen = Screen.NAME; }
            }
        }

        private void saveOptionsKey(int key) {
            if (key == KeyEvent.VK_ESCAPE) { screen = Screen.SLOTS; return; }
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_W || key == KeyEvent.VK_A || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_S || key == KeyEvent.VK_D) saveOptionIndex = 1 - saveOptionIndex;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                if (saveOptionIndex == 0) loadSave(selectedSaveSlot);
                else { Properties save = readSave(selectedSaveSlot); unlockedPhase = Math.max(0, Math.min(Phase.values().length - 1, intValue(save, "unlockedPhase", intValue(save, "phase", 0)))); phaseSelectIndex = 0; screen = Screen.PHASE_SELECT; }
            }
        }

        private void phaseSelectKey(int key) {
            if (key == KeyEvent.VK_ESCAPE) { screen = Screen.SAVE_OPTIONS; return; }
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_W || key == KeyEvent.VK_A) phaseSelectIndex = (phaseSelectIndex + Phase.values().length - 1) % Phase.values().length;
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_S || key == KeyEvent.VK_D) phaseSelectIndex = (phaseSelectIndex + 1) % Phase.values().length;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) selectSavedPhase(phaseSelectIndex);
        }

        private void selectSavedPhase(int phase) {
            Properties save = readSave(selectedSaveSlot);
            unlockedPhase = Math.max(0, Math.min(Phase.values().length - 1, intValue(save, "unlockedPhase", intValue(save, "phase", 0))));
            if (phase > unlockedPhase) { showToast("Essa fase ainda esta bloqueada."); return; }
            pendingPhase = phase; confirmAction = ConfirmAction.RESTART_PHASE; confirmReturn = Screen.PHASE_SELECT; settingsIndex = 0; screen = Screen.SAVE_CONFIRM;
        }

        private void startSavedPhase(int phase) {
            loadSave(selectedSaveSlot);
            currentPhase = phase;
            x = 110; y = 500; vy = 0; lives = 3; coinsCollected = 0; bucketFull = false; campaignComplete = false;
            buildLevel(); camera = 0; saveGame(); screen = Screen.PHASE_INTRO;
        }

        private void saveConfirmKey(int key) {
            if (key == KeyEvent.VK_ESCAPE) { screen = confirmReturn; return; }
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_A || key == KeyEvent.VK_D) settingsIndex = 1 - settingsIndex;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
                if (settingsIndex == 0) {
                    if (confirmAction == ConfirmAction.DELETE_SAVE) deleteSave(slotIndex);
                    else if (confirmAction == ConfirmAction.RESTART_PHASE) startSavedPhase(pendingPhase);
                    else { name = ""; nameConfirm = false; screen = Screen.NAME; }
                } else screen = confirmReturn;
                settingsIndex = 0;
            }
        }

        private void nameKey(int key) {
            if (key == KeyEvent.VK_BACK_SPACE && !nameConfirm && !name.isEmpty()) name = name.substring(0, name.length() - 1);
            else if (key == KeyEvent.VK_ESCAPE) screen = Screen.SLOTS;
            else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (nameConfirm) startGame(); else nameConfirm = true; }
        }

        private void worldKey(int key) {
            if (key == KeyEvent.VK_ESCAPE) { menuIndex = 0; screen = Screen.PAUSE; }
            else if (key == KeyEvent.VK_F5) saveGame();
            else if (key == KeyEvent.VK_M) screen = Screen.MAP;
            else if (key == KeyEvent.VK_E) interactPhase();
        }

        private void pauseKey(int key) {
            if (key == KeyEvent.VK_ESCAPE) { screen = Screen.WORLD; return; }
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) menuIndex = (menuIndex + 4) % 5;
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) menuIndex = (menuIndex + 1) % 5;
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (menuIndex == 0) screen = Screen.WORLD; else if (menuIndex == 1) saveGame(); else if (menuIndex == 2) { settingsReturn = Screen.PAUSE; screen = Screen.SETTINGS; } else if (menuIndex == 3) screen = Screen.MAP; else { saveGame(); returnToMenu(); } }
        }

        private void returnToMenu() { screen = Screen.MENU; menuIndex = 0; choosingContinue = false; }

        private boolean inside(int x, int y, int bx, int by, int bw, int bh) { return new Rectangle(bx, by, bw, bh).contains(x, y); }

        private void updateMouseHover(int x, int y) {
            int cursor = java.awt.Cursor.DEFAULT_CURSOR;
            if (screen == Screen.MENU) { int index = menuAt(x, y); menuIndex = index; if (index >= 0) cursor = java.awt.Cursor.HAND_CURSOR; }
            else if (screen == Screen.SLOTS) { if (inside(x,y,250,250,360,170)) { slotIndex=0; cursor=java.awt.Cursor.HAND_CURSOR; } else if (inside(x,y,670,250,360,170)) { slotIndex=1; cursor=java.awt.Cursor.HAND_CURSOR; } }
            else if (screen == Screen.SAVE_OPTIONS) { saveOptionIndex = inside(x,y,300,315,680,72) ? 0 : inside(x,y,300,405,680,72) ? 1 : saveOptionIndex; if (inside(x,y,300,315,680,72)||inside(x,y,300,405,680,72)) cursor=java.awt.Cursor.HAND_CURSOR; }
            else if (screen == Screen.PHASE_SELECT) { for(int i=0;i<Phase.values().length;i++) if(inside(x,y,215,205+i*88,850,70)){phaseSelectIndex=i;cursor=java.awt.Cursor.HAND_CURSOR;break;} }
            else if (screen == Screen.SAVE_CONFIRM) { if(inside(x,y,370,410,230,64)){settingsIndex=0;cursor=java.awt.Cursor.HAND_CURSOR;}else if(inside(x,y,680,410,230,64)){settingsIndex=1;cursor=java.awt.Cursor.HAND_CURSOR;} }
            else if (screen == Screen.SETTINGS) { for(int i=0;i<3;i++)if(inside(x,y,330,245+i*70,620,58)){settingsIndex=i;cursor=java.awt.Cursor.HAND_CURSOR;break;} }
            else if (screen == Screen.PAUSE) { if(inside(x,y,400,240,480,310)){menuIndex=Math.max(0,Math.min(4,(y-260)/52));cursor=java.awt.Cursor.HAND_CURSOR;} }
            else if (screen == Screen.BATTLE) { if(inside(x,y,245,310,790,216)){battleChoice=Math.max(0,Math.min(3,(y-310)/54));cursor=java.awt.Cursor.HAND_CURSOR;} }
            else if (screen == Screen.DIALOGUE || screen == Screen.PHASE_INTRO || screen == Screen.PHASE_RESULT || screen == Screen.END || screen == Screen.EXTRAS || screen == Screen.CONTROLS) cursor=java.awt.Cursor.HAND_CURSOR;
            else if (screen == Screen.MAP) cursor=java.awt.Cursor.HAND_CURSOR;
            else if (screen == Screen.WORLD) { double worldX=x+camera; if(currentPhase==0&&traps.stream().anyMatch(t->!t.hit&&Math.abs(worldX-t.x)<42)||currentPhase==2&&(Math.abs(worldX-145)<38||Math.abs(worldX-1650)<38||fires.stream().anyMatch(f->!f.hit&&Math.abs(worldX-f.x)<42)))cursor=java.awt.Cursor.HAND_CURSOR; }
            setCursor(java.awt.Cursor.getPredefinedCursor(cursor)); repaint();
        }

        private void handleMouseClick(int x, int y) {
            switch (screen) {
                case MENU -> { int i=menuAt(x,y); if(i>=0){menuIndex=i;menuKey(KeyEvent.VK_ENTER);} }
                case SLOTS -> { int clickedSlot=inside(x,y,250,250,360,170)?0:inside(x,y,670,250,360,170)?1:-1;if(clickedSlot<0)return;slotIndex=clickedSlot;if(y>=365){if(hasSave(slotIndex)){confirmAction=ConfirmAction.DELETE_SAVE;confirmReturn=Screen.SLOTS;settingsIndex=0;screen=Screen.SAVE_CONFIRM;}}else slotsKey(KeyEvent.VK_ENTER); }
                case WORLD -> { double worldX=x+camera; boolean target=y>=510&&y<=610&&(currentPhase==0&&traps.stream().anyMatch(t->!t.hit&&Math.abs(worldX-t.x)<42)||currentPhase==2&&(Math.abs(worldX-145)<38||Math.abs(worldX-1650)<38||fires.stream().anyMatch(f->!f.hit&&Math.abs(worldX-f.x)<42)));if(target&&Math.abs(worldX-this.x)<110)interactPhase();else if(target)showToast("Aproxime-se do elemento para interagir com E ou clique."); }
                case SAVE_OPTIONS -> { if(inside(x,y,300,315,680,72)){saveOptionIndex=0;saveOptionsKey(KeyEvent.VK_ENTER);}else if(inside(x,y,300,405,680,72)){saveOptionIndex=1;saveOptionsKey(KeyEvent.VK_ENTER);}else if(inside(x,y,300,505,680,55))screen=Screen.SLOTS; }
                case PHASE_SELECT -> { for(int i=0;i<Phase.values().length;i++)if(inside(x,y,215,205+i*88,850,70)){phaseSelectIndex=i;selectSavedPhase(i);return;} if(inside(x,y,420,570,440,55))screen=Screen.SAVE_OPTIONS; }
                case SAVE_CONFIRM -> { if(inside(x,y,370,410,230,64)){settingsIndex=0;saveConfirmKey(KeyEvent.VK_ENTER);}else if(inside(x,y,680,410,230,64)){settingsIndex=1;saveConfirmKey(KeyEvent.VK_ENTER);} }
                case SETTINGS -> { for(int i=0;i<3;i++)if(inside(x,y,330,245+i*70,620,58)){settingsIndex=i;settingsKey(KeyEvent.VK_ENTER);break;} if(inside(x,y,440,525,400,50))screen=settingsReturn; }
                case CONTROLS -> screen=Screen.SETTINGS;
                case EXTRAS -> returnToMenu();
                case PAUSE -> { if(inside(x,y,400,240,480,310)){menuIndex=Math.max(0,Math.min(4,(y-260)/52));pauseKey(KeyEvent.VK_ENTER);} }
                case MAP -> { if(inside(x,y,400,520,480,65))screen=Screen.WORLD; }
                case DIALOGUE -> { if(inside(x,y,100,490,1080,165))advanceDialogue(); }
                case BATTLE -> { if(inside(x,y,245,310,790,216)){battleChoice=Math.max(0,Math.min(3,(y-310)/54));battleKey(KeyEvent.VK_ENTER);} }
                case PHASE_INTRO -> { if(inside(x,y,530,535,500,100))screen=Screen.WORLD; }
                case PHASE_RESULT -> { if(inside(x,y,500,530,500,80))screen=Screen.PHASE_INTRO; }
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
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { if (settingsIndex == 0) textScale = (textScale + 1) % 3; else if (settingsIndex == 1) contrast = !contrast; else screen = Screen.CONTROLS; }
        }

        private void dialogueKey(int key) { if (key == KeyEvent.VK_ESCAPE) screen = Screen.WORLD; else if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_E) advanceDialogue(); }
        private void advanceDialogue() { if (++dialogueIndex >= dialogue.length) { startQuizRound(); screen = Screen.BATTLE; } }

        private void startQuizRound() {
            List<Question> shuffled = new ArrayList<>(Arrays.asList(QUESTION_BANK[currentPhase]));
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
            completedPhase = currentPhase;
            if (currentPhase < Phase.values().length - 1) {
                unlockedPhase = Math.max(unlockedPhase, currentPhase + 1);
                currentPhase++;
                x = 110; y = 500; vy = 0; lives = 3; coinsCollected = 0;
                buildLevel();
                screen = Screen.PHASE_RESULT;
            } else {
                unlockedPhase = Phase.values().length - 1;
                campaignComplete = true;
                screen = Screen.END;
            }
            saveGame();
        }

        private void startGame() {
            if (name.trim().isEmpty()) name = "Viajante";
            currentPhase = 0; unlockedPhase = 0; completedPhase = -1; campaignComplete = false;
            x = 110; y = 500; vy = 0; lives = 3; coinsCollected = 0; won = false;
            buildLevel(); saveGame(); screen = Screen.PHASE_INTRO;
        }
        private void drawText(Graphics2D g, String text, int x, int y, int size, Color color) { g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(12, size + textScale * 2))); g.setColor(color); g.drawString(text, x, y); }
        private Color ink() { return contrast ? Color.WHITE : new Color(25, 48, 51); }
        private Color panel() { return contrast ? new Color(12, 12, 12) : new Color(249, 235, 187); }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (screen == Screen.SAVE_CONFIRM) { drawSaveConfirmationScreen(g); if (!toast.isEmpty()) { g.setColor(new Color(0, 0, 0, 190)); g.fillRoundRect(350, 645, 580, 42, 10, 10); drawText(g, toast, 380, 673, 16, Color.WHITE); } g.dispose(); return; }
            if (screen == Screen.MENU) drawMenu(g); else if (screen == Screen.SLOTS) drawSlots(g); else if (screen == Screen.SAVE_OPTIONS) { drawMenu(g); drawPopupDim(g); drawSaveOptions(g); } else if (screen == Screen.PHASE_SELECT) { drawMenu(g); drawPopupDim(g); drawPhaseSelect(g); } else if (screen == Screen.SAVE_CONFIRM) { if(confirmReturn==Screen.PHASE_SELECT){drawMenu(g);drawPopupDim(g);drawPhaseSelect(g);}else drawSlots(g); drawPopupDim(g); drawSaveConfirmation(g); } else if (screen == Screen.NAME) drawName(g); else if (screen == Screen.PHASE_INTRO) drawPhaseIntro(g); else if (screen == Screen.PHASE_RESULT) drawPhaseResult(g); else if (screen == Screen.SETTINGS) { drawSettingsUnderlay(g); drawPopupDim(g); drawSettings(g); } else if (screen == Screen.CONTROLS) { drawSettingsUnderlay(g); drawPopupDim(g); drawSettings(g); drawControls(g); } else if (screen == Screen.EXTRAS) { drawMenu(g); drawPopupDim(g); drawExtras(g); } else { drawWorld(g); if (screen == Screen.PAUSE) drawPause(g); else if (screen == Screen.MAP) drawMap(g); else if (screen == Screen.DIALOGUE) drawDialogue(g); else if (screen == Screen.BATTLE) drawBattle(g); else if (screen == Screen.END) drawEnd(g); }
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
                g.drawImage(button, drawX - (selectedWidth - width) / 2, drawY - (selectedHeight - height) / 2, selectedWidth, selectedHeight, this);
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
        private void drawSlots(Graphics2D g) { drawBackdrop(g); drawText(g, "ESCOLHA UM SLOT", 440, 150, 36, Color.WHITE); for (int i = 0; i < 2; i++) { int xx = 250 + i * 420; g.setColor(i == slotIndex ? new Color(217, 92, 70) : new Color(24, 58, 50)); g.fillRoundRect(xx, 250, 360, 170, 12, 12); drawText(g, "SLOT " + (i + 1), xx + 34, 300, 25, Color.WHITE); drawText(g, hasSave(i) ? saveSummary(i) : "Vazio - Enter para criar", xx + 34, 345, 16, new Color(245, 230, 190)); drawText(g, "Delete apaga", xx + 34, 385, 14, new Color(196, 222, 188)); } drawText(g, "A/D escolhem   Enter abre   Esc volta", 430, 625, 15, Color.WHITE); }
        private void drawName(Graphics2D g) { drawBackdrop(g); drawText(g, "COMO TE CHAMAM?", 405, 200, 34, Color.WHITE); g.setColor(panel()); g.fillRoundRect(330, 270, 620, 90, 10, 10); drawText(g, name + (System.currentTimeMillis() / 400 % 2 == 0 ? "_" : ""), 380, 330, 28, ink()); drawText(g, nameConfirm ? "Enter para confirmar" : "Digite seu apelido e pressione Enter", 420, 430, 17, Color.WHITE); }
        private void drawPhaseIntro(Graphics2D g) {
            drawBackdrop(g);
            Phase phase = Phase.values()[currentPhase];
            g.setColor(new Color(7, 26, 28, 220)); g.fillRoundRect(100, 72, 1080, 580, 14, 14);
            BufferedImage portrait = guardianImages[currentPhase];
            if (portrait != null) drawImageFit(g, portrait, 145, 175, 390, 405);
            drawText(g, phase.title, 565, 160, 27, new Color(255, 216, 92));
            drawText(g, phase.guardian + " - " + phase.theme, 565, 210, 23, Color.WHITE);
            drawParagraph(g, phase.legend, 565, 275, 530, 26, new Color(220, 237, 213), 20);
            drawParagraph(g, phaseObjectiveText(currentPhase), 565, 410, 530, 27, new Color(255, 231, 156), 19);
            drawText(g, "Enter comeca a fase", 565, 585, 17, Color.WHITE);
        }

        private void drawPhaseResult(Graphics2D g) {
            drawBackdrop(g);
            Phase done = Phase.values()[completedPhase];
            Phase next = Phase.values()[currentPhase];
            g.setColor(new Color(7, 26, 28, 225)); g.fillRoundRect(115, 85, 1050, 550, 14, 14);
            BufferedImage portrait = guardianImages[completedPhase];
            if (portrait != null) drawImageFit(g, portrait, 155, 190, 360, 360);
            drawText(g, "FASE CONCLUIDA", 565, 170, 22, new Color(255, 216, 92));
            drawText(g, done.title, 565, 215, 26, Color.WHITE);
            drawParagraph(g, done.legend, 565, 275, 520, 25, new Color(220, 237, 213), 19);
            drawText(g, "Nova lenda desbloqueada", 565, 435, 19, new Color(255, 216, 92));
            drawText(g, next.title, 565, 475, 23, Color.WHITE);
            drawText(g, "Enter para continuar", 565, 570, 17, Color.WHITE);
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
            if (settingsReturn == Screen.PAUSE) { drawWorld(g); drawPause(g); }
            else drawMenu(g);
        }
        private void drawPopupDim(Graphics2D g) { g.setColor(new Color(0, 0, 0, 145)); g.fillRect(0, 0, W, H); }
        private void drawSaveConfirmationScreen(Graphics2D g) { if(confirmReturn==Screen.PHASE_SELECT){drawMenu(g);drawPopupDim(g);drawPhaseSelect(g);}else{drawSlots(g);drawPopupDim(g);}drawSaveConfirmation(g); }
        private void drawSettings(Graphics2D g) {
            g.setColor(new Color(18, 43, 38, 225)); g.fillRoundRect(270, 135, 740, 455, 16, 16);
            g.setColor(new Color(213, 179, 105)); g.drawRoundRect(270, 135, 740, 455, 16, 16);
            drawText(g, "CONFIGURACOES", 470, 195, 33, new Color(255, 216, 92));
            String[] options = { "Tamanho do texto: " + (textScale == 0 ? "Compacto" : textScale == 1 ? "Padrao" : "Ampliado"), "Alto contraste: " + (contrast ? "Ligado" : "Desligado"), "Controles" };
            for (int i = 0; i < 3; i++) { int y = 280 + i * 70; g.setColor(i == settingsIndex ? new Color(184, 91, 57) : new Color(31, 69, 56)); g.fillRoundRect(330, y - 29, 620, 52, 8, 8); drawText(g, options[i], 375, y + 5, 20, Color.WHITE); }
            drawText(g, "Esc fecha", 565, 545, 16, new Color(231, 224, 194));
        }
        private void drawControls(Graphics2D g) { g.setColor(new Color(12, 30, 28, 225)); g.fillRoundRect(315, 205, 650, 300, 14, 14); drawText(g, "CONTROLES", 505, 260, 29, new Color(255,216,92)); drawText(g, "A/D ou SETAS: correr", 390, 325, 21, Color.WHITE); drawText(g, "W, SETA CIMA ou ESPACO: pular", 390, 370, 21, Color.WHITE); drawText(g, "M: mapa   F5: salvar   Esc: pausar", 390, 415, 20, Color.WHITE); drawText(g, "Clique ou pressione Esc para voltar", 430, 470, 16, new Color(245,230,190)); }
        private void drawExtras(Graphics2D g) { g.setColor(new Color(18, 43, 38, 238)); g.fillRoundRect(270, 190, 740, 350, 16, 16); g.setColor(new Color(213,179,105));g.drawRoundRect(270,190,740,350,16,16);drawText(g, "AJUDA E INFORMACOES", 420, 250, 30, new Color(255,216,92));drawParagraph(g, "Uma aventura inspirada nas lendas da floresta brasileira. Proteja a mata, respeite os animais e siga as orientacoes de cada guardiao.", 350, 320, 580, 30, Color.WHITE, 20);drawText(g, "Clique fora ou pressione Esc para voltar", 410, 480, 16, new Color(245,230,190)); }

        private void drawSaveOptions(Graphics2D g) { g.setColor(new Color(18,43,38,220));g.fillRoundRect(250,225,780,340,16,16);drawText(g,"CONTINUAR CAMPANHA",440,280,29,new Color(255,216,92));String[] labels={"Continuar de onde parou","Escolher fase desbloqueada"};for(int i=0;i<2;i++){int y=315+i*90;g.setColor(i==saveOptionIndex?new Color(184,91,57):new Color(31,69,56));g.fillRoundRect(300,y,680,72,10,10);drawText(g,labels[i],350,y+46,21,Color.WHITE);}drawText(g,"Esc volta aos saves",515,525,16,Color.WHITE);}
        private void drawPhaseSelect(Graphics2D g) { g.setColor(new Color(18,43,38,220));g.fillRoundRect(190,105,900,520,16,16);drawText(g,"SELECIONAR FASE",480,155,29,new Color(255,216,92));for(int i=0;i<Phase.values().length;i++){int y=205+i*88;boolean available=i<=unlockedPhase;g.setColor(i==phaseSelectIndex?new Color(184,91,57):new Color(31,69,56));if(!available)g.setColor(new Color(55,65,58));g.fillRoundRect(215,y,850,70,10,10);drawText(g,Phase.values()[i].title,250,y+31,17,available?Color.WHITE:new Color(175,175,160));drawText(g,available?"DESBLOQUEADA":"BLOQUEADA",805,y+53,13,new Color(255,222,153));}drawText(g,"Voltar",590,604,17,Color.WHITE);}
        private void drawSaveConfirmation(Graphics2D g) { g.setColor(new Color(18,43,38,225));g.fillRoundRect(275,225,730,285,16,16);String title=confirmAction==ConfirmAction.DELETE_SAVE?"APAGAR ESTE SAVE?":confirmAction==ConfirmAction.RESTART_PHASE?"REINICIAR NESTA FASE?":"COMECAR NOVA CAMPANHA?";drawText(g,title,420,295,27,new Color(255,216,92));String message=confirmAction==ConfirmAction.DELETE_SAVE?"O progresso deste slot sera removido.":confirmAction==ConfirmAction.RESTART_PHASE?"O progresso atual sera substituido pelo inicio da fase escolhida.":"O progresso salvo neste slot sera substituido.";drawParagraph(g,message,390,350,500,28,Color.WHITE,19);String[] labels=confirmAction==ConfirmAction.DELETE_SAVE?new String[]{"Apagar","Cancelar"}:confirmAction==ConfirmAction.RESTART_PHASE?new String[]{"Reiniciar","Cancelar"}:new String[]{"Continuar","Cancelar"};for(int i=0;i<2;i++){int x=370+i*310;g.setColor(settingsIndex==i?new Color(184,91,57):new Color(31,69,56));g.fillRoundRect(x,410,230,64,10,10);drawText(g,labels[i],x+68,450,18,Color.WHITE);}}

        private void drawWorld(Graphics2D g) {
            g.setColor(new Color(75, 151, 83)); g.fillRect(0, 0, W, H);
            g.translate(-camera, 0);
            if (worldBackground != null) g.drawImage(worldBackground, 0, 78, this);
            else { g.setColor(new Color(47, 95, 57)); g.fillRect(0, 78, WORLD_W, GROUND_Y - 78); }
            for (TreeDecoration tree : trees) g.drawImage(tree.image(), tree.x(), tree.y(), this);
            for (Rectangle platform : platforms) {
                if (platform.y == GROUND_Y) drawGround(g, platform);
                else drawPlatform(g, platform);
            }
            g.setColor(PHASE_FILTERS[currentPhase]);
            g.fillRect((int) camera, 78, W, H - 78);
            if (currentPhase == 2) drawFireStations(g);
            if (currentPhase == 0) for (Thing trap : traps) if (!trap.hit) drawTrap(g, trap.x, trap.y);
            for (Thing fire : fires) if (!fire.hit) drawFire(g, fire.x, fire.y);
            for (Thing coin : coins) if (!coin.hit) drawObjective(g, coin);
            for (Thing enemy : enemies) if (!enemy.hit) { g.setColor(new Color(106, 55, 42)); g.fillOval(enemy.x - 20, 552, 40, 48); g.setColor(new Color(245, 177, 130)); g.fillOval(enemy.x - 13, 558, 26, 22); g.setColor(Color.WHITE); g.fillOval(enemy.x - 9, 564, 6, 6); g.fillOval(enemy.x + 3, 564, 6, 6); }
            drawPlayer(g); g.translate(camera, 0);
            g.setColor(new Color(25, 48, 51, 220)); g.fillRect(0, 0, W, 78);
            drawText(g, Phase.values()[currentPhase].title, 24, 27, 15, new Color(255, 226, 125));
            String objective = switch (currentPhase) {
                case 0 -> "Sementes: " + coinsCollected + "/12   Armadilhas: " + traps.stream().filter(t -> t.hit).count() + "/3";
                case 1 -> "Travessia: " + Math.min(100, (int)(x * 100 / 3020)) + "%";
                case 2 -> "Focos: " + fires.stream().filter(f -> f.hit).count() + "/" + fires.size() + "   Balde: " + (bucketFull ? "cheio" : "vazio");
                default -> "Residuos: " + coinsCollected + "/10";
            };
            drawText(g, objective + "   VIDAS " + "*".repeat(lives), 24, 58, 17, Color.WHITE);
            String controls = currentPhase == 0 || currentPhase == 2
                ? "E interage   A/D correr   Espaco pular   M mapa   Esc pausa"
                : "A/D correr   Espaco pular   M mapa   F5 salvar   Esc pausa";
            drawText(g, controls, 690, 58, 15, Color.WHITE);
        }

        private void drawGround(Graphics2D g, Rectangle collision) {
            if (groundTile == null) {
                g.setColor(new Color(91, 125, 54)); g.fillRect(collision.x, collision.y, collision.width, collision.height);
                return;
            }
            Shape previousClip = g.getClip();
            g.clipRect(collision.x, collision.y - GROUND_OVERDRAW, collision.width, collision.height + GROUND_OVERDRAW);
            int tileWidth = groundTile.getWidth();
            int firstTile = Math.floorDiv(collision.x, tileWidth) * tileWidth;
            for (int tileX = firstTile; tileX < collision.x + collision.width; tileX += tileWidth)
                g.drawImage(groundTile, tileX, collision.y - GROUND_OVERDRAW, this);
            g.setClip(previousClip);
        }

        private void drawPlatform(Graphics2D g, Rectangle collision) {
            BufferedImage artwork = platformArtwork.get(collision.width);
            if (artwork != null) g.drawImage(artwork, collision.x, collision.y, this);
            else {
                g.setColor(new Color(115, 72, 48)); g.fillRect(collision.x, collision.y, collision.width, collision.height);
                g.setColor(new Color(70, 170, 78)); g.fillRect(collision.x, collision.y, collision.width, 9);
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
        private void drawPlayer(Graphics2D g) { int px = (int)x, py = (int)y; Image image = walking ? ((walkingFrame / 8) % 2 == 0 ? playerWalk1 : playerWalk2) : playerImage; if (image != null) { if (facingRight) g.drawImage(image, px - 8, py - 20, 58, 78, this); else g.drawImage(image, px + 50, py - 20, px - 8, py + 58, 0, 0, image.getWidth(this), image.getHeight(this), this); } else { g.setColor(new Color(217, 92, 70)); g.fillOval(px, py, 38, 38); g.setColor(new Color(242, 177, 139)); g.fillOval(px + 5, py - 17, 28, 28); } }
        private void drawPause(Graphics2D g) { g.setColor(new Color(11, 28, 27, 225)); g.fillRect(310, 130, 660, 470); drawText(g, "PAUSA", 570, 205, 42, new Color(255, 216, 92)); String[] options = { "Continuar", "Salvar", "Configuracoes", "Mapa", "Sair para o menu" }; for (int i = 0; i < options.length; i++) drawText(g, (i == menuIndex ? "> " : "  ") + options[i], 495, 290 + i * 52, 22, Color.WHITE); }
        private void drawMap(Graphics2D g) {
            g.setColor(new Color(13, 39, 38, 210)); g.fillRoundRect(70, 95, 1140, 530, 12, 12);
            drawText(g, "JORNADA DOS GUARDIOES", 415, 160, 32, new Color(255, 216, 92));
            for (int i = 0; i < Phase.values().length; i++) {
                int cardX = 105 + i * 275;
                boolean complete = i < currentPhase || campaignComplete;
                boolean available = i <= unlockedPhase;
                g.setColor(complete ? new Color(71, 139, 82) : available ? new Color(187, 125, 52) : new Color(54, 70, 67));
                g.fillRoundRect(cardX, 280, 240, 145, 12, 12);
                BufferedImage portrait = guardianImages[i];
                if (portrait != null) drawImageFit(g, portrait, cardX + 78, 188, 84, 82);
                drawText(g, "FASE " + (i + 1), cardX + 78, 318, 15, Color.WHITE);
                drawText(g, Phase.values()[i].guardian, cardX + 20, 357, i == 3 ? 17 : 20, Color.WHITE);
                drawText(g, complete ? "CONCLUIDA" : available ? "DESBLOQUEADA" : "BLOQUEADA", cardX + 30, 397, 13, new Color(255, 230, 160));
                if (i < Phase.values().length - 1) { g.setColor(new Color(255, 216, 92)); g.fillRect(cardX + 240, 347, 35, 4); }
            }
            drawText(g, "M ou Esc para voltar", 520, 555, 17, Color.WHITE);
        }
        private void drawDialogue(Graphics2D g) { g.setColor(new Color(8, 22, 23, 220)); g.fillRoundRect(100, 500, 1080, 150, 12, 12); BufferedImage portrait = guardianImages[currentPhase]; if (portrait != null) drawImageFit(g, portrait, 130, 375, 155, 175); drawText(g, Phase.values()[currentPhase].guardian, 310, 535, 22, new Color(255, 216, 92)); drawParagraph(g, dialogue[dialogueIndex], 310, 575, 805, 24, Color.WHITE, 18); drawText(g, "Enter/E/Space para continuar", 850, 630, 14, new Color(196, 222, 188)); }
        private void drawBattle(Graphics2D g) {
            Question question = quizQuestions.get(quizIndex);
            g.setColor(new Color(16, 37, 38, 215)); g.fillRoundRect(170, 100, 940, 520, 12, 12);
            drawText(g, "DESAFIO - " + Phase.values()[currentPhase].guardian.toUpperCase(), 350, 155, 28, new Color(255, 216, 92));
            drawText(g, "Pergunta " + (quizIndex + 1) + "/" + quizQuestions.size(), 965, 155, 17, new Color(255, 230, 160));
            drawParagraph(g, question.prompt(), 245, 220, 790, 28, Color.WHITE, 21);
            for (int i = 0; i < question.options().length; i++) drawText(g, (battleChoice == i ? "> " : "  ") + (i + 1) + " - " + question.options()[i], 280, 342 + i * 54, 17, battleChoice == i ? new Color(255, 216, 92) : Color.WHITE);
            drawText(g, "Setas ou 1-4 selecionam   Enter confirma", 430, 565, 16, new Color(196, 222, 188));
        }
        private void drawEnd(Graphics2D g) { drawBackdrop(g); BufferedImage portrait = guardianImages[Phase.NEGRINHO.ordinal()]; if (portrait != null) drawImageFit(g, portrait, 170, 220, 270, 330); drawText(g, "CAMPANHA CONCLUIDA", 500, 230, 38, new Color(255, 216, 92)); drawParagraph(g, "Voce acompanhou Curupira, Saci, Caipora e Negrinho do Pastoreio e ajudou a proteger a floresta.", 500, 300, 570, 30, Color.WHITE, 22); drawText(g, "Enter volta ao menu", 500, 520, 18, new Color(245, 230, 190)); }

        private int firstSave() { return hasSave(0) ? 0 : hasSave(1) ? 1 : -1; }
        private File saveFile(int slot) { return new File(System.getProperty("user.home"), ".entremitoseraizes/saves/slot-" + (slot + 1) + ".properties"); }
        private boolean hasSave(int slot) { Properties save=readSave(slot); try { return saveFile(slot).isFile()&&save.getProperty("name")!=null&&Integer.parseInt(save.getProperty("phase"))>=0; } catch (RuntimeException exception) { return false; } }
        private String saveSummary(int slot) { Properties p = readSave(slot); int phase = Math.max(0, Math.min(Phase.values().length - 1, intValue(p, "phase", 0))); return p.getProperty("name", "Viajante") + " - " + Phase.values()[phase].title; }
        private Properties readSave(int slot) { Properties p = new Properties(); if (!saveFile(slot).isFile()) return p; try (FileInputStream in = new FileInputStream(saveFile(slot))) { p.load(in); } catch (IOException ignored) { } return p; }
        private void saveGame() {
            File dir = saveFile(slotIndex).getParentFile();
            if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) { showToast("Nao foi possivel criar a pasta de saves."); return; }
            Properties p = new Properties();
            p.setProperty("name", name); p.setProperty("coins", String.valueOf(coinsCollected));
            p.setProperty("lives", String.valueOf(lives)); p.setProperty("x", String.valueOf(x)); p.setProperty("y", String.valueOf(y));
            p.setProperty("phase", String.valueOf(currentPhase)); p.setProperty("unlockedPhase", String.valueOf(unlockedPhase));
            p.setProperty("coinMask", Long.toHexString(mask(coins))); p.setProperty("fireMask", Long.toHexString(mask(fires))); p.setProperty("trapMask", Long.toHexString(mask(traps)));
            p.setProperty("bucketFull", String.valueOf(bucketFull)); p.setProperty("campaignComplete", String.valueOf(campaignComplete));
            try (FileOutputStream out = new FileOutputStream(saveFile(slotIndex))) {
                p.store(out, "Entre Mitos e Raizes - campanha");
            } catch (IOException exception) { showToast("Falha ao salvar: " + exception.getMessage()); }
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
            name = p.getProperty("name", "Viajante");
            currentPhase = Math.max(0, Math.min(Phase.values().length - 1, intValue(p, "phase", 0)));
            unlockedPhase = Math.max(currentPhase, Math.min(Phase.values().length - 1, intValue(p, "unlockedPhase", currentPhase)));
            campaignComplete = Boolean.parseBoolean(p.getProperty("campaignComplete", "false"));
            lives = Math.max(1, Math.min(3, intValue(p, "lives", 3)));
            x = clamp(doubleValue(p, "x", 110), 0, WORLD_W - 42); y = clamp(doubleValue(p, "y", 500), 100, 650);
            bucketFull = Boolean.parseBoolean(p.getProperty("bucketFull", "false"));
            int savedCoins = intValue(p, "coins", 0);
            buildLevel();
            restoreMask(coins, longValue(p, "coinMask", 0), savedCoins);
            restoreMask(fires, longValue(p, "fireMask", 0), 0);
            restoreMask(traps, longValue(p, "trapMask", 0), 0);
            coinsCollected = (int) coins.stream().filter(item -> item.hit).count();
            camera = clamp(x - 340, 0, WORLD_W - W);
            screen = campaignComplete ? Screen.END : Screen.PHASE_INTRO;
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
        private void deleteSave(int slot) { if (hasSave(slot)) { if (saveFile(slot).delete()) showToast("Slot apagado."); else showToast("Nao foi possivel apagar este slot."); } screen = Screen.SLOTS; }
        private void showToast(String message) { toast = message; toastUntil = System.currentTimeMillis() + 2200; }
        private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    }

    private static final class Thing {
        int x, y;
        boolean hit;
        Thing(int x, int y) { this.x = x; this.y = y; }
    }

    private record TreeDecoration(int x, int y, BufferedImage image) { }
}
