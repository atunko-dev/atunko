package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MavenProjectScannerTest {

    private final MavenProjectScanner scanner = new MavenProjectScanner();

    private static final Path MAVEN_PROJECT_DIR = Path.of("src/test/resources/fixtures/maven-project");

    @Test
    @SVCs({"atunko:SVC_CORE_0005"})
    void scan_returnsNonEmptyClasspath() {
        ProjectInfo info = scanner.scan(MAVEN_PROJECT_DIR);

        assertThat(info.classpath()).isNotEmpty();
        assertThat(info.classpath()).anyMatch(path -> path.toString().endsWith(".jar"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0005"})
    void scan_returnsSourceDirectories() {
        ProjectInfo info = scanner.scan(MAVEN_PROJECT_DIR);

        assertThat(info.sourceDirs()).isNotEmpty();
        assertThat(info.sourceDirs()).anyMatch(path -> path.toString().contains("src/main/java"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0005"})
    void scan_nonExistentDirectory_throws() {
        assertThatThrownBy(() -> scanner.scan(Path.of("/nonexistent/maven/project")))
                .isInstanceOf(Exception.class);
    }
}
