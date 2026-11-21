package br.com.raizes.audio;

import br.com.raizes.assets.AssetManager;
import java.io.BufferedInputStream;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** One streaming audio output. No whole-track PCM buffers or work on the Swing thread. */
public final class AmbientAudio implements AutoCloseable {
    public enum Track {
        SILENCE(null), MENU("Audio/MenuTheme.wav"), FOREST("Audio/ForestTheme.wav");
        final String path;
        Track(String path) { this.path = path; }
    }

    @FunctionalInterface interface Output {
        SourceDataLine open(javax.sound.sampled.AudioFormat format) throws Exception;
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor(action -> {
        Thread thread = new Thread(action, "raizes-ambient-audio");
        thread.setDaemon(true);
        return thread;
    });
    private final Output output;
    private final Set<Track> warned = EnumSet.noneOf(Track.class);
    private final AtomicLong generation = new AtomicLong();
    private volatile Track requested = Track.SILENCE;
    private volatile boolean closed;
    private volatile String failure;
    private final AtomicLong completedLoops = new AtomicLong();

    public AmbientAudio() {
        this(format -> {
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            try { line.open(format, 16384); return line; }
            catch (Exception error) { line.close(); throw error; }
        });
    }

    AmbientAudio(Output output) { this.output = output; }

    public synchronized void play(Track track) {
        java.util.Objects.requireNonNull(track);
        if (closed || requested == track) return;
        requested = track;
        long token = generation.incrementAndGet();
        worker.execute(() -> stream(track, token));
    }

    private boolean active(long token) { return !closed && generation.get() == token; }

    private void stream(Track track, long token) {
        if (track == Track.SILENCE || !active(token)) return;
        SourceDataLine line = null;
        byte[] buffer = new byte[4096];
        try {
            while (active(token)) {
                try (AudioInputStream input = input(track)) {
                    if (!active(token)) return;
                    if (line == null) { line = output.open(input.getFormat()); line.start(); }
                    int count;
                    while (active(token) && (count = input.read(buffer)) != -1) {
                        int offset = 0;
                        while (offset < count && active(token)) {
                            int written = line.write(buffer, offset, count - offset);
                            if (written <= 0) throw new IllegalStateException("Saída de áudio interrompida.");
                            offset += written;
                        }
                    }
                    if (active(token)) completedLoops.incrementAndGet();
                }
            }
        } catch (Exception error) {
            failure = track + ": " + error.getMessage();
            if (warned.add(track)) System.err.println("[Entre Mitos e Raízes] Áudio ambiente indisponível: " + failure);
        } finally {
            if (line != null) { line.stop(); line.flush(); line.close(); }
        }
    }

    private static AudioInputStream input(Track track) throws Exception {
        BufferedInputStream stream = new BufferedInputStream(AssetManager.open(track.path));
        try { return AudioSystem.getAudioInputStream(stream); }
        catch (Exception error) { stream.close(); throw error; }
    }

    /** Validate headers at startup; long audio and video files are streamed when needed. */
    public static void validateAssets() {
        for (Track track : new Track[] { Track.MENU, Track.FOREST }) {
            try (AudioInputStream input = input(track)) {
                if (input.getFrameLength() <= 0 || input.getFormat().getFrameSize() <= 0)
                    throw new IllegalArgumentException("Áudio vazio ou inválido: " + track.path);
            } catch (Exception error) { throw new IllegalStateException("Não foi possível preparar " + track.path, error); }
        }
    }

    public Track requestedTrack() { return requested; }
    public String failure() { return failure; }
    long completedLoops() { return completedLoops.get(); }
    boolean terminated() { return worker.isTerminated(); }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        requested = Track.SILENCE;
        generation.incrementAndGet();
        worker.shutdown();
    }
}
