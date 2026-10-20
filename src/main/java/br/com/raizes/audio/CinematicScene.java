package br.com.raizes.audio;

import java.util.Objects;

/** One video and its optional on-screen narration. */
public record CinematicScene(String video, String caption) {
    public CinematicScene {
        video = Objects.requireNonNull(video).strip();
        caption = Objects.requireNonNullElse(caption, "");
        if (video.isEmpty()) throw new IllegalArgumentException("O caminho do vídeo está vazio.");
    }
}
