package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers Gradle and Maven projects under a root directory. Walks the tree top-down, skipping
 * build output directories, hidden directories, and directories marked with {@code .atunkoignore}.
 * Gradle multi-project roots and Maven multi-module aggregators are treated as single projects;
 * their subproject/module directories are excluded from independent scanning.
 */
@Requirements({"atunko:CORE_0010"})
public final class WorkspaceScanner {

    private static final Set<String> SKIP_DIR_NAMES = Set.of("build", "target", ".gradle", "node_modules", ".git");

    private static final Pattern GRADLE_INCLUDE_PATTERN = Pattern.compile("""
        include\\s*[\\(]?\\s*["':]+([\\w:/-]+)["']+\
        """);

    private WorkspaceScanner() {}

    /**
     * Scans {@code root} for projects and returns a {@link Workspace} containing all discovered
     * {@link ProjectEntry} instances.
     */
    @Requirements({"atunko:CORE_0010"})
    public static Workspace scan(Path root) {
        Path absRoot = root.toAbsolutePath().normalize();
        List<Path> candidates = discoverProjectDirs(absRoot);
        List<ProjectEntry> projects = new ArrayList<>();
        for (Path candidate : candidates) {
            addProject(candidate, projects);
        }
        return new Workspace(absRoot, projects);
    }

    /**
     * Returns the candidate project directories under {@code root} without scanning them.
     * Useful when only discovery (not full project scanning) is needed.
     */
    public static List<Path> discoverProjectDirs(Path root) {
        List<Path> candidates = new ArrayList<>();
        Set<Path> claimedDirs = new HashSet<>();
        walkCandidates(root, root, claimedDirs, candidates);
        return List.copyOf(candidates);
    }

    private static void walkCandidates(Path dir, Path root, Set<Path> claimedDirs, List<Path> candidates) {
        if (shouldSkip(dir, root, claimedDirs)) {
            return;
        }

        // Gradle root with settings.gradle[.kts]
        Path settingsGradle = dir.resolve("settings.gradle");
        Path settingsGradleKts = dir.resolve("settings.gradle.kts");
        if (Files.exists(settingsGradle) || Files.exists(settingsGradleKts)) {
            Path settingsFile = Files.exists(settingsGradle) ? settingsGradle : settingsGradleKts;
            claimGradleSubprojects(dir, settingsFile, claimedDirs);
            candidates.add(dir);
            descendCandidates(dir, root, claimedDirs, candidates);
            return;
        }

        // Maven pom.xml
        Path pomXml = dir.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            if (isMavenMultiModule(pomXml)) {
                claimMavenModules(dir, pomXml, claimedDirs);
            }
            candidates.add(dir);
            descendCandidates(dir, root, claimedDirs, candidates);
            return;
        }

        // Standalone Gradle project (build.gradle[.kts] only, no settings.gradle)
        if (Files.exists(dir.resolve("build.gradle")) || Files.exists(dir.resolve("build.gradle.kts"))) {
            candidates.add(dir);
            descendCandidates(dir, root, claimedDirs, candidates);
            return;
        }

        // No build file — keep descending
        descendCandidates(dir, root, claimedDirs, candidates);
    }

    private static void descendCandidates(Path dir, Path root, Set<Path> claimedDirs, List<Path> candidates) {
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isDirectory)
                    .sorted()
                    .forEach(child -> walkCandidates(child, root, claimedDirs, candidates));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Requirements({"atunko:CORE_0010.1", "atunko:CORE_0010.2"})
    private static boolean shouldSkip(Path dir, Path root, Set<Path> claimedDirs) {
        if (claimedDirs.contains(dir)) {
            return true;
        }
        String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
        if (SKIP_DIR_NAMES.contains(name)) {
            return true;
        }
        if (!dir.equals(root) && name.startsWith(".")) {
            return true;
        }
        if (Files.exists(dir.resolve(".atunkoignore"))) {
            return true;
        }
        return false;
    }

    @Requirements({"atunko:CORE_0010.3"})
    private static void claimGradleSubprojects(Path rootDir, Path settingsFile, Set<Path> claimedDirs) {
        try {
            String content = Files.readString(settingsFile);
            Matcher matcher = GRADLE_INCLUDE_PATTERN.matcher(content);
            while (matcher.find()) {
                String projectPath = matcher.group(1).replace(':', '/').replaceAll("^/", "");
                Path subprojectDir = rootDir.resolve(projectPath).normalize();
                claimedDirs.add(subprojectDir);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Requirements({"atunko:CORE_0010.4"})
    private static boolean isMavenMultiModule(Path pomXml) {
        try {
            String content = Files.readString(pomXml);
            return content.contains("<modules>");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Requirements({"atunko:CORE_0010.4"})
    private static void claimMavenModules(Path rootDir, Path pomXml, Set<Path> claimedDirs) {
        try {
            String content = Files.readString(pomXml);
            Pattern modulePattern = Pattern.compile("<module>([^<]+)</module>");
            Matcher matcher = modulePattern.matcher(content);
            while (matcher.find()) {
                Path moduleDir = rootDir.resolve(matcher.group(1).trim()).normalize();
                claimedDirs.add(moduleDir);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void addProject(Path dir, List<ProjectEntry> projects) {
        try {
            ProjectScanner scanner = ProjectScannerFactory.detect(dir);
            ProjectInfo info = scanner.scan(dir);
            projects.add(new ProjectEntry(dir, info));
        } catch (Exception e) {
            // Skip directories that cannot be scanned (e.g. incomplete build files)
        }
    }
}
