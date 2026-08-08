package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import java.util.List;

/**
 * Application session state set once at startup before the UI launches. Shared by both the TUI and
 * Web UI — holds the list of project entries for the current session. A single-project session is a
 * workspace of size 1.
 */
public final class SessionHolder {

    private static volatile List<ProjectEntry> entries = List.of();
    private static volatile Path workspaceRoot = null;

    /** Directory of a lazily initialised session whose scan has not run yet; {@code null} otherwise. */
    private static volatile Path pendingProjectDir = null;

    /** Scanner to use for the pending scan; {@code null} means detect it from the directory. */
    private static volatile ProjectScanner pendingScanner = null;

    private SessionHolder() {}

    @Requirements({"atunko:CORE_0010", "atunko:WEB_0002.4"})
    public static void initWorkspace(Path root, List<ProjectEntry> projectEntries) {
        clearPendingScan();
        workspaceRoot = root;
        entries = List.copyOf(projectEntries);
    }

    @Requirements({"atunko:CORE_0010"})
    public static void init(List<ProjectEntry> projectEntries) {
        clearPendingScan();
        workspaceRoot = null;
        entries = List.copyOf(projectEntries);
    }

    /** Backward-compat initialiser for single-project startup. */
    @Requirements({"atunko:CORE_0004"})
    public static void init(Path dir, ProjectInfo info) {
        clearPendingScan();
        entries = List.of(new ProjectEntry(dir, info));
    }

    /**
     * Initialises a single-project session without scanning it. The directory is available
     * immediately through {@link #getProjectDir()}, while {@link #getProjectEntries()} stays empty
     * and {@link #getProjectInfo()} stays {@code null} until {@link #ensureScanned()} runs the
     * build-system scan — which is the expensive part and is not needed before the first execution.
     */
    @Requirements({"atunko:CORE_0017"})
    public static void initLazy(Path dir) {
        initLazy(dir, null);
    }

    /**
     * Same as {@link #initLazy(Path)} but with an explicit scanner instead of detecting one from
     * the directory when the scan finally runs.
     */
    @Requirements({"atunko:CORE_0017"})
    public static void initLazy(Path dir, ProjectScanner scanner) {
        workspaceRoot = null;
        entries = List.of();
        pendingScanner = scanner;
        pendingProjectDir = dir;
    }

    /**
     * Runs the deferred scan of a lazily initialised session, at most once. A no-op for eagerly
     * initialised sessions and for sessions already scanned. A failing scan is propagated to the
     * caller and is not memoized, so the session survives it and the next call retries the scan.
     */
    @Requirements({"atunko:CORE_0017", "atunko:CORE_0017.1", "atunko:CORE_0017.2"})
    public static synchronized void ensureScanned() {
        Path dir = pendingProjectDir;
        if (dir == null) {
            return;
        }
        ProjectScanner scanner = pendingScanner != null ? pendingScanner : ProjectScannerFactory.detect(dir);
        ProjectInfo info = scanner.scan(dir);
        entries = List.of(new ProjectEntry(dir, info));
        clearPendingScan();
    }

    private static void clearPendingScan() {
        pendingProjectDir = null;
        pendingScanner = null;
    }

    public static List<ProjectEntry> getProjectEntries() {
        return entries;
    }

    /** Returns the workspace root when launched with {@code --workspace}, otherwise {@code null}. */
    public static Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    /** Backward-compat accessor — returns the first project's directory. */
    @Requirements({"atunko:CORE_0017"})
    public static Path getProjectDir() {
        if (!entries.isEmpty()) {
            return entries.getFirst().projectDir();
        }
        Path pending = pendingProjectDir;
        return pending != null ? pending : Path.of(".");
    }

    /** Backward-compat accessor — returns the first project's info. */
    public static ProjectInfo getProjectInfo() {
        return entries.isEmpty() ? null : entries.getFirst().info();
    }
}
