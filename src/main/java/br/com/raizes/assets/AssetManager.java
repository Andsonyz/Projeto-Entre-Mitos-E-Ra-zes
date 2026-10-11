package br.com.raizes.assets;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Resolves game assets from the classpath, with a project-folder fallback for development. */
public final class AssetManager {
    private static final Set<String> WARNED_MISSING = ConcurrentHashMap.newKeySet();

    private AssetManager() { }

    public static BufferedImage image(String path) {
        String resourcePath = normalize(path);
        try (InputStream input = AssetManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input != null) return ImageIO.read(input);
        } catch (IOException | RuntimeException exception) {
            warn("Could not read image '" + path + "': " + exception.getMessage());
            return null;
        }

        for (File candidate : fileCandidates(path, resourcePath)) {
            if (!candidate.isFile()) continue;
            try { return ImageIO.read(candidate); }
            catch (IOException | RuntimeException exception) {
                warn("Could not read image '" + candidate + "': " + exception.getMessage());
                return null;
            }
        }
        warn("Image asset not found: " + path);
        return null;
    }

    /** Returns a filesystem file for APIs such as JavaFX Media that require a URI. */
    public static File file(String path) {
        return resolveFile(path, true);
    }

    /** Returns a resource file without logging when the resource is intentionally optional. */
    public static File optionalFile(String path) {
        return resolveFile(path, false);
    }

    private static File resolveFile(String path, boolean warnIfMissing) {
        String resourcePath = normalize(path);
        for (File candidate : fileCandidates(path, resourcePath)) {
            if (candidate.isFile()) return candidate;
        }

        URL resource = AssetManager.class.getClassLoader().getResource(resourcePath);
        if (resource != null && "file".equalsIgnoreCase(resource.getProtocol())) {
            try { return Path.of(resource.toURI()).toFile(); }
            catch (URISyntaxException | IllegalArgumentException exception) {
                warn("Invalid asset URL for '" + path + "': " + exception.getMessage());
            }
        }
        if (resource != null) {
            String extension = resourcePath.contains(".") ? resourcePath.substring(resourcePath.lastIndexOf('.')) : ".asset";
            try (InputStream input = resource.openStream()) {
                Path temporaryFile = Files.createTempFile("raizes-asset-", extension);
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
                temporaryFile.toFile().deleteOnExit();
                return temporaryFile.toFile();
            } catch (IOException exception) {
                warn("Could not extract asset '" + path + "': " + exception.getMessage());
            }
        }
        if (warnIfMissing) warn("File asset not found: " + path);
        return null;
    }

    private static File[] fileCandidates(String original, String resourcePath) {
        if (original.equals(resourcePath)) return new File[] { new File(resourcePath) };
        return new File[] { new File(original), new File(resourcePath) };
    }

    private static String normalize(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.startsWith("src/main/resources/")) normalized = normalized.substring("src/main/resources/".length());
        return normalized;
    }

    private static void warn(String message) {
        if (WARNED_MISSING.add(message)) System.err.println("[Entre Mitos e Raízes] " + message);
    }
}
