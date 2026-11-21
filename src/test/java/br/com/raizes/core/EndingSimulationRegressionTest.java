package br.com.raizes.core;

import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.model.GameCredits;
import br.com.raizes.persistence.SaveManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Entire campaign via visible simulation controls, with byte-for-byte save isolation. */
public final class EndingSimulationRegressionTest {
    public static void main(String[] args) {
        int result = 0;
        try { verify(); System.out.println("OK: simulação completa, quatro guardiões, créditos, menu e saves preservados."); }
        catch (Throwable error) { result = 1; error.printStackTrace(); }
        finally { Platform.exit(); }
        System.exit(result);
    }

    private static void verify() throws Exception {
        Path directory = Files.createTempDirectory("raizes-ending-");
        SaveManager saves = new SaveManager(directory);
        Properties original = new Properties();
        original.setProperty("name", "Campanha real"); original.setProperty("phase", "3");
        original.setProperty("campaignComplete", "true"); original.setProperty("celebration", "true");
        check(saves.write(0, original) && saves.write(1, original), "Fixtures de saves.");
        byte[] first = Files.readAllBytes(directory.resolve("slot-1.properties"));
        byte[] second = Files.readAllBytes(directory.resolve("slot-2.properties"));
        GameAssets assets = GameAssets.load(ignored -> { });
        Game game = edt(() -> {
            Game created = new Game(new CinematicCatalog((java.io.File)null), saves, assets);
            created.timer.stop(); created.setSize(1280,720); return created;
        });
        try {
            edt(() -> {
                check(!game.advanceSimulation(), "Atalho de simulação ativo na campanha normal.");
                render(game, "menu");
                var help = game.menuBounds[3]; click(game,(int)help.getCenterX(),(int)help.getCenterY());
                check(game.screen==GameState.EXTRAS,"Ajuda inacessível.");
                render(game,"ajuda"); click(game,640,465);
                check(game.simulationMode,"Simulação por mouse não abriu.");
                for (int phase=0;phase<4;phase++) {
                    check(game.currentPhase==phase && game.screen==GameState.PHASE_INTRO,"Apresentação da fase "+phase);
                    if (phase==0) verifyIntroContent(game);
                    step(game); check(game.screen==GameState.WORLD,"Início da fase.");
                    step(game); check(game.screen==GameState.DIALOGUE,"Objetivos não chegaram ao guardião.");
                    check(game.coinsCollected==game.coins.size() && game.traps.stream().allMatch(t->t.hit)
                        && game.fires.stream().allMatch(f->f.hit),"Objetivos incompletos.");
                    step(game); check(game.screen==GameState.BATTLE,"Quiz ignorado no fluxo.");
                    step(game); check(game.screen==GameState.DIALOGUE,"Cinco acertos sem agradecimento.");
                    render(game,"agradecimento-"+(phase+1)); step(game);
                }
                check(game.celebrationActive && game.screen==GameState.WORLD,"Celebração ausente.");
                for (int guardian=0;guardian<4;guardian++) {
                    step(game); check(game.screen==GameState.DIALOGUE,"Conversa final ausente.");
                    render(game,"guardiao-"+guardian); step(game);
                    check(game.screen==GameState.WORLD,"Não saiu da conversa final.");
                }
                step(game); check(game.screen==GameState.END,"Não chegou aos créditos.");
                check(GameCredits.CREATORS.equals(java.util.List.of("Antonio Andson de Oliveira Rocha",
                    "Sophia Hellen Pires da Silveira")),"Autoria incorreta.");
                render(game,"creditos"); click(game,640,565);
                check(game.screen==GameState.MENU && !game.simulationMode,"Retorno ao menu.");
                return null;
            });
            check(Arrays.equals(first,Files.readAllBytes(directory.resolve("slot-1.properties")))
                && Arrays.equals(second,Files.readAllBytes(directory.resolve("slot-2.properties"))),"Simulação modificou saves reais.");
            edt(() -> {
                // Continue a genuine completed save, end via mouse, then repeat via keyboard.
                var continueButton=game.menuBounds[1]; click(game,(int)continueButton.getCenterX(),(int)continueButton.getCenterY());
                press(game,KeyEvent.VK_ENTER); press(game,KeyEvent.VK_ENTER);
                check(game.celebrationActive && !game.simulationMode,"Save real não abriu na celebração.");
                click(game,1150,37); check(game.screen==GameState.END,"Encerramento por mouse.");
                press(game,KeyEvent.VK_ESCAPE); check(game.screen==GameState.MENU,"Esc dos créditos.");
                click(game,(int)continueButton.getCenterX(),(int)continueButton.getCenterY());
                press(game,KeyEvent.VK_ENTER); press(game,KeyEvent.VK_ENTER);
                press(game,KeyEvent.VK_ENTER); check(game.screen==GameState.END,"Encerramento por teclado.");
                press(game,KeyEvent.VK_ENTER); check(game.screen==GameState.MENU,"Enter dos créditos.");
                return null;
            });
            check(Boolean.parseBoolean(saves.read(0).getProperty("campaignComplete")),"Conclusão perdida.");
        } finally { edt(() -> {game.close(); return null;}); }
        Path catalog = directory.resolve("cancel-cinematics.properties");
        Files.writeString(catalog,"chapter.1.1.video=Videos/simulation-cancel-missing.mp4\n");
        Game cancellation = edt(() -> {
            Game created = new Game(new CinematicCatalog(catalog.toFile()),saves,assets);
            created.timer.stop(); return created;
        });
        byte[] beforeCancellation=Files.readAllBytes(directory.resolve("slot-1.properties"));
        try {
            edt(() -> {
                cancellation.startSimulation();
                check(cancellation.screen==GameState.CINEMATIC && cancellation.simulationMode,"Simulação não entrou no vídeo.");
                press(cancellation,KeyEvent.VK_ESCAPE);
                check(cancellation.screen==GameState.MENU && !cancellation.simulationMode,"Esc do vídeo deixou simulação ativa.");
                return null;
            });
            FutureTask<Void> fence=new FutureTask<>(()->null);Platform.runLater(fence);fence.get(5,TimeUnit.SECONDS);
            edt(() -> {check(cancellation.screen==GameState.MENU,"Callback antigo reabriu a campanha.");return null;});
            check(Arrays.equals(beforeCancellation,Files.readAllBytes(directory.resolve("slot-1.properties"))),"Cancelar gravou progresso simulado.");
        } finally {edt(() -> {cancellation.close();return null;});}
    }

    private static void step(Game game) {
        var control = Game.SIMULATION_CONTROL;
        click(game,(int)control.getCenterX(),(int)control.getCenterY());
    }
    private static void click(Game game,int x,int y) {
        MouseEvent event=new MouseEvent(game,MouseEvent.MOUSE_CLICKED,0,0,x,y,1,false,MouseEvent.BUTTON1);
        for(var listener:game.getMouseListeners()) listener.mouseClicked(event);
    }
    private static void press(Game game,int code) {
        for(var listener:game.getKeyListeners()) listener.keyPressed(new KeyEvent(game,KeyEvent.KEY_PRESSED,0,0,code,KeyEvent.CHAR_UNDEFINED));
        for(var listener:game.getKeyListeners()) listener.keyReleased(new KeyEvent(game,KeyEvent.KEY_RELEASED,0,0,code,KeyEvent.CHAR_UNDEFINED));
    }
    private static void render(Game game,String name) throws Exception {
        Path output=Path.of("out/project-tests/artifacts/ending"); Files.createDirectories(output);
        ImageIO.write(frame(game),"png",output.resolve(name+".png").toFile());
    }
    private static BufferedImage frame(Game game) {
        BufferedImage image=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();try{game.paint(g);}finally{g.dispose();}
        return image;
    }
    private static void verifyIntroContent(Game game) {
        BufferedImage simulated=frame(game), reference;
        game.simulationMode=false;
        try {reference=frame(game);} finally {game.simulationMode=true;}
        for(int y=0;y<540;y+=7)for(int x=0;x<1280;x+=7)
            check(simulated.getRGB(x,y)==reference.getRGB(x,y),"O controle de simulação cobre o conteúdo da apresentação.");
    }
    private static <T>T edt(Callable<T> action)throws Exception {
        FutureTask<T> task=new FutureTask<>(action);SwingUtilities.invokeLater(task);return task.get(30,TimeUnit.SECONDS);
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
