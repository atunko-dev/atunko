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
    void saveWritesYamlFile() throws IOException {
        RunConfig config = new RunConfig(List.of(
                new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports"),
                new RecipeEntry("org.openrewrite.java.format.AutoFormat")));

        Path file = tempDir.resolve("run.yaml");
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
    void saveWithDescriptionIncludesDescription() throws IOException {
        RunConfig config =
                new RunConfig("Spring Boot migration", List.of(new RecipeEntry("org.openrewrite.java.Boot")));

        Path file = tempDir.resolve("run.yaml");
        service.save(config, file);

        String content = Files.readString(file);
        assertThat(content).contains("description:");
        assertThat(content).contains("Spring Boot migration");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0007"})
    void saveWithOptionsAndExcludeWritesStructuredEntry() throws IOException {
        RecipeEntry entry = new RecipeEntry(
                "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5",
                Map.of("newVersion", "3.5.0"),
                List.of("org.openrewrite.java.spring.boot3.SomeSubRecipe"));
        RunConfig config = new RunConfig("With options", List.of(entry));

        Path file = tempDir.resolve("run.yaml");
        service.save(config, file);

        String content = Files.readString(file);
        assertThat(content).contains("newVersion:");
        assertThat(content).contains("3.5.0");
        assertThat(content).contains("exclude:");
        assertThat(content).contains("org.openrewrite.java.spring.boot3.SomeSubRecipe");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0007"})
    void saveWithEmptyRecipesWritesEmptyList() throws IOException {
        RunConfig config = new RunConfig(List.of());

        Path file = tempDir.resolve("run.yaml");
        service.save(config, file);

        assertThat(file).exists();
        String content = Files.readString(file);
        assertThat(content).contains("recipes:");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0007"})
    void saveOverwritesExistingFile() throws IOException {
        Path file = tempDir.resolve("run.yaml");
        Files.writeString(file, "old content");

        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports")));
        service.save(config, file);

        String content = Files.readString(file);
        assertThat(content).doesNotContain("old content");
        assertThat(content).contains("org.openrewrite.java.cleanup.RemoveUnusedImports");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void loadReadsYamlFile() throws IOException {
        RunConfig original = new RunConfig(
                "Test config",
                List.of(
                        new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports"),
                        new RecipeEntry("org.openrewrite.java.format.AutoFormat")));

        Path file = tempDir.resolve("run.yaml");
        service.save(original, file);

        RunConfig loaded = service.load(file);

        assertThat(loaded.recipes()).hasSize(2);
        assertThat(loaded.recipes().get(0).name()).isEqualTo("org.openrewrite.java.cleanup.RemoveUnusedImports");
        assertThat(loaded.recipes().get(1).name()).isEqualTo("org.openrewrite.java.format.AutoFormat");
        assertThat(loaded.description()).isEqualTo("Test config");
        assertThat(loaded.version()).isEqualTo(RunConfig.CURRENT_VERSION);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void loadWithOptionsAndExcludeRoundTrips() throws IOException {
        RecipeEntry entry = new RecipeEntry(
                "org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5",
                Map.of("newVersion", "3.5.0"),
                List.of("org.openrewrite.java.spring.boot3.SomeSubRecipe"));
        RunConfig original = new RunConfig(List.of(entry));

        Path file = tempDir.resolve("run.yaml");
        service.save(original, file);

        RunConfig loaded = service.load(file);

        assertThat(loaded.recipes()).hasSize(1);
        RecipeEntry loadedEntry = loaded.recipes().get(0);
        assertThat(loadedEntry.name()).isEqualTo("org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5");
        assertThat(loadedEntry.options()).containsEntry("newVersion", "3.5.0");
        assertThat(loadedEntry.exclude()).containsExactly("org.openrewrite.java.spring.boot3.SomeSubRecipe");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void loadNonExistentFileThrows() {
        assertThatThrownBy(() -> service.load(Path.of("/nonexistent/run.yaml"))).isInstanceOf(IOException.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void loadInvalidYamlThrows() throws IOException {
        Path file = tempDir.resolve("run.yaml");
        Files.writeString(file, "not: [valid: yaml: for: runconfig");

        assertThatThrownBy(() -> service.load(file)).isInstanceOf(Exception.class);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0008"})
    void loadMinimalYamlWorks() throws IOException {
        Path file = tempDir.resolve("run.yaml");
        Files.writeString(file, "version: 1\nrecipes:\n- name: org.openrewrite.java.RemoveUnusedImports\n");

        RunConfig loaded = service.load(file);

        assertThat(loaded).isNotNull();
        assertThat(loaded.recipes()).hasSize(1);
        assertThat(loaded.recipes().get(0).name()).isEqualTo("org.openrewrite.java.RemoveUnusedImports");
        assertThat(loaded.recipes().get(0).options()).isNull();
        assertThat(loaded.recipes().get(0).exclude()).isNull();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0012"})
    void configWithoutWorkspaceHasNullWorkspaceField() throws IOException {
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.openrewrite.java.RemoveUnusedImports")));

        Path file = tempDir.resolve("run.yaml");
        service.save(config, file);

        String content = Files.readString(file);
        assertThat(content).doesNotContain("workspace");

        RunConfig loaded = service.load(file);
        assertThat(loaded.workspace()).isNull();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0007.1", "atunko:SVC_CORE_0008.1"})
    void workspaceBlockRoundTrips() throws IOException {
        WorkspaceConfig workspace = new WorkspaceConfig("./services", List.of("**/*"), List.of("**/target/**"));
        RunConfig config =
                new RunConfig(null, workspace, List.of(new RecipeEntry("org.openrewrite.java.RemoveUnusedImports")));

        Path file = tempDir.resolve("run.yaml");
        service.save(config, file);

        String content = Files.readString(file);
        assertThat(content).contains("workspace:");
        assertThat(content).contains("root:");
        assertThat(content).contains("./services");

        RunConfig loaded = service.load(file);
        assertThat(loaded.workspace()).isNotNull();
        assertThat(loaded.workspace().root()).isEqualTo("./services");
        assertThat(loaded.workspace().include()).containsExactly("**/*");
        assertThat(loaded.workspace().exclude()).containsExactly("**/target/**");
    }
}
