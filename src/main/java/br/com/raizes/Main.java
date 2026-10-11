package br.com.raizes;

import br.com.raizes.core.Game;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Entre Mitos e Raízes - A Trilha do Curupira");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new Game());
            window.pack();
            window.setLocationRelativeTo(null);
            window.setResizable(false);
            window.setVisible(true);
        });
    }
}
