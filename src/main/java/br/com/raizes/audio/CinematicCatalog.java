package br.com.raizes.audio;

import br.com.raizes.assets.AssetManager;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads ordered playlists once; changing videos does not require recompiling Java. */
public final class CinematicCatalog {
    private static final Pattern VIDEO_KEY = Pattern.compile("(prologue|chapter\\.[1-9]\\d*)\\.([1-9]\\d*)\\.video");
    private final Map<String, List<CinematicScene>> playlists;

    public CinematicCatalog() {
        this(AssetManager.optionalFile("Videos/cinematics.properties"));
    }

    public CinematicCatalog(File configuration) {
        playlists = load(configuration);
    }

    public List<CinematicScene> prologue() { return playlists.getOrDefault("prologue", List.of()); }

    public List<CinematicScene> beforeChapter(int chapter) {
        return playlists.getOrDefault("chapter." + chapter, List.of());
    }

    private static Map<String, List<CinematicScene>> load(File configuration) {
        if (configuration == null) return Map.of();
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configuration.toPath(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("[Entre Mitos e Raízes] Não foi possível ler " + configuration + ": " + exception);
            return Map.of();
        }

        Map<String, TreeMap<Integer, CinematicScene>> ordered = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            Matcher match = VIDEO_KEY.matcher(key);
            if (!match.matches()) continue;
            String video = properties.getProperty(key).strip();
            if (video.isEmpty()) continue;
            try {
                int index = Integer.parseInt(match.group(2));
                String prefix = key.substring(0, key.length() - "video".length());
                String caption = properties.getProperty(prefix + "caption", "");
                ordered.computeIfAbsent(match.group(1), ignored -> new TreeMap<>())
                    .put(index, new CinematicScene(video, caption));
            } catch (IllegalArgumentException exception) {
                System.err.println("[Entre Mitos e Raízes] Cena inválida em " + key + ": " + exception.getMessage());
            }
        }
        Map<String, List<CinematicScene>> result = new HashMap<>();
        ordered.forEach((name, scenes) -> result.put(name, List.copyOf(scenes.values())));
        return Map.copyOf(result);
    }
}
