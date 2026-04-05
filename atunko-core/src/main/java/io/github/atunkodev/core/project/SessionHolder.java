package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;

/**
 * Application session state set once at startup before the UI launches. Shared by both the TUI and
 * Web UI — holds the scanned project directory and its resolved project information.
 */
public final class SessionHolder {

    private static volatile Path projectDir = Path.of(".");
    private static volatile ProjectInfo projectInfo;

    private SessionHolder() {}

    @Requirements({"atunko:CORE_0004"})
    public static void init(Path dir, ProjectInfo info) {
        projectDir = dir;
        projectInfo = info;
    }

    public static Path getProjectDir() {
        return projectDir;
    }

    public static ProjectInfo getProjectInfo() {
        return projectInfo;
    }
}
