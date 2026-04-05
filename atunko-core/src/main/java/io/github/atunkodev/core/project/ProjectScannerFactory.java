package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Files;
import java.nio.file.Path;

/** Auto-detects the build system in use and returns the appropriate {@link ProjectScanner}. */
public final class ProjectScannerFactory {

    private ProjectScannerFactory() {}

    @Requirements({"atunko:CORE_0004"})
    public static ProjectScanner detect(Path projectDir) {
        Path abs = projectDir.toAbsolutePath().normalize();
        if (Files.exists(abs.resolve("pom.xml"))) {
            return new MavenProjectScanner();
        }
        if (Files.exists(abs.resolve("settings.gradle"))
                || Files.exists(abs.resolve("settings.gradle.kts"))
                || Files.exists(abs.resolve("build.gradle"))
                || Files.exists(abs.resolve("build.gradle.kts"))) {
            return new GradleProjectScanner();
        }
        throw new IllegalArgumentException(
                "No build files found in " + abs + ": expected pom.xml or Gradle build files");
    }
}
