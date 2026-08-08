package io.github.atunkodev.core.config;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;

/**
 * Resolves atunko's per-user configuration directory following XDG conventions: {@code $XDG_CONFIG_HOME/atunko},
 * falling back to {@code ~/.config/atunko}. The {@value #CONFIG_DIR_PROPERTY} system property overrides the location
 * entirely, which is the hook tests use to keep the real home directory untouched.
 */
public final class ConfigDirs {

    /** System property overriding the config dir location, e.g. for tests. */
    public static final String CONFIG_DIR_PROPERTY = "atunko.config.dir";

    private ConfigDirs() {}

    /** The atunko config directory. Resolved on every call so a property override takes effect immediately. */
    @Requirements({"atunko:CORE_0020.2"})
    public static Path configDir() {
        String override = System.getProperty(CONFIG_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path base =
                (xdg != null && !xdg.isBlank()) ? Path.of(xdg) : Path.of(System.getProperty("user.home"), ".config");
        return base.resolve("atunko");
    }

    /** Directory auto-scanned for user recipe YAML files. */
    @Requirements({"atunko:CORE_0020.2"})
    public static Path recipesDir() {
        return configDir().resolve("recipes");
    }

    /** File persisting the user's favorite recipe names. */
    @Requirements({"atunko:CORE_0021"})
    public static Path favoritesFile() {
        return configDir().resolve("favorites.yml");
    }

    /** File persisting the recently executed recipe names. */
    @Requirements({"atunko:CORE_0022"})
    public static Path recentFile() {
        return configDir().resolve("recent.yml");
    }
}
