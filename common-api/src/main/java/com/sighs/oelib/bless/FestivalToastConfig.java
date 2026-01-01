package com.sighs.oelib.bless;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sighs.oelib.OELib;
import com.sighs.oelib.bless.render.ChongYangOverlay;
import com.sighs.oelib.bless.render.NewYearOverlay;
import com.sighs.oelib.platform.Platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public final class FestivalToastConfig {
    private static final String FILE_NAME = "oelib_festivals_blessing.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static FestivalToastConfig instance;

    public boolean enabled = true;
    public boolean chineseFestivalsOnlyForChineseLanguage = true;
    public Map<String, FestivalPreferences> festivals = new HashMap<>();

    private FestivalToastConfig() {
    }

    public static FestivalToastConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static void save() {
        if (instance == null) {
            return;
        }
        var configDir = Platform.getConfigPath();
        var file = configDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            OELib.LOGGER.error("Failed to save festival toast config", e);
        }
    }

    private static FestivalToastConfig load() {
        var configDir = Platform.getConfigPath();
        var file = configDir.resolve(FILE_NAME);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                var loaded = GSON.fromJson(reader, FestivalToastConfig.class);
                if (loaded != null) {
                    loaded.ensureDefaults();
                    return loaded;
                }
            } catch (IOException | JsonSyntaxException e) {
                OELib.LOGGER.error("Failed to load festival toast config, using defaults", e);
            }
        }
        FestivalToastConfig config = new FestivalToastConfig();
        config.ensureDefaults();
        return config;
    }

    public FestivalPreferences festival(String id) {
        var preferences = festivals.get(id);
        if (preferences == null) {
            preferences = new FestivalPreferences();
            festivals.put(id, preferences);
        }
        return preferences;
    }

    public boolean hasShownToday(String festivalId, LocalDate today) {
        var preferences = festival(festivalId);
        if (!preferences.enabled) {
            return true;
        }
        if (preferences.lastShownDate == null) {
            return false;
        }
        return today.toString().equals(preferences.lastShownDate);
    }

    public void markShownToday(String festivalId, LocalDate today) {
        var preferences = festival(festivalId);
        preferences.lastShownDate = today.toString();
    }

    private void ensureDefaults() {
        if (festivals == null) {
            festivals = new HashMap<>();
        }
        festival(NewYearOverlay.FESTIVAL_ID);
        festival(ChongYangOverlay.FESTIVAL_ID);
    }

    public static final class FestivalPreferences {
        public boolean enabled = true;
        public String lastShownDate;
    }
}

