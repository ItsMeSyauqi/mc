package com.example.santaisurvival;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/** Menyimpan home per pemain (JSON di folder world) dan cooldown teleport (in-memory). */
public class HomeManager {
    public static final class Home {
        public String dimension;
        public double x, y, z;
        public float yaw, pitch;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, Map<String, Home>>>() {}.getType();

    private static Map<String, Map<String, Home>> data = new HashMap<>();
    private static final Map<UUID, Long> lastTeleport = new HashMap<>();
    private static Path file;

    public static synchronized void load(Path path) {
        file = path;
        try {
            if (Files.exists(path)) {
                Map<String, Map<String, Home>> read = GSON.fromJson(Files.readString(path), TYPE);
                if (read != null) data = read;
            }
        } catch (Exception e) {
            SantaiSurvival.LOGGER.error("Gagal memuat data home", e);
        }
    }

    public static synchronized void save() {
        if (file == null) return;
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(data));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            SantaiSurvival.LOGGER.error("Gagal menyimpan data home", e);
        }
    }

    private static Map<String, Home> homesOf(UUID id) {
        return data.computeIfAbsent(id.toString(), k -> new LinkedHashMap<>());
    }

    public static synchronized Home get(UUID id, String name) { return homesOf(id).get(name); }

    public static synchronized List<String> names(UUID id) { return new ArrayList<>(homesOf(id).keySet()); }

    public static synchronized int count(UUID id) { return homesOf(id).size(); }

    public static synchronized void set(UUID id, String name, Home home) {
        homesOf(id).put(name, home);
        save();
    }

    public static synchronized boolean remove(UUID id, String name) {
        boolean removed = homesOf(id).remove(name) != null;
        if (removed) save();
        return removed;
    }

    public static synchronized long cooldownLeftMs(UUID id, int cooldownSeconds) {
        Long last = lastTeleport.get(id);
        if (last == null) return 0;
        return Math.max(0, cooldownSeconds * 1000L - (System.currentTimeMillis() - last));
    }

    public static synchronized void markTeleport(UUID id) { lastTeleport.put(id, System.currentTimeMillis()); }

    public static synchronized void clearCooldown(UUID id) { lastTeleport.remove(id); }
}
