package br.com.raizes.audio;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import javax.swing.SwingUtilities;

/** Repeated native decoder startup for every movie, including the ending. */
public final class VideoCompatibilityRegressionTest {
    public static void main(String[] args) {
        int result=0;
        try {
            Path root=Path.of(args.length==0 ? "Videos/Cenas" : args[0]);
            int cycles=Integer.getInteger("raizes.videoCycles",3);
            AtomicInteger errors=new AtomicInteger(), completed=new AtomicInteger(), cancelled=new AtomicInteger();
            CinematicManager manager=edt(() -> new CinematicManager(message -> errors.incrementAndGet(),cancelled::incrementAndGet));
            manager.ready().get(10,TimeUnit.SECONDS);
            try {
                for(int cycle=0;cycle<cycles;cycle++) for(String movie:List.of("CenaFase1","CenaFase2","CenaFase3","CenaFase4","CenaFinal")) {
                    long start=System.nanoTime();
                    manager.start(List.of(new CinematicScene(root.resolve(movie+".mp4").toAbsolutePath().toString(),"")),"COMPATIBILIDADE",completed::incrementAndGet);
                    MediaPlayer player=null;
                    long deadline=start+TimeUnit.SECONDS.toNanos(25);
                    while(System.nanoTime()<deadline) {
                        player=fx(() -> player(manager));
                        if(player!=null && fx(player::getStatus)==MediaPlayer.Status.PLAYING) break;
                        check(manager.state()!=CinematicManager.PlaybackState.FAILED,"Falha em "+movie);
                        Thread.sleep(20);
                    }
                    check(player!=null && fx(player::getStatus)==MediaPlayer.Status.PLAYING,"Timeout: "+movie);
                    double latency=(System.nanoTime()-start)/1e6;
                    MediaPlayer playing=player;
                    check(fx(() -> playing.getMedia().getWidth())>0,"Vídeo sem dimensões decodificadas.");
                    CountDownLatch responsive=new CountDownLatch(2);
                    SwingUtilities.invokeLater(responsive::countDown);Platform.runLater(responsive::countDown);
                    check(responsive.await(2,TimeUnit.SECONDS),"Interface bloqueada.");
                    Thread.sleep(1000);
                    check(fx(() -> playing.getCurrentTime().toMillis())>200,"Relógio do vídeo parado.");
                    manager.cancel(); fx(() -> null); edt(() -> null);
                    check(fx(playing::getStatus)==MediaPlayer.Status.DISPOSED,"Player retido após cancelamento.");
                    long committed=((com.sun.management.OperatingSystemMXBean)java.lang.management.ManagementFactory.getOperatingSystemMXBean()).getCommittedVirtualMemorySize();
                    System.out.printf(java.util.Locale.ROOT,"cycle=%d movie=%s startup_ms=%.1f committed_mib=%.1f%n",cycle+1,movie,latency,committed/1048576.0);
                }
                check(errors.get()==0 && completed.get()==0 && cancelled.get()==cycles*5,"Callbacks de falha/finalização incorretos.");
            } finally {manager.dispose();fx(() -> null);}
            System.out.println("OK: cinco vídeos, preparação repetida, progresso, interfaces responsivas e descarte.");
        } catch(Throwable error) {result=1;error.printStackTrace();}
        finally {Platform.exit();}
        System.exit(result);
    }
    private static MediaPlayer player(CinematicManager manager)throws Exception {
        var field=CinematicManager.class.getDeclaredField("player");field.setAccessible(true);return (MediaPlayer)field.get(manager);
    }
    private static <T>T fx(Callable<T> action)throws Exception {
        FutureTask<T> task=new FutureTask<>(action);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);
    }
    private static <T>T edt(Callable<T> action)throws Exception {
        FutureTask<T> task=new FutureTask<>(action);SwingUtilities.invokeLater(task);return task.get(10,TimeUnit.SECONDS);
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
