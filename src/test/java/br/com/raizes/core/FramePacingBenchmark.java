package br.com.raizes.core;

import br.com.raizes.audio.CinematicCatalog;
import br.com.raizes.persistence.SaveManager;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;
import javafx.application.Platform;

/** Repeatable paint benchmark; reports measured values without inventing an FPS guarantee. */
public final class FramePacingBenchmark {
    public static void main(String[] args) throws Exception {
        Path saves=Files.createTempDirectory("raizes-frame-pacing-");
        GameAssets assets=GameAssets.load(ignored -> { });
        FutureTask<Void> task=new FutureTask<>(() -> {
            Game game=new Game(new CinematicCatalog((java.io.File)null),new SaveManager(saves),assets);
            game.timer.stop(); game.setSize(1536,864); game.screen=GameState.WORLD;
            BufferedImage frame=new BufferedImage(1536,864,BufferedImage.TYPE_INT_RGB);
            var build=Game.class.getDeclaredMethod("buildLevel");build.setAccessible(true);
            try {
                for(int phase=0;phase<4;phase++) {
                    game.currentPhase=phase;build.invoke(game);
                    for(int i=0;i<200;i++) paint(game,frame,i);
                    long[] times=new long[600]; long gcBefore=gcMillis();
                    for(int i=0;i<times.length;i++) {long start=System.nanoTime();paint(game,frame,i);times[i]=System.nanoTime()-start;}
                    long gc=gcMillis()-gcBefore; Arrays.sort(times);
                    System.out.printf(java.util.Locale.ROOT,"phase=%d mean=%.3f p95=%.3f p99=%.3f max=%.3f gc_ms=%d%n",
                        phase+1,Arrays.stream(times).average().orElseThrow()/1e6,times[569]/1e6,times[593]/1e6,times[599]/1e6,gc);
                }
            } finally {game.close();}
            return null;
        });
        SwingUtilities.invokeLater(task);
        try{task.get();}finally{Platform.exit();Files.deleteIfExists(saves);}
        System.exit(0);
    }
    private static void paint(Game game,BufferedImage frame,int i) {
        game.camera=(i*7.5)%1920;game.elapsed=i/60.0;
        var graphics=frame.createGraphics();try{game.paint(graphics);}finally{graphics.dispose();}
    }
    private static long gcMillis(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(bean->Math.max(0,bean.getCollectionTime())).sum();}
}
