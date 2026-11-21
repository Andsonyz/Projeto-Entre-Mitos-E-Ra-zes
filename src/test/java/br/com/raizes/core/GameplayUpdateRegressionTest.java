package br.com.raizes.core;

import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.model.Phase;
import br.com.raizes.model.Thing;
import br.com.raizes.persistence.SaveManager;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javafx.application.Platform;
import static br.com.raizes.config.GameConfig.*;

/** Objectives, guardian exits/thanks, all platform surfaces, celebration and legacy saves. */
public final class GameplayUpdateRegressionTest {
    private static final Path ARTIFACTS=Path.of("out/project-tests/artifacts/gameplay");
    public static void main(String[] args) {
        int result=0;
        try{verify();System.out.println("OK: objetivos reais, saída dos guardiões/quiz, agradecimentos, plataformas e celebração/saves.");}
        catch(Throwable error){result=1;error.printStackTrace();}
        finally{Platform.exit();} System.exit(result);
    }

    private static void verify() throws Exception {
        Files.createDirectories(ARTIFACTS);
        GameAssets assets=GameAssets.load(ignored->{ });
        Path directory=Files.createTempDirectory("raizes-gameplay-update-");
        SaveManager saves=new SaveManager(directory);
        Game game=edt(()->{
            Game created=new Game(new CinematicCatalog((java.io.File)null),saves,assets);
            created.timer.stop();created.setSize(W,H);return created;
        });
        try {
            edt(()->{
                check(game.platformImage.getWidth()==800&&game.platformImage.getHeight()==400,"Modelo de plataforma não foi preparado em 800x400");
                for(int y=0;y<400;y++)for(int x=0;x<800;x++)
                    check((game.platformImage.getRGB(x,y)>>>24)==255,"Modelo ainda tem pixels transparentes");
                render(game,"menu.png");
                for(int i=0;i<5;i++)check(i==3 ? game.menuBounds[i].x<200 : game.menuBounds[i].x>900,"Menu no lado errado");
                game.screen=GameState.PHASE_SELECT;render(game,"celebration-locked.png");
                for(int i=0;i<4;i++)key(game,KeyEvent.VK_DOWN);key(game,KeyEvent.VK_ENTER);
                check(!game.celebrationActive && game.screen==GameState.PHASE_SELECT,"Celebração bloqueada abriu por teclado");
                click(game,500,535);check(!game.celebrationActive,"Celebração bloqueada abriu por mouse");
                invoke(game,"selectSavedPhase",new Class<?>[]{int.class},4);
                check(!game.celebrationActive,"Celebração bloqueada abriu por chamada interna");
                for(int phase=0;phase<4;phase++) {
                    invoke(game,"resetPhaseProgress",new Class<?>[]{int.class},phase);
                    game.screen=GameState.WORLD;
                    if(phase==0)check(game.coins.size()==17 && game.traps.size()==3 && game.objectiveStatus().contains("/17"),"Contagem de sementes/armadilhas");
                    if(phase==2)check(game.fires.size()==9 && game.objectiveStatus().contains("/9"),"Contagem de fogo");
                    if(phase==3)check(game.coins.size()==11 && game.objectiveStatus().contains("/11"),"Contagem de resíduos");
                    if(phase!=1) {
                        game.x=3030;tickWorld(game);
                        check(game.screen==GameState.DIALOGUE,"Guardião não avisou dos objetivos pendentes");
                        check(!Boolean.parseBoolean(saves.read(0).getProperty("campaignComplete","false")),"Campanha liberada sem objetivos");
                        key(game,KeyEvent.VK_ESCAPE);check(game.screen==GameState.WORLD,"Não saiu do diálogo");
                        tickWorld(game);check(game.screen==GameState.WORLD,"Diálogo reabriu sem ação");
                    }
                    // Isolate platform geometry from contact damage in the fire chapter.
                    for(Thing fire:game.fires)fire.hit=true;
                    for(Rectangle platform:game.platforms) {
                        if(platform.y==GROUND_Y)continue;
                        BufferedImage art=game.platformArtwork.get(Game.platformArtKey(platform));
                        check(art!=null && art.getWidth()==platform.width && art.getHeight()==platform.height,"Arte fora da dimensão física");
                        for(int x=0;x<art.getWidth();x++)check((art.getRGB(x,0)>>>24)==255,"Margem invisível sobre plataforma");
                        for(int y=0;y<art.getHeight();y++)check((art.getRGB(0,y)>>>24)==255 && (art.getRGB(art.getWidth()-1,y)>>>24)==255,"Margem invisível lateral");
                        game.x=platform.getCenterX()-PLAYER_WIDTH/2.0;game.y=platform.y-PLAYER_HEIGHT-2;game.vy=150;
                        tickWorld(game);check(Math.abs(game.y+PLAYER_HEIGHT-platform.y)<0.001,"Pés fora da superfície visível: fase="+phase+", plataforma="+platform+", pés="+(game.y+PLAYER_HEIGHT));
                    }
                    game.x=3030;
                    for(Thing item:game.coins)item.hit=true;game.coinsCollected=game.coins.size();
                    for(Thing trap:game.traps)trap.hit=true;for(Thing fire:game.fires)fire.hit=true;
                    game.x=2970;tickWorld(game);game.x=3030;tickWorld(game);
                    check(game.screen==GameState.DIALOGUE,"Não iniciou o diálogo do desafio");
                    for(int i=0;i<8 && game.screen==GameState.DIALOGUE;i++)key(game,KeyEvent.VK_ENTER);
                    check(game.screen==GameState.BATTLE,"Questionário não iniciou");
                    key(game,KeyEvent.VK_ESCAPE);check(game.screen==GameState.WORLD,"Não saiu do quiz por teclado");
                    key(game,KeyEvent.VK_E);
                    for(int i=0;i<8 && game.screen==GameState.DIALOGUE;i++)key(game,KeyEvent.VK_ENTER);
                    click(game,240,625);check(game.screen==GameState.WORLD,"Não saiu do quiz por mouse");
                    key(game,KeyEvent.VK_E);
                    for(int i=0;i<8 && game.screen==GameState.DIALOGUE;i++)key(game,KeyEvent.VK_ENTER);
                    for(int question=0;question<5;question++) {
                        int answer=game.quizQuestions.get(question).correctOption();
                        key(game,KeyEvent.VK_1+answer);key(game,KeyEvent.VK_ENTER);
                    }
                    check(game.screen==GameState.DIALOGUE && game.currentPhase==phase,"Agradecimento foi pulado");
                    check(Boolean.parseBoolean(saves.read(0).getProperty("quizComplete","false")),"Quiz concluído não foi salvo");
                    if(phase==0) {
                        key(game,KeyEvent.VK_ESCAPE);check(game.screen==GameState.WORLD,"Não saiu do agradecimento");
                        game.lives=1;invoke(game,"loseLife",new Class<?>[]{});
                        check(game.coinsCollected==17 && game.traps.stream().allMatch(item->item.hit),"Morrer após o quiz apagou objetivos já concluídos");
                        invoke(game,"saveGame",new Class<?>[]{});
                        invoke(game,"loadSave",new Class<?>[]{int.class},0);
                        check(game.screen==GameState.DIALOGUE && game.coinsCollected==17,"Save não preservou quiz/objetivos concluídos");
                    }
                    render(game,"thanks-"+(phase+1)+".png");
                    for(int i=0;i<5 && game.screen==GameState.DIALOGUE;i++)key(game,KeyEvent.VK_ENTER);
                    if(phase<3)check(game.currentPhase==phase+1 && game.screen==GameState.PHASE_INTRO,"Próxima fase antes/depois do agradecimento");
                }
                check(game.celebrationActive && game.screen==GameState.WORLD,"Celebração não abriu");
                game.screen=GameState.PHASE_SELECT;render(game,"celebration-unlocked.png");
                click(game,500,535);check(game.celebrationActive&&game.screen==GameState.WORLD,"Seleção da celebração concluída não abriu");
                check(game.worldBackground==assets.celebrationBackground,"Background final incorreto");
                check(game.coins.isEmpty()&&game.enemies.isEmpty()&&game.fires.isEmpty()&&game.traps.isEmpty(),"Celebração tem perigos/objetivos de outra fase");
                for(int guardian=0;guardian<4;guardian++) {
                    game.x=Game.CELEBRATION_GUARDIANS[guardian]-20;game.y=GROUND_Y-PLAYER_HEIGHT;
                    game.camera=Math.max(0,Math.min(WORLD_W-W,game.x-340));key(game,KeyEvent.VK_E);
                    check(game.screen==GameState.DIALOGUE,"Guardião final não interagiu: "+guardian);
                    var field=Game.class.getDeclaredField("dialogueGuardian");field.setAccessible(true);
                    check(field.getInt(game)==guardian,"Retrato/nome do guardião final incorreto");
                    render(game,"celebration-guardian-"+guardian+".png");key(game,KeyEvent.VK_ESCAPE);
                    click(game,(int)(Game.CELEBRATION_GUARDIANS[guardian]-game.camera),GROUND_Y-70);
                    check(game.screen==GameState.DIALOGUE,"Guardião não aceitou clique próximo");
                    click(game,350,625);check(game.screen==GameState.WORLD,"Não saiu da conversa por mouse");
                }
                game.camera=0;render(game,"celebration-start.png");game.camera=WORLD_W-W;render(game,"celebration-end.png");
                game.x=1750;invoke(game,"saveGame",new Class<?>[]{});
                invoke(game,"loadSave",new Class<?>[]{int.class},0);
                check(game.celebrationActive&&game.x==1750,"Save final não restaurou posição");
                Properties legacy=saves.read(0);legacy.remove("celebration");legacy.remove("quizComplete");legacy.remove("traversalComplete");
                legacy.setProperty("campaignComplete","true");check(saves.write(0,legacy),"Falha preparando save antigo");
                invoke(game,"loadSave",new Class<?>[]{int.class},0);
                check(game.celebrationActive&&game.x==110,"Save antigo concluído não ganhou a celebração");
                Properties partial=new Properties();partial.setProperty("name","Save antigo");partial.setProperty("phase","0");
                partial.setProperty("coins","12");partial.setProperty("coinMask","fff");partial.setProperty("trapMask","7");partial.setProperty("x","3030");
                check(saves.write(0,partial),"Não gravou save antigo parcial");invoke(game,"loadSave",new Class<?>[]{int.class},0);
                key(game,KeyEvent.VK_ENTER);tickWorld(game);
                check(game.coinsCollected==12&&game.screen==GameState.DIALOGUE,"Save com 12 sementes pulou as cinco restantes");
                var lines=Game.class.getDeclaredField("dialogue");lines.setAccessible(true);
                check(((String[])lines.get(game))[0].contains("5 sementes"),"Lembrete do save antigo não corresponde aos itens restantes");
                return null;
            });
        } finally {edt(()->{game.close();return null;});Files.deleteIfExists(directory.resolve("slot-1.properties"));Files.deleteIfExists(directory);}
    }
    private static void tickWorld(Game game) throws Exception {invoke(game,"updateWorld",new Class<?>[]{double.class},0.02);}
    private static void key(Game game,int key){for(var listener:game.getKeyListeners()){listener.keyPressed(new KeyEvent(game,KeyEvent.KEY_PRESSED,0,0,key,KeyEvent.CHAR_UNDEFINED));listener.keyReleased(new KeyEvent(game,KeyEvent.KEY_RELEASED,0,0,key,KeyEvent.CHAR_UNDEFINED));}}
    private static void click(Game game,int x,int y){for(var listener:game.getMouseListeners())listener.mouseClicked(new MouseEvent(game,MouseEvent.MOUSE_CLICKED,0,0,x,y,1,false,MouseEvent.BUTTON1));}
    private static Object invoke(Game game,String name,Class<?>[] types,Object... values)throws Exception{var method=Game.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(game,values);}
    private static void render(Game game,String name)throws Exception{BufferedImage output=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);var g=output.createGraphics();game.paint(g);g.dispose();ImageIO.write(output,"png",ARTIFACTS.resolve(name).toFile());}
    private static <T>T edt(java.util.concurrent.Callable<T> action)throws Exception{FutureTask<T> task=new FutureTask<>(action);SwingUtilities.invokeLater(task);return task.get(60,TimeUnit.SECONDS);}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
