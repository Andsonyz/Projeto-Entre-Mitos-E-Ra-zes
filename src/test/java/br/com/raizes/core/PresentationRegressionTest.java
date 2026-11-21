package br.com.raizes.core;

import br.com.raizes.assets.AssetManager;
import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.audio.AmbientAudio;
import br.com.raizes.model.Phase;
import br.com.raizes.persistence.SaveManager;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javafx.application.Platform;
import static br.com.raizes.config.GameConfig.*;

/** Real assets, camera limits, right-hand menu, responsive loading and stale callbacks. */
public final class PresentationRegressionTest {
    private static final Path ARTIFACTS = Path.of(System.getProperty("raizes.artifacts", "out/project-tests/artifacts/presentation"));

    public static void main(String[] args) {
        int result = 0;
        try { verify(); System.out.println("OK: fundos das quatro fases, menu à direita, carregamento, cancelamento e recuperação."); }
        catch (Throwable error) { result = 1; error.printStackTrace(); }
        finally { Platform.exit(); }
        System.exit(result);
    }

    private static void verify() throws Exception {
        Files.createDirectories(ARTIFACTS);
        AtomicInteger previous = new AtomicInteger(-1);
        GameAssets assets = GameAssets.load(value -> {
            check(!SwingUtilities.isEventDispatchThread(), "Decodificação na EDT.");
            check(value >= previous.getAndSet(value), "Progresso regrediu.");
        });
        check(previous.get() == 100, "Carregamento não terminou.");
        String[] expected = { "BackgroundHunter.png", "BackgroundParkour.png", "BackGroundFire.png", "BackGroundLixo.png" };
        Path saves = Files.createTempDirectory("raizes-presentation-");
        Game game = edt(() -> {
            Game created = new Game(new CinematicCatalog((java.io.File) null), new SaveManager(saves), assets);
            created.timer.stop(); created.setSize(W, H); return created;
        });
        try {
            edt(() -> {
                for (GameState state : GameState.values()) {
                    game.screen = state;
                    AmbientAudio.Track expectedTrack = switch (state) {
                        case CINEMATIC -> AmbientAudio.Track.SILENCE;
                        case WORLD, PHASE_INTRO, TRAP_CHALLENGE, PAUSE, MAP, DIALOGUE, BATTLE, END -> AmbientAudio.Track.FOREST;
                        default -> AmbientAudio.Track.MENU;
                    };
                    check(game.ambientTrack() == expectedTrack, "Faixa incorreta na tela " + state);
                }
                var settingsReturn = Game.class.getDeclaredField("settingsReturn"); settingsReturn.setAccessible(true);
                settingsReturn.set(game,GameState.PAUSE);
                game.screen=GameState.SETTINGS; check(game.ambientTrack()==AmbientAudio.Track.FOREST,"Configuração perdeu música da fase.");
                game.screen=GameState.CONTROLS; check(game.ambientTrack()==AmbientAudio.Track.FOREST,"Controles perderam música da fase.");
                settingsReturn.set(game,GameState.MENU); game.screen=GameState.MENU;
                screenshot(game, "menu.png");
                for (int i = 0; i < game.menuBounds.length; i++) {
                    Rectangle box = game.menuBounds[i];
                    check(box != null && (i == 3 ? box.x < 200 : box.x >= 940) && box.x + box.width < W && box.y + box.height < H,
                        "Opção fora da lateral esperada: " + i + " " + box);
                    for (int j = 0; j < i; j++) check(!box.intersects(game.menuBounds[j]), "Opções sobrepostas.");
                }
                for (int[] size : new int[][] { {1280,720}, {1536,864}, {1600,1000} }) {
                    game.screen = GameState.MENU; game.setSize(size[0], size[1]);
                    Rectangle box = game.menuBounds[0];
                    double scale = Math.min(size[0]/(double)W, size[1]/(double)H);
                    int x = (int)((size[0]-W*scale)/2 + box.getCenterX()*scale);
                    int y = (int)((size[1]-H*scale)/2 + box.getCenterY()*scale);
                    MouseEvent click = new MouseEvent(game, MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1);
                    for (var listener : game.getMouseListeners()) listener.mouseClicked(click);
                    check(game.screen == GameState.SLOTS, "Clique no menu falhou em " + size[0]);
                }
                game.setSize(W,H);
                var build = Game.class.getDeclaredMethod("buildLevel"); build.setAccessible(true);
                for (int phase = 0; phase < Phase.values().length; phase++) {
                    check(Phase.values()[phase].background.endsWith("/" + expected[phase]), "Mapeamento de fase incorreto.");
                    BufferedImage source = AssetManager.image(Phase.values()[phase].background);
                    game.currentPhase = phase; build.invoke(game);
                    BufferedImage actual = game.worldBackground;
                    check(actual == assets.backgrounds[phase], "Fundo não reutilizado.");
                    check(actual.getWidth() >= WORLD_W && actual.getHeight() >= H-78, "Fundo não cobre o mundo.");
                    check(Math.abs(actual.getWidth() - actual.getHeight()*source.getWidth()/(double)source.getHeight()) <= 1.1,
                        "Fundo distorcido.");
                    BufferedImage expectedImage = GameAssets.resizeImage(source, actual.getWidth(), actual.getHeight());
                    for (int y=0;y<actual.getHeight();y+=23) for(int x=0;x<actual.getWidth();x+=29)
                        check(actual.getRGB(x,y)==expectedImage.getRGB(x,y), "Imagem errada na fase " + phase);
                    game.screen = GameState.WORLD;
                    game.camera = 0; screenshot(game, "phase-"+(phase+1)+"-start.png");
                    game.camera = WORLD_W-W; screenshot(game, "phase-"+(phase+1)+"-end.png");
                    build.invoke(game); check(game.worldBackground == actual, "Fundo recarregado ao reiniciar.");
                }
                return null;
            });
        } finally { edt(() -> { game.cinematicManager.dispose(); game.ambientAudio.close(); return null; }); Files.deleteIfExists(saves); }
        loading(assets);
    }

    private static void loading(GameAssets assets) throws Exception {
        BufferedImage artwork = AssetManager.image(GameAssets.LOADING_IMAGE);
        check(artwork != null, "Arte de carregamento ausente.");
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), ready = new CountDownLatch(1);
        AtomicInteger callbacks = new AtomicInteger();
        LoadingScreen panel = edt(() -> {
            LoadingScreen screen = new LoadingScreen(artwork, ignored -> { callbacks.incrementAndGet(); ready.countDown(); }, progress -> {
                check(!SwingUtilities.isEventDispatchThread(), "Carregador executou na interface.");
                entered.countDown(); check(release.await(10, TimeUnit.SECONDS), "Teste não liberou o carregador."); return assets;
            });
            screen.setSize(W,H); screen.addNotify(); screen.start(); return screen;
        });
        check(entered.await(5,TimeUnit.SECONDS), "Worker não iniciou.");
        edt(() -> { check(callbacks.get()==0,"Menu abriu antes da preparação."); screenshot(panel,"loading.png"); return null; });
        release.countDown(); check(ready.await(5,TimeUnit.SECONDS),"Menu não abriu após carregar.");
        edt(() -> { panel.removeNotify(); check(callbacks.get()==1,"Conclusão duplicada."); return null; });

        CountDownLatch cancelledEntered = new CountDownLatch(1), cancelRelease = new CountDownLatch(1), cancelledFinished = new CountDownLatch(1);
        LoadingScreen cancelled = edt(() -> {
            LoadingScreen screen = new LoadingScreen(artwork, ignored -> callbacks.incrementAndGet(), progress -> {
                cancelledEntered.countDown();
                try { cancelRelease.await(5,TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                cancelledFinished.countDown(); return assets;
            });
            screen.addNotify(); screen.start(); return screen;
        });
        check(cancelledEntered.await(5,TimeUnit.SECONDS),"Worker de cancelamento não iniciou.");
        edt(() -> { cancelled.removeNotify(); return null; }); cancelRelease.countDown();
        check(cancelledFinished.await(5,TimeUnit.SECONDS),"Cancelamento não encerrou worker.");
        edt(() -> { check(callbacks.get()==1,"Callback antigo abriu menu."); return null; });

        AtomicInteger attempts = new AtomicInteger(); CountDownLatch recovered = new CountDownLatch(1);
        LoadingScreen retry = edt(() -> {
            LoadingScreen screen = new LoadingScreen(artwork, ignored -> recovered.countDown(), progress -> {
                if (attempts.getAndIncrement()==0) throw new IllegalStateException("Falha intencional de teste"); return assets;
            });
            screen.setSize(W,H); screen.addNotify(); screen.start(); return screen;
        });
        long deadline = System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while (!edt(() -> retry.getComponent(0).isVisible()) && System.nanoTime()<deadline)
            java.util.concurrent.locks.LockSupport.parkNanos(2_000_000);
        edt(() -> {
            check(retry.getComponent(0).isVisible(),"Falha não ofereceu recuperação.");
            ((javax.swing.JButton)retry.getComponent(0)).doClick(); return null;
        });
        check(recovered.await(5,TimeUnit.SECONDS),"Retry não recuperou carregamento.");
        edt(() -> { retry.removeNotify(); return null; });
    }

    private static void screenshot(javax.swing.JComponent component, String name) throws Exception {
        BufferedImage output = new BufferedImage(component.getWidth(),component.getHeight(),BufferedImage.TYPE_INT_ARGB);
        var graphics=output.createGraphics(); component.paint(graphics); graphics.dispose();
        ImageIO.write(output,"png",ARTIFACTS.resolve(name).toFile());
    }
    private static <T> T edt(java.util.concurrent.Callable<T> action) throws Exception {
        FutureTask<T> task=new FutureTask<>(action); SwingUtilities.invokeLater(task); return task.get(15,TimeUnit.SECONDS);
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
