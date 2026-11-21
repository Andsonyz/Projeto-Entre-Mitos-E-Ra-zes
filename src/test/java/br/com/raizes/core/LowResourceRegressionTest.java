package br.com.raizes.core;

import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.persistence.SaveManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Static screens sleep; reduced rendering preserves live physics and responsive input. */
public final class LowResourceRegressionTest {
    public static void main(String[] args) {
        int result=0;
        try {verify();System.out.println("OK: interface estática sem repintura contínua; modo leve preserva física e suspensão.");}
        catch(Throwable error){result=1;error.printStackTrace();}
        finally{Platform.exit();}
        System.exit(result);
    }
    private static void verify()throws Exception {
        System.setProperty("raizes.lowPower","true");
        var saves=Files.createTempDirectory("raizes-low-resource-");
        GameAssets assets=GameAssets.load(ignored -> { });
        Game game=edt(() -> {Game created=new Game(new CinematicCatalog((java.io.File)null),new SaveManager(saves),assets);created.setSize(1280,720);return created;});
        AtomicInteger paints=new AtomicInteger();
        RepaintManager previous=edt(() -> RepaintManager.currentManager(game));
        edt(() -> {RepaintManager.setCurrentManager(new RepaintManager(){
            @Override public void addDirtyRegion(JComponent component,int x,int y,int w,int h){
                if(component==game)paints.incrementAndGet();super.addDirtyRegion(component,x,y,w,h);
            }
        });return null;});
        try {
            interval(900);paints.set(0);interval(600);
            check(paints.get()==0,"Menu ocioso repintou "+paints.get()+" vezes.");
            double before=edt(() -> {
                game.screen=GameState.WORLD;game.held.add(KeyEvent.VK_D);paints.set(0);
                // High-frequency mouse movement and OS key repeat must not bypass the frame limit.
                for(int i=0;i<100;i++) {
                    MouseEvent move=new MouseEvent(game,MouseEvent.MOUSE_MOVED,0,0,100+i,200,0,false);
                    for(var listener:game.getMouseMotionListeners())listener.mouseMoved(move);
                    KeyEvent repeat=new KeyEvent(game,KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_D,'d');
                    for(var listener:game.getKeyListeners())listener.keyPressed(repeat);
                }
                return game.x;
            });
            interval(600);
            double moved=edt(() -> game.x-before);
            int worldPaints=paints.get();
            check(moved>100 && moved<210,"Física alterada pelo limite de pintura: "+moved);
            check(worldPaints>=8 && worldPaints<=21,"Limite de renderização não aplicado: "+worldPaints);
            double frozen=edt(() -> {game.screen=GameState.CINEMATIC;paints.set(0);return game.x;});
            interval(400);
            check(edt(() -> game.x)==frozen && paints.get()==0,"Mundo ativo sob vídeo.");
            System.out.printf(java.util.Locale.ROOT,"idle_paints=0 world_paints_600ms=%d distance=%.1f heap_limit_mib=%d%n",worldPaints,moved,Runtime.getRuntime().maxMemory()/1024/1024);
        }finally{edt(() -> {game.close();RepaintManager.setCurrentManager(previous);return null;});}
    }
    private static void interval(int millis)throws Exception {
        var done=new java.util.concurrent.CountDownLatch(1);
        edt(() -> {Timer timer=new Timer(millis,e->done.countDown());timer.setRepeats(false);timer.start();return null;});
        check(done.await(millis+2000,TimeUnit.MILLISECONDS),"EDT não respondeu.");
    }
    private static <T>T edt(Callable<T> action)throws Exception {
        FutureTask<T> task=new FutureTask<>(action);SwingUtilities.invokeLater(task);return task.get(20,TimeUnit.SECONDS);
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
