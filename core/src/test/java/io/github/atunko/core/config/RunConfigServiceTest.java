package io.github.atunko.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunConfigServiceTest {

    private final RunConfigService service = new RunConfigService();

    @TempDir
    Path tempDir;

    @Test
    @SVCs({"SVC_CORE_0007"})
    void save_writesYamlFile() throws IOException {
        RunConfig config = new RunConfig(
                List.of("org.openrewrite.java.cleanup.RemoveUnusedImports", "org.openrewrite.java.format.AutoFormat"));

        Path file = tempDir.resolve(".atunko.yml");
        service.save(config, file);

        assertThat(file).exists();
        String content = Files.readString(file);
        assertThat(content).contains("recipes:");
        assertThat(content).contains("org.openrewrite.java.cleanup.RemoveUnusedImports");
        assertThat(content).contains("org.openrewrite.java.format.AutoFormat");
    }

    @Test
    @SVCs({"SVC_CORE_0007"})
    void save_withEmptyRecipes_writesEmptyList() throws IOException {
        RunConfig config = new RunConfig(List.of());

        Path file = tempDir.resolve(".atunko.yml");
        service.save(config, file);

        assertThat(file).exists();
        String content = Files.readString(file);
        assertThat(content).contains("recipes:");
    }

    @Test
    @SVCs({"SVC_CORE_0007"})
    void save_overwritesExistingFile() throws IOException {
        Path file = tempDir.resolve(".atunko.yml");
        Files.writeString(file, "old content");

        RunConfig config = new RunConfig(List.of("org.openrewrite.java.cleanup.RemoveUnusedImports"));
        service.save(config, file);

        String content = Files.readString(file);
        assertThat(content).doesNotContain("old content");
        assertThat(content).contains("org.openrewrite.java.cleanup.RemoveUnusedImports");
    }
}
