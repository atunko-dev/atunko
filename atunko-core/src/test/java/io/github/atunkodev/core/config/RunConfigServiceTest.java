package io.github.atunkodev.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunConfigServiceTest {

    private final RunConfigService service = new RunConfigService();

    @TempDir
    Path tempDir;

    @Test
    @SVCs({"atunko:SVC_CORE_0007"})
    void save_writesYamlFile() throws IOException {
        RunConfig config = new RunConfig(List.of(
                new RecipeConfig("org.openrewrite.java.cleanup.RemoveUnusedImports"),
                new RecipeConfig("org.openrewrite.java.format.AutoFormat")));

        Path file = tempDir.resolve(".atunko.yml");
        service.save(config, file);

        assertThat(file).exists();
        String content = Files.readString(file);
        assertThat(content).contains("version:");
        assertThat(content).contains("recipes:");
        assertThat(content).contains("org.openrewrite.java.cleanup.RemoveUnusedImports");
        assertThat(content).contains("org.openrewrite.java.format.AutoFormat");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0007"})
    void save_withEmptyRecipes_writesEmptyList() throws IOException {
        RunConfig config = new RunConfig(List.of());

        Path file = tempDir.resolve(".atunko.yml");
        service.save(config, file);

        assertThat(file).exists();
        String content = Files.readString(file);
        assertThat(content).contains("recipes:");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0007"})
    void save_overwritesExistingFile() throws IOException {
        Path file = tempDir.resolve(".atunko.yml");
        Files.writeString(file, "old content");

        RunConfig config = new RunConfig(List.of(new RecipeConfig("org.openrewrite.java.cleanup.RemoveUnusedImports")));
        service.save(config, file);

        String content = Files.readString(file);
        assertThat(content).doesNotContain("old content");
        assertThat(content).contains("org.openrewrite.java.cleanup.RemoveUnusedImports");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void load_readsYamlFile() throws IOException {
        RunConfig original = new RunConfig(List.of(
                new RecipeConfig("org.openrewrite.java.cleanup.RemoveUnusedImports"),
                new RecipeConfig("org.openrewrite.java.format.AutoFormat")));

        Path file = tempDir.resolve(".atunko.yml");
        service.save(original, file);

        RunConfig loaded = service.load(file);

        assertThat(loaded.recipeNames()).containsExactlyElementsOf(original.recipeNames());
        assertThat(loaded.version()).isEqualTo(RunConfig.CURRENT_VERSION);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void load_nonExistentFile_throws() {
        assertThatThrownBy(() -> service.load(Path.of("/nonexistent/.atunko.yml")))
                .isInstanceOf(IOException.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void load_invalidYaml_throws() throws IOException {
        Path file = tempDir.resolve(".atunko.yml");
        Files.writeString(file, "not: [valid: yaml: for: runconfig");

        assertThatThrownBy(() -> service.load(file)).isInstanceOf(Exception.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void load_yamlWithNullRecipes_throws() throws IOException {
        Path file = tempDir.resolve(".atunko.yml");
        Files.writeString(file, "version: 1\nrecipes:\n");

        assertThatThrownBy(() -> service.load(file)).isInstanceOf(Exception.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0007.1"})
    void save_withOptions_includesOptionValuesInYaml() throws IOException {
        RunConfig config = new RunConfig(List.of(
                new RecipeConfig("org.openrewrite.java.ChangeMethodName", Map.of("newMethodName", "baz")),
                new RecipeConfig("org.openrewrite.java.RemoveUnusedImports")));

        Path file = tempDir.resolve(".atunko.yml");
        service.save(config, file);

        String content = Files.readString(file);
        assertThat(content).contains("newMethodName");
        assertThat(content).contains("baz");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0007.1", "atunko:SVC_CORE_0008.1"})
    void roundTrip_withOptions_preservesOptionValues() throws IOException {
        RecipeConfig withOptions =
                new RecipeConfig("org.openrewrite.java.ChangeMethodName", Map.of("newMethodName", "baz"));
        RecipeConfig withoutOptions = new RecipeConfig("org.openrewrite.java.RemoveUnusedImports");
        RunConfig original = new RunConfig(List.of(withOptions, withoutOptions));

        Path file = tempDir.resolve(".atunko.yml");
        service.save(original, file);
        RunConfig loaded = service.load(file);

        assertThat(loaded.recipes()).hasSize(2);
        assertThat(loaded.recipes().get(0).name()).isEqualTo("org.openrewrite.java.ChangeMethodName");
        assertThat(loaded.recipes().get(0).options()).containsEntry("newMethodName", "baz");
        assertThat(loaded.recipes().get(0).hasOptions()).isTrue();
        assertThat(loaded.recipes().get(1).name()).isEqualTo("org.openrewrite.java.RemoveUnusedImports");
        assertThat(loaded.recipes().get(1).hasOptions()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008.1"})
    void load_yamlWithOptions_parsesOptionValues() throws IOException {
        Path file = tempDir.resolve(".atunko.yml");
        Files.writeString(file, """
            version: 1
            recipes:
              - name: org.openrewrite.java.ChangeMethodName
                options:
                  oldMethodName: foo
                  newMethodName: bar
              - name: org.openrewrite.java.RemoveUnusedImports
            """);

        RunConfig loaded = service.load(file);

        assertThat(loaded.recipes()).hasSize(2);
        assertThat(loaded.recipes().get(0).options()).containsEntry("oldMethodName", "foo");
        assertThat(loaded.recipes().get(0).options()).containsEntry("newMethodName", "bar");
        assertThat(loaded.recipes().get(1).options()).isEmpty();
    }
}
