package br.com.raizes.audio;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import br.com.raizes.assets.AssetManager;
import java.io.BufferedInputStream;

/** Actual WAV decoding, partial writes, loops, rapid changes and one output at a time. */
public final class AmbientAudioRegressionTest {
    public static void main(String[] args) throws Exception {
        AmbientAudio.validateAssets();
        AtomicInteger opened=new AtomicInteger(), active=new AtomicInteger(), maxActive=new AtomicInteger();
        AtomicLong written=new AtomicLong();
        AtomicLong forestWritten=new AtomicLong();
        long forestBytes;
        try (var input=AudioSystem.getAudioInputStream(new BufferedInputStream(AssetManager.open(AmbientAudio.Track.FOREST.path)))) {
            forestBytes=input.getFrameLength()*input.getFormat().getFrameSize();
        }
        AmbientAudio player=new AmbientAudio(format -> {
            int outputIndex=opened.incrementAndGet(); maxActive.accumulateAndGet(active.incrementAndGet(),Math::max);
            return (SourceDataLine)Proxy.newProxyInstance(SourceDataLine.class.getClassLoader(),new Class<?>[]{SourceDataLine.class},
                (proxy,method,values)-> {
                    if(method.getName().equals("write")) {
                        int count=Math.min(512,(Integer)values[2]); written.addAndGet(count);
                        if(outputIndex==2) forestWritten.addAndGet(count);
                        return count;
                    }
                    if(method.getName().equals("close")) active.decrementAndGet();
                    if(method.getReturnType()==boolean.class) return false;
                    if(method.getReturnType()==int.class) return 0;
                    if(method.getReturnType()==long.class) return 0L;
                    if(method.getReturnType()==float.class) return 0f;
                    return null;
                });
        });
        try {
            player.play(AmbientAudio.Track.MENU);
            await(()->player.completedLoops()>=2,"Menu não repetiu o WAV inteiro.");
            check(written.get()>28_000_000,"Áudio não foi decodificado integralmente.");
            int outputs=opened.get(); player.play(AmbientAudio.Track.MENU);
            check(opened.get()==outputs,"Mesma faixa reiniciou.");
            long previous=player.completedLoops(); player.play(AmbientAudio.Track.FOREST);
            await(()->opened.get()>=2 && forestWritten.get()>2*forestBytes && player.completedLoops()>previous,
                "Floresta não reproduziu duas vezes o WAV inteiro.");
            player.play(AmbientAudio.Track.SILENCE); await(()->active.get()==0,"Cinemática não silenciou.");
            for(int i=0;i<20;i++) { player.play(AmbientAudio.Track.MENU); player.play(AmbientAudio.Track.FOREST); }
            player.close(); await(player::terminated,"Worker não encerrou.");
            check(active.get()==0 && maxActive.get()==1,"Saídas simultâneas ou não liberadas.");
            check(player.failure()==null,"Falha de decodificação: "+player.failure());
            System.out.println("OK: WAVs completos, looping, troca, silêncio, chamadas repetidas e liberação.");
        } finally { player.close(); }
    }
    private static void await(BooleanSupplier condition,String message) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(12);
        while(!condition.getAsBoolean() && System.nanoTime()<deadline) java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
        check(condition.getAsBoolean(),message);
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
}
