package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_CORE_0004"})
class ProjectScannerFactoryTest {

    @Test
    @SVCs({"atunko:SVC_CORE_0004.1"})
    void detect_settingsGradle_returnsGradleScanner(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("settings.gradle"));
        assertThat(ProjectScannerFactory.detect(dir)).isInstanceOf(GradleProjectScanner.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.1"})
    void detect_buildGradleKts_returnsGradleScanner(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("build.gradle.kts"));
        assertThat(ProjectScannerFactory.detect(dir)).isInstanceOf(GradleProjectScanner.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.1"})
    void detect_settingsGradleKts_returnsGradleScanner(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("settings.gradle.kts"));
        assertThat(ProjectScannerFactory.detect(dir)).isInstanceOf(GradleProjectScanner.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.2"})
    void detect_pomXml_returnsMavenScanner(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve("pom.xml"));
        assertThat(ProjectScannerFactory.detect(dir)).isInstanceOf(MavenProjectScanner.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0004.3"})
    void detect_noBuildFiles_throwsIllegalArgument(@TempDir Path dir) {
        assertThatThrownBy(() -> ProjectScannerFactory.detect(dir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No build files found");
    }
}
