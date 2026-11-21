package br.com.raizes.audio;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Optional hardware smoke check: advances real mixer frames, then releases both outputs. */
public final class AmbientAudioDeviceTest {
    public static void main(String[] args) throws Exception {
        AtomicInteger outputs = new AtomicInteger();
        AtomicReference<SourceDataLine> current = new AtomicReference<>();
        AmbientAudio player = new AmbientAudio(format -> {
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, 16384); current.set(line); outputs.incrementAndGet(); return line;
        });
        try {
            player.play(AmbientAudio.Track.MENU);
            await(() -> current.get()!=null && current.get().getLongFramePosition()>22050, player, "Menu sem avanço na saída real");
            SourceDataLine menu=current.get();
            player.play(AmbientAudio.Track.FOREST);
            await(() -> outputs.get()==2 && current.get().getLongFramePosition()>22050, player, "Floresta sem avanço na saída real");
            if(menu.isOpen()) throw new AssertionError("Saída anterior continuou aberta");
            SourceDataLine forest=current.get(); player.play(AmbientAudio.Track.SILENCE);
            await(() -> !forest.isOpen(), player, "Saída não silenciou");
            player.close(); await(player::terminated, player, "Worker não encerrou");
            System.out.println("OK: MenuTheme e ForestTheme avançaram mais de 22050 frames no mixer real; canais liberados.");
        } finally { player.close(); }
    }
    private static void await(BooleanSupplier condition, AmbientAudio player, String message) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(!condition.getAsBoolean() && player.failure()==null && System.nanoTime()<deadline)
            java.util.concurrent.locks.LockSupport.parkNanos(2_000_000);
        if(!condition.getAsBoolean()) throw new AssertionError(message+"; "+player.failure());
    }
}
