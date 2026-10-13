package br.com.raizes;

import br.com.raizes.core.Game;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Entre Mitos e Raízes - A Trilha do Curupira");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setUndecorated(true);
            window.setBackground(Color.BLACK);
            window.setContentPane(new Game());
            window.setResizable(false);
            GraphicsDevice screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            window.setBounds(screen.getDefaultConfiguration().getBounds());
            window.setVisible(true);
            if (screen.isFullScreenSupported()) screen.setFullScreenWindow(window);
        });
    }
}
