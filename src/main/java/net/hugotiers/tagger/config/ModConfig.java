package net.hugotiers.tagger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("hugotiers-tagger");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hugotiers-tagger.json";

    public static final String DEFAULT_API_BASE_URL = "https://hugotiers.net";
    /** The only display modes that can ever produce a badge. */
    public static final java.util.List<String> MODES =
            java.util.List.of("all", "cavekit", "crystalfight", "macefight");

    public boolean enabled = true;
    public String gamemode = "all";
    // Written on the render thread (settings screen), read on the "hugotiers-tier-fetch" thread —
    // volatile so the fetcher sees a change instead of a stale or torn value.
    public volatile String apiBaseUrl = DEFAULT_API_BASE_URL;
    public volatile long cacheTtlSeconds = 300;
    public boolean rainbow = true;

    public static ModConfig load() {
        ModConfig defaults = new ModConfig();
        try {
            Path path = configPath();
            if (Files.notExists(path)) {
                defaults.save();
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) {
                    config.sanitize();
                    return config;
                }
                LOGGER.warn("Config file was empty; using defaults.");
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not load config; using defaults.", exception);
        }

        defaults.save();
        return defaults;
    }

    /**
     * Repairs values that could never work, so a bad config cannot leave the mod permanently
     * invisible with no explanation. The old chat command accepted any word as a display mode, so
     * a typo like "mace" would silently hide every badge forever.
     *
     * <p>A VALID single-kit mode is deliberately left alone — showing nothing for players without a
     * rank in that kit is what the user asked for — but it is logged, because it is by far the most
     * likely reason someone reports "I see no tiers at all".
     */
    private void sanitize() {
        if (gamemode == null || !MODES.contains(gamemode)) {
            LOGGER.warn("Unknown display mode '{}' in config; falling back to 'all'.", gamemode);
            gamemode = "all";
        } else if (!gamemode.equals("all")) {
            LOGGER.info("Display mode is '{}': players without a rank in that kit show no badge. "
                    + "Set it to 'all' in the settings screen to see every kit.", gamemode);
        }
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = DEFAULT_API_BASE_URL;
        }
        if (cacheTtlSeconds < 10 || cacheTtlSeconds > 86_400) {
            cacheTtlSeconds = 300;
        }
    }

    public void save() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not save config.", exception);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
