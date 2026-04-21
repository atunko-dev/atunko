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

    private SessionHolder() {}

    @Requirements({"atunko:CORE_0010", "atunko:WEB_0002.4"})
    public static void initWorkspace(Path root, List<ProjectEntry> projectEntries) {
        workspaceRoot = root;
        entries = List.copyOf(projectEntries);
    }

    @Requirements({"atunko:CORE_0010"})
    public static void init(List<ProjectEntry> projectEntries) {
        workspaceRoot = null;
        entries = List.copyOf(projectEntries);
    }

    /** Backward-compat initialiser for single-project startup. */
    @Requirements({"atunko:CORE_0004"})
    public static void init(Path dir, ProjectInfo info) {
        entries = List.of(new ProjectEntry(dir, info));
    }

    public static List<ProjectEntry> getProjectEntries() {
        return entries;
    }

    /** Returns the workspace root when launched with {@code --workspace}, otherwise {@code null}. */
    public static Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    /** Backward-compat accessor — returns the first project's directory. */
    public static Path getProjectDir() {
        return entries.isEmpty() ? Path.of(".") : entries.getFirst().projectDir();
    }

    /** Backward-compat accessor — returns the first project's info. */
    public static ProjectInfo getProjectInfo() {
        return entries.isEmpty() ? null : entries.getFirst().info();
    }
}
