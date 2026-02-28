package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MavenProjectScanner {

    private static final int TIMEOUT_MINUTES = 5;

    @Requirements({"CORE_0005"})
    public ProjectInfo scan(Path projectDir) {
        Path absoluteDir = projectDir.toAbsolutePath().normalize();
        Path pomFile = absoluteDir.resolve("pom.xml");

        if (!Files.exists(pomFile)) {
            throw new IllegalArgumentException("No pom.xml found in " + absoluteDir);
        }

        List<Path> classpath = resolveClasspath(absoluteDir);
        List<Path> sourceDirs = resolveSourceDirs(absoluteDir);

        return new ProjectInfo(classpath, sourceDirs);
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
                process.getInputStream().transferTo(OutputStream.nullOutputStream());
                boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);

                if (!finished) {
                    process.destroyForcibly();
                    throw new RuntimeException(
                            "Maven dependency:build-classpath timed out after " + TIMEOUT_MINUTES + " minutes");
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new RuntimeException("Maven dependency:build-classpath failed with exit code " + exitCode);
                }

                String classpathStr = Files.readString(outputFile).strip();
                if (classpathStr.isEmpty()) {
                    return List.of();
                }

                return Arrays.stream(classpathStr.split(System.getProperty("path.separator")))
                        .map(Path::of)
                        .toList();
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
