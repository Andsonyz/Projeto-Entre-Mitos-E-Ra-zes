package br.com.raizes.audio;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javafx.application.Platform;

/** Manual probe of the production video component: P for first scene, button for second. */
public final class CinematicWindowProbe {
    public static void main(String[] args)throws Exception {
        CountDownLatch closed=new CountDownLatch(1);AtomicInteger finished=new AtomicInteger();
        SwingUtilities.invokeLater(()->{
            CinematicManager player=new CinematicManager(System.err::println);
            player.ready().thenRun(()->SwingUtilities.invokeLater(()->{
                JFrame window=new JFrame("Verificação de vídeo - Entre Mitos e Raízes");
                window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JPanel container=new JPanel(new java.awt.BorderLayout());container.add(player.panel());window.setContentPane(container);
                window.setSize(1280,760);window.setLocationRelativeTo(null);window.setVisible(true);
                window.addWindowListener(new java.awt.event.WindowAdapter(){@Override public void windowClosed(java.awt.event.WindowEvent event){player.dispose();closed.countDown();}});
                boolean recovery=args.length>0&&args[0].equals("recovery");
                player.start(List.of(new CinematicScene(recovery?"Videos/probe-missing-1.mp4":"Videos/Cenas/CenaFase1.mp4","Cena 1: pressione P para pular."),
                    new CinematicScene(recovery?"Videos/probe-missing-2.mp4":"Videos/Cenas/CenaFase2.mp4","Cena 2: use o botão Pular esta cena.")),"VERIFICAÇÃO",()->{
                    finished.incrementAndGet();window.setContentPane(new JLabel("Sequência concluída uma vez.",JLabel.CENTER));window.revalidate();window.repaint();
                });
            }));
        });
        if(!closed.await(240,TimeUnit.SECONDS)){System.err.println("Probe não foi fechado");Platform.exit();System.exit(1);}
        Platform.exit();if(finished.get()!=1){System.err.println("Conclusões: "+finished.get());System.exit(1);}
        System.out.println("OK: duas cenas avançadas na janela; conclusão única.");System.exit(0);
    }
}
