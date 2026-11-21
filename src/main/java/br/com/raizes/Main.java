package br.com.raizes;

import br.com.raizes.core.Game;
import br.com.raizes.core.GameAssets;
import br.com.raizes.core.LoadingScreen;
import br.com.raizes.assets.AssetManager;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        BufferedImage loadingArtwork = AssetManager.image(GameAssets.LOADING_IMAGE);
        SwingUtilities.invokeLater(() -> createWindow(loadingArtwork));
    }

    static JFrame createWindow(BufferedImage loadingArtwork) {
        JFrame window = new JFrame("Entre Mitos e Raízes - A Trilha do Curupira");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setUndecorated(true);
        window.setBackground(Color.BLACK);
        LoadingScreen loading = new LoadingScreen(loadingArtwork, assets -> openGame(window, assets));
        window.setContentPane(loading);
        window.setResizable(false);
        GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        window.setBounds(screen.getDefaultConfiguration().getBounds());
        window.setVisible(true);
        if (screen.isFullScreenSupported()) screen.setFullScreenWindow(window);
        SwingUtilities.invokeLater(loading::start);
        return window;
    }

    private static void openGame(JFrame window, GameAssets assets) {
        Game game = new Game(assets);
        // Node/NodeHelper must finish on JavaFX before JFXPanel.addNotify runs on Swing.
        game.ready().whenComplete((ignored, error) -> SwingUtilities.invokeLater(() -> {
            if (!window.isDisplayable()) { game.close(); return; }
            if (error != null) {
                game.close();
                if (window.getContentPane() instanceof LoadingScreen loading) loading.fail(error);
                return;
            }
            window.setContentPane(game);
            window.revalidate();
            window.repaint();
            game.requestFocusInWindow();
        }));
    }
}
