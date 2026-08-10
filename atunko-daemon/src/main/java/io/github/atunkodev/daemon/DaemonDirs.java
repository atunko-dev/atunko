package io.github.atunkodev.daemon;

import java.nio.file.Path;

/**
 * Resolves the directory holding daemon registry files, following XDG conventions:
 * {@code $XDG_STATE_HOME/atunko/daemons}, falling back to {@code ~/.local/state/atunko/daemons}.
 *
 * <p>State, not config — a registry entry is coordination data for a running process and is meaningless once that
 * process is gone, so it does not belong beside the user's favorites and run configs in
 * {@code io.github.atunkodev.core.config.ConfigDirs}.
 *
 * <p>The {@value #REGISTRY_DIR_PROPERTY} system property overrides the location entirely, which is the hook tests use
 * to keep the developer's real state directory untouched.
 */
public final class DaemonDirs {

    /** System property overriding the registry dir location, e.g. for tests. */
    public static final String REGISTRY_DIR_PROPERTY = "atunko.daemon.registry.dir";

    private DaemonDirs() {}

    /** Resolved on every call so a property override takes effect immediately. */
    public static Path registryDir() {
        String override = System.getProperty(REGISTRY_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        String xdg = System.getenv("XDG_STATE_HOME");
        Path base = (xdg != null && !xdg.isBlank())
                ? Path.of(xdg)
                : Path.of(System.getProperty("user.home"), ".local", "state");
        return base.resolve("atunko").resolve("daemons");
    }
}
