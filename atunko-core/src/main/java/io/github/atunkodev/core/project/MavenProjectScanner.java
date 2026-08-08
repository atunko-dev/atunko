package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MavenProjectScanner implements ProjectScanner {

    private static final int TIMEOUT_MINUTES = 5;

    /** Directory names never searched for module poms — build output and VCS/tooling metadata. */
    private static final Set<String> IGNORED_DIRECTORY_NAMES = Set.of("target", "build", ".git", "node_modules");

    @Requirements({"atunko:CORE_MAVEN_0001"})
    public ProjectInfo scan(Path projectDir) {
        Path absoluteDir = projectDir.toAbsolutePath().normalize();
        Path pomFile = absoluteDir.resolve("pom.xml");

        if (!Files.exists(pomFile)) {
            throw new IllegalArgumentException("No pom.xml found in " + absoluteDir);
        }

        List<Path> classpath = resolveClasspath(absoluteDir);
        List<Path> sourceDirs = resolveSourceDirs(absoluteDir);
        List<Path> poms = resolvePomFiles(absoluteDir);

        return new ProjectInfo(classpath, sourceDirs, List.of(), List.of(), List.of(), poms);
    }

    /**
     * Every {@code pom.xml} of the build — the root one plus any module poms. They live above the source directories,
     * so they are reported separately; {@link ProjectSourceParser} parses them all in one {@code MavenParser} call so
     * that parent/child relationships resolve.
     */
    @Requirements({"atunko:CORE_0016.1"})
    private List<Path> resolvePomFiles(Path projectDir) {
        try (Stream<Path> walk = Files.walk(projectDir)) {
            return walk.filter(p -> "pom.xml".equals(p.getFileName().toString()))
                    .filter(Files::isRegularFile)
                    .filter(p -> !isInIgnoredDirectory(projectDir, p))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            // A pom that cannot be listed simply is not parsed as Maven; scanning must not fail because of it.
            return List.of(projectDir.resolve("pom.xml"));
        }
    }

    private boolean isInIgnoredDirectory(Path projectDir, Path pom) {
        Path relative = projectDir.relativize(pom);
        for (Path segment : relative) {
            if (IGNORED_DIRECTORY_NAMES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private List<Path> resolveClasspath(Path projectDir) {
        try {
            Path outputFile = Files.createTempFile("maven-classpath-", ".txt");
            try {
                String mvn = findMavenExecutable(projectDir);

                ProcessBuilder pb = new ProcessBuilder(
                        mvn,
                        "dependency:build-classpath",
                        "-DincludeScope=compile",
                        "-Dmdep.outputFile=" + outputFile.toAbsolutePath(),
                        "-q",
                        "-B",
                        "-f",
                        projectDir.resolve("pom.xml").toAbsolutePath().toString());

                pb.directory(projectDir.toFile());
                pb.redirectErrorStream(true);

                Process process = pb.start();
                try {
                    process.getInputStream().transferTo(OutputStream.nullOutputStream());
                    boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);

                    if (!finished) {
                        throw new RuntimeException(
                                "Maven dependency:build-classpath timed out after " + TIMEOUT_MINUTES + " minutes");
                    }

                    int exitCode = process.exitValue();
                    if (exitCode != 0) {
                        throw new RuntimeException(
                                "Maven dependency:build-classpath failed with exit code " + exitCode);
                    }

                    String classpathStr = Files.readString(outputFile).strip();
                    if (classpathStr.isEmpty()) {
                        return List.of();
                    }

                    return Arrays.stream(classpathStr.split(System.getProperty("path.separator")))
                            .map(Path::of)
                            .toList();
                } finally {
                    process.destroyForcibly();
                }
            } finally {
                Files.deleteIfExists(outputFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve Maven classpath", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Maven classpath resolution was interrupted", e);
        }
    }

    private List<Path> resolveSourceDirs(Path projectDir) {
        Path srcMainJava = projectDir.resolve("src/main/java");
        if (Files.isDirectory(srcMainJava)) {
            return List.of(srcMainJava);
        }
        return List.of();
    }

    private String findMavenExecutable(Path projectDir) {
        Path wrapper = projectDir.resolve("mvnw");
        if (Files.isExecutable(wrapper)) {
            return wrapper.toAbsolutePath().toString();
        }
        return "mvn";
    }
}
