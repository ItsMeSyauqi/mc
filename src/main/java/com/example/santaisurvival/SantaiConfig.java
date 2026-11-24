package com.example.santaisurvival;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Config sederhana. Edit config/santaisurvival.json lalu restart server. */
public class SantaiConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean homesEnabled = true;
    public int maxHomes = 3;
    public int teleportCooldownSeconds = 5;

    public boolean treeFellerEnabled = true;
    public boolean treeFellerRequireSneak = true;
    public int treeFellerMaxLogs = 128;
    public boolean treeFellerOnlyNaturalTrees = true;

    public boolean oneSleepEnabled = true;
    public boolean sleepClearWeather = true;
    public List<String> sleepMessages = new ArrayList<>(List.of(
            "{player} tidur nyenyak, dan matahari pun langsung kepo ingin lihat.",
            "{player} ngorok sebentar, tiba-tiba sudah pagi. Ajaib!",
            "Alarm {player} berbunyi: ayam virtual sudah berkokok.",
            "{player} mimpi indah, semua orang ikut dapat pagi gratis."
    ));

    public static SantaiConfig load(Path file) {
        SantaiConfig cfg = new SantaiConfig();
        try {
            if (Files.exists(file)) {
                SantaiConfig read = GSON.fromJson(Files.readString(file), SantaiConfig.class);
                if (read != null) cfg = read;
            }
            cfg.maxHomes = Math.max(1, cfg.maxHomes);
            cfg.teleportCooldownSeconds = Math.max(0, cfg.teleportCooldownSeconds);
            cfg.treeFellerMaxLogs = Math.max(1, Math.min(512, cfg.treeFellerMaxLogs));
            if (cfg.sleepMessages == null || cfg.sleepMessages.isEmpty()) {
                cfg.sleepMessages = new SantaiConfig().sleepMessages;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(cfg));
        } catch (Exception e) {
            SantaiSurvival.LOGGER.error("Gagal membaca config, memakai default", e);
        }
        return cfg;
    }
}
