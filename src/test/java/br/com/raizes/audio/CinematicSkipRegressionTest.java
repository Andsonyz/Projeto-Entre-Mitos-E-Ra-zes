package br.com.raizes.audio;

import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.MediaPlayer;

/** Real first scene, per-scene skip, P repeat, stale media callback, cancel and failed-file skip. */
public final class CinematicSkipRegressionTest {
    public static void main(String[] args) {
        int result=0;
        try{verify();System.out.println("OK: pular por cena/tecla/botão/API, autorepetição, callback antigo, falha e cancelamento.");}
        catch(Throwable error){result=1;error.printStackTrace();}
        finally{Platform.exit();}System.exit(result);
    }
    private static void verify() throws Exception {
        AtomicInteger completed=new AtomicInteger(),cancelled=new AtomicInteger();
        CinematicManager manager=edt(()->new CinematicManager(System.err::println,cancelled::incrementAndGet));
        try {
            manager.ready().get(15,TimeUnit.SECONDS);
            manager.start(List.of(new CinematicScene("Videos/Cenas/CenaFase1.mp4","Primeira"),
                new CinematicScene("Videos/skip-missing-second.mp4","Segunda"),
                new CinematicScene("Videos/skip-missing-third.mp4","Terceira")),"PULAR",completed::incrementAndGet);
            await(()->manager.state()==CinematicManager.PlaybackState.PLAYING,"Primeira cena não reproduziu");
            fx(()->{
                var root=((JFXPanel)manager.panel()).getScene().getRoot();root.applyCss();root.layout();
                var path=java.nio.file.Path.of("out/project-tests/artifacts/cinematic-skip-playing.png");
                java.nio.file.Files.createDirectories(path.getParent());
                javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",path.toFile());
                return null;
            });
            MediaPlayer old=fx(()->(MediaPlayer)field(manager,"player"));Runnable stale=fx(old::getOnEndOfMedia);
            fx(()->{key(manager,KeyEvent.KEY_PRESSED);check(index(manager)==1,"P não pulou apenas a primeira cena");
                stale.run();check(index(manager)==1,"Callback antigo pulou a segunda cena");
                key(manager,KeyEvent.KEY_PRESSED);check(index(manager)==1,"P segurado pulou outra cena");
                key(manager,KeyEvent.KEY_RELEASED);return null;});
            check(fx(old::getStatus)==MediaPlayer.Status.DISPOSED,"Player pulado não foi descartado");
            manager.skipScene();fx(()->null);check(fx(()->index(manager))==2,"API pulou mais de uma cena");
            fx(()->{((Button)field(manager,"skipButton")).fire();return null;});
            edt(()->null);check(completed.get()==1,"Conclusão da sequência não foi única");
            fx(()->{stale.run();return null;});manager.skipScene();fx(()->null);edt(()->null);
            check(completed.get()==1,"Callback repetido concluiu novamente");
            manager.start(List.of(new CinematicScene("Videos/skip-missing-only.mp4","Falha")),"ERRO",completed::incrementAndGet);
            await(()->manager.state()==CinematicManager.PlaybackState.FAILED,"Falha não chegou à recuperação");
            fx(()->{((Button)field(manager,"skipButton")).fire();return null;});edt(()->null);
            check(completed.get()==2,"Não foi possível pular uma cena com falha");
            manager.start(List.of(new CinematicScene("Videos/Cenas/CenaFase1.mp4","Cancelar")),"CANCELAR",completed::incrementAndGet);
            manager.cancel();manager.skipScene();fx(()->null);edt(()->null);
            check(cancelled.get()==1 && completed.get()==2,"Cancelar virou conclusão/pulo");
        } finally {manager.dispose();fx(()->null);}
    }
    private static void key(CinematicManager manager,javafx.event.EventType<KeyEvent> type){
        Event.fireEvent(((JFXPanel)manager.panel()).getScene(),new KeyEvent(type,"p","p",KeyCode.P,false,false,false,false));
    }
    private static int index(CinematicManager manager)throws Exception{return (Integer)field(manager,"sceneIndex");}
    private static Object field(Object target,String name)throws Exception{var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
    private static <T>T fx(java.util.concurrent.Callable<T> action)throws Exception{FutureTask<T> task=new FutureTask<>(action);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);}
    private static <T>T edt(java.util.concurrent.Callable<T> action)throws Exception{FutureTask<T> task=new FutureTask<>(action);SwingUtilities.invokeLater(task);return task.get(15,TimeUnit.SECONDS);}
    private static void await(BooleanSupplier condition,String message){long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);while(!condition.getAsBoolean()&&System.nanoTime()<deadline)java.util.concurrent.locks.LockSupport.parkNanos(2_000_000);check(condition.getAsBoolean(),message);}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
