package br.com.raizes;

import br.com.raizes.assets.AssetManager;
import br.com.raizes.core.Game;
import br.com.raizes.core.GameAssets;
import br.com.raizes.core.LoadingScreen;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javafx.application.Platform;

/** Fresh process, real visible production window: catches Swing/JavaFX class-init deadlocks. */
public final class StartupRegressionTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch complete=new CountDownLatch(1);
        AtomicInteger pulses=new AtomicInteger();
        SwingUtilities.invokeLater(() -> {
            JFrame window=Main.createWindow(AssetManager.image(GameAssets.LOADING_IMAGE));
            if(!(window.getContentPane() instanceof LoadingScreen)) throw new AssertionError("Menu abriu antes do carregamento");
            Timer monitor=new Timer(25,null);
            monitor.addActionListener(event -> {
                pulses.incrementAndGet();
                if(window.getContentPane() instanceof Game) {
                    monitor.stop(); window.dispose(); complete.countDown();
                }
            });
            monitor.start();
        });
        if(!complete.await(30,TimeUnit.SECONDS)) {
            System.err.println("Falha: abertura bloqueada; pulsos EDT="+pulses.get());
            Thread.getAllStackTraces().forEach((thread,stack)-> {
                if(thread.getName().contains("AWT-EventQueue") || thread.getName().contains("JavaFX")) {
                    System.err.println(thread); for(var frame:stack) System.err.println("  "+frame);
                }
            });
            System.exit(1);
        }
        Platform.exit();
        System.out.println("OK: janela real carregamento → menu sem bloqueio Swing/JavaFX; pulsos EDT="+pulses.get());
        System.exit(0);
    }
}
