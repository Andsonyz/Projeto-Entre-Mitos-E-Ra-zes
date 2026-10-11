package br.com.raizes.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** Reads and writes the two campaign save slots. */
public final class SaveManager {
    private final File directory = new File(System.getProperty("user.home"), ".entremitoseraizes/saves");
    private final Map<Integer, CachedSlot> cache = new HashMap<>();

    public synchronized Properties read(int slot) {
        fileFor(slot); // Validate the slot before consulting the cache.
        File file = fileFor(slot);
        long modified = file.isFile() ? file.lastModified() : -1;
        long length = file.isFile() ? file.length() : -1;
        CachedSlot cached = cache.get(slot);
        if (cached != null && cached.modified == modified && cached.length == length) return copy(cached.properties);

        Properties properties = new Properties();
        if (file.isFile()) {
            try (FileInputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException | IllegalArgumentException exception) {
                properties.clear();
                System.err.println("[Entre Mitos e Raízes] Não foi possível ler o save '" + file + "': " + exception.getMessage());
            }
        }
        cache.put(slot, new CachedSlot(modified, length, copy(properties)));
        return copy(properties);
    }

    public synchronized boolean hasSave(int slot) {
        Properties save = read(slot);
        try {
            return fileFor(slot).isFile()
                && !save.getProperty("name", "").isBlank()
                && Integer.parseInt(save.getProperty("phase")) >= 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public synchronized boolean write(int slot, Properties properties) {
        File destination = fileFor(slot);
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) return false;

        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(directory.toPath(), "slot-" + (slot + 1) + "-", ".tmp");
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                properties.store(output, "Entre Mitos e Raizes - campanha");
            }
            try {
                Files.move(temporaryFile, destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            cache.remove(slot);
            return true;
        } catch (IOException exception) {
            System.err.println("[Entre Mitos e Raízes] Não foi possível salvar '" + destination + "': " + exception.getMessage());
            return false;
        } finally {
            if (temporaryFile != null) {
                try { Files.deleteIfExists(temporaryFile); }
                catch (IOException exception) {
                    System.err.println("[Entre Mitos e Raízes] Não foi possível remover o temporário do save: " + exception.getMessage());
                }
            }
        }
    }

    public synchronized boolean exists(int slot) { return fileFor(slot).isFile(); }

    public synchronized boolean delete(int slot) {
        File file = fileFor(slot);
        boolean deleted = !file.exists() || file.delete();
        if (deleted) cache.remove(slot);
        return deleted;
    }

    private Properties copy(Properties source) {
        Properties result = new Properties();
        result.putAll(source);
        return result;
    }

    private record CachedSlot(long modified, long length, Properties properties) { }

    private File fileFor(int slot) {
        if (slot < 0 || slot > 1) throw new IllegalArgumentException("Slot inválido: " + slot);
        return new File(directory, "slot-" + (slot + 1) + ".properties");
    }
}
