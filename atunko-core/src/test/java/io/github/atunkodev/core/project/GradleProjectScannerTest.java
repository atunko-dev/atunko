package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GradleProjectScannerTest {

    private final GradleProjectScanner scanner = new GradleProjectScanner();

    // Use the root project — Gradle Tooling API connects to the root (where settings.gradle lives)
    private static final Path ROOT_PROJECT_DIR = Path.of(".");

    @Test
    @SVCs({"SVC_CORE_0004"})
    void scan_returnsNonEmptyClasspath() {
        ProjectInfo info = scanner.scan(ROOT_PROJECT_DIR);

        assertThat(info.classpath()).isNotEmpty();
        assertThat(info.classpath()).anyMatch(path -> path.toString().endsWith(".jar"));
    }

    @Test
    @SVCs({"SVC_CORE_0004"})
    void scan_returnsSourceDirectories() {
        ProjectInfo info = scanner.scan(ROOT_PROJECT_DIR);

        assertThat(info.sourceDirs()).isNotEmpty();
        assertThat(info.sourceDirs()).anyMatch(path -> path.toString().contains("src/main/java"));
    }

    @Test
    @SVCs({"SVC_CORE_0004"})
    void scan_nonExistentDirectory_throws() {
        assertThatThrownBy(() -> scanner.scan(Path.of("/nonexistent/gradle/project")))
                .isInstanceOf(Exception.class);
    }
}
