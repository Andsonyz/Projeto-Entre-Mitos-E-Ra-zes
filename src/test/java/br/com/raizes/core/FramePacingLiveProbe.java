package br.com.raizes.core;

import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.persistence.SaveManager;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javafx.application.Platform;

/** Optional native-window probe, usable against the preserved before-build as well. */
public final class FramePacingLiveProbe {
    public static void main(String[] args)throws Exception {
        var directory=Files.createTempDirectory("raizes-live-pacing-");
        GameAssets assets=GameAssets.load(ignored->{ });
        CountDownLatch finished=new CountDownLatch(1);
        SwingUtilities.invokeLater(()->{
            Game game=new Game(new CinematicCatalog((java.io.File)null),new SaveManager(directory),assets);
            game.timer.stop();
            game.ready().thenRun(()->SwingUtilities.invokeLater(()->{
                JFrame window=new JFrame("Verificação de fluidez - Entre Mitos e Raízes");
                window.setUndecorated(true);window.setContentPane(game);window.setSize(1536,864);window.setVisible(true);
                game.screen=GameState.WORLD;game.enemies.forEach(enemy->enemy.hit=true);
                game.held.add(java.awt.event.KeyEvent.VK_D);game.held.add(java.awt.event.KeyEvent.VK_SPACE);
                long[] gaps=new long[600];var paints=new ArrayList<Long>();
                int[] sample={-90};long[] previous={System.nanoTime()},gc={0};
                RepaintManager.setCurrentManager(new RepaintManager(){
                    @Override public void paintDirtyRegions(){long start=System.nanoTime();super.paintDirtyRegions();if(sample[0]>=0)paints.add(System.nanoTime()-start);}
                });
                Timer monitor=new Timer(16,null);
                monitor.addActionListener(event->{
                    long now=System.nanoTime();if(game.x>2800){game.x=110;game.y=500;}
                    if(sample[0]==0)gc[0]=gcMillis();
                    if(sample[0]>=0 && sample[0]<gaps.length)gaps[sample[0]]=now-previous[0];
                    previous[0]=now;
                    if(++sample[0]==gaps.length){
                        monitor.stop();game.timer.stop();Arrays.sort(gaps);
                        long[] measured=paints.stream().mapToLong(Long::longValue).sorted().toArray();
                        System.out.printf(java.util.Locale.ROOT,"native_gap_p95=%.3f native_gap_p99=%.3f native_gap_max=%.3f paints=%d paint_p95=%.3f gc_ms=%d%n",
                            gaps[569]/1e6,gaps[593]/1e6,gaps[599]/1e6,measured.length,
                            measured.length==0?0:measured[(int)(measured.length*.95)-1]/1e6,gcMillis()-gc[0]);
                        window.dispose();finished.countDown();
                    }
                });
                game.timer.start();monitor.start();
            }));
        });
        if(!finished.await(45,TimeUnit.SECONDS)){System.err.println("Probe não concluiu na janela real");System.exit(1);}
        Platform.exit();Files.deleteIfExists(directory);System.exit(0);
    }
    private static long gcMillis(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean->Math.max(0,bean.getCollectionTime())).sum();}
}
