package br.com.raizes.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import static br.com.raizes.config.GameConfig.*;

/** Visible before the resource worker starts. Completion, not a delay, opens the menu. */
public final class LoadingScreen extends JPanel {
    @FunctionalInterface interface Loader { GameAssets load(IntConsumer progress) throws Exception; }
    private final BufferedImage artwork;
    private final Consumer<GameAssets> ready;
    private final Loader loader;
    private final JButton retry = new JButton("Tentar novamente");
    private SwingWorker<GameAssets, Integer> worker;
    private int progress;
    private String error;

    public LoadingScreen(BufferedImage artwork, Consumer<GameAssets> ready) {
        this(artwork, ready, GameAssets::load);
    }

    LoadingScreen(BufferedImage artwork, Consumer<GameAssets> ready, Loader loader) {
        this.artwork = artwork;
        this.ready = ready;
        this.loader = loader;
        setBackground(Color.BLACK);
        setLayout(null);
        retry.setVisible(false);
        retry.addActionListener(event -> start());
        add(retry);
    }

    public void start() {
        if (worker != null && !worker.isDone()) return;
        error = null;
        progress = 0;
        retry.setVisible(false);
        worker = new SwingWorker<>() {
            @Override protected GameAssets doInBackground() throws Exception { return loader.load(value -> publish(value)); }
            @Override protected void process(List<Integer> values) {
                // The final preparation step is the asynchronous JavaFX panel, before menu attachment.
                progress = Math.min(95, values.getLast());
                repaint();
            }
            @Override protected void done() {
                if (isCancelled() || !isDisplayable()) return;
                try { ready.accept(get()); }
                catch (CancellationException ignored) { }
                catch (Exception failure) {
                    fail(failure);
                }
            }
        };
        worker.execute();
    }

    public void fail(Throwable failure) {
        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
        error = "Falha ao carregar os recursos. Verifique os arquivos e tente novamente.";
        System.err.println("[Entre Mitos e Raízes] " + cause);
        retry.setVisible(true);
        repaint();
    }

    @Override public void doLayout() {
        retry.setBounds(Math.max(0, getWidth() / 2 - 110), Math.max(0, getHeight() - 100), 220, 36);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            double scale = Math.min(getWidth() / (double) W, getHeight() / (double) H);
            g.translate((getWidth() - W * scale) / 2, (getHeight() - H * scale) / 2);
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (artwork != null) g.drawImage(artwork, 0, 0, W, H, null);
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRoundRect(300, 691, 680, 18, 8, 8);
            g.setColor(new Color(230, 199, 125));
            g.fillRoundRect(304, 695, 672 * progress / 100, 10, 6, 6);
            if (error != null || artwork == null) {
                g.setColor(new Color(0, 0, 0, 210)); g.fillRoundRect(100, 460, 1080, 90, 12, 12);
                g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
                g.drawString(error == null ? "Preparando o jogo..." : error, 130, 510);
            }
        } finally { g.dispose(); }
    }

    @Override public void removeNotify() {
        if (worker != null && !worker.isDone()) worker.cancel(true);
        super.removeNotify();
    }
}
