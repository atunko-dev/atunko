package io.github.atunkodev.core.project;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceCapabilityHintsTest {

    @Test
    @SVCs({"atunko:SVC_CORE_0016.3"})
    void hintsMavenForADirectoryWithAPomAtItsRoot() {
        Path mavenProject = Path.of("src/test/resources/fixtures/maven-multi-module");

        assertThat(SourceCapabilityHints.forProjectDir(mavenProject)).containsExactly(SourceCapability.MAVEN);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016.3"})
    void hintsNothingForADirectoryWithoutAPom(@TempDir Path dir) {
        assertThat(SourceCapabilityHints.forProjectDir(dir)).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016.3"})
    void neverHintsGradleEvenForAGradleBuild(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("build.gradle"), "plugins { id 'java' }\n");
        Files.writeString(dir.resolve("settings.gradle"), "rootProject.name = 'x'\n");

        assertThat(SourceCapabilityHints.forProjectDir(dir)).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0016.3"})
    void hintsNothingForANullProjectDirectory() {
        assertThat(SourceCapabilityHints.forProjectDir(null)).isEmpty();
    }
}
