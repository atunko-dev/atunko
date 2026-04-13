package io.github.atunkodev.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.config.ConfigExportService.ExportMode;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigExportServiceTest {

    private final ConfigExportService service = new ConfigExportService();

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void exportToGradleGeneratesActiveRecipeBlock() {
        RunConfig config = new RunConfig(List.of(
                new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports"),
                new RecipeEntry("org.openrewrite.java.format.AutoFormat")));

        String output = service.exportToGradle(config);

        assertThat(output).contains("rewrite {");
        assertThat(output).contains("activeRecipe(");
        assertThat(output).contains("\"org.openrewrite.java.cleanup.RemoveUnusedImports\"");
        assertThat(output).contains("\"org.openrewrite.java.format.AutoFormat\"");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void exportToMavenGeneratesPluginBlock() {
        RunConfig config = new RunConfig(List.of(
                new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports"),
                new RecipeEntry("org.openrewrite.java.format.AutoFormat")));

        String output = service.exportToMaven(config);

        assertThat(output).contains("<groupId>org.openrewrite.maven</groupId>");
        assertThat(output).contains("<artifactId>rewrite-maven-plugin</artifactId>");
        assertThat(output).contains("<activeRecipes>");
        assertThat(output).contains("<recipe>org.openrewrite.java.cleanup.RemoveUnusedImports</recipe>");
        assertThat(output).contains("<recipe>org.openrewrite.java.format.AutoFormat</recipe>");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void exportToGradleWithOptionsIncludesRecipeName() {
        RunConfig config = new RunConfig(List.of(
                new RecipeEntry("org.openrewrite.java.ChangeMethodName", Map.of("methodPattern", "foo"), null)));

        String output = service.exportToGradle(config);

        assertThat(output).contains("\"org.openrewrite.java.ChangeMethodName\"");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void exportToMavenWithSingleRecipeHasNoTrailingComma() {
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports")));

        String output = service.exportToMaven(config);

        assertThat(output).doesNotContain(",");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.33"})
    void fullGradleIncludesPluginsRepositoriesAndRewriteBlocks() {
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports")));

        String output = service.exportToGradle(config, ExportMode.FULL);

        assertThat(output).contains("plugins {");
        assertThat(output).contains("repositories {");
        assertThat(output).contains("rewrite {");
        assertThat(output).contains("activeRecipe(");
        assertThat(output).contains("\"org.openrewrite.java.cleanup.RemoveUnusedImports\"");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.34"})
    void fullMavenIncludesXmlDeclarationAndAtunkoGroupId() {
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports")));

        String output = service.exportToMaven(config, ExportMode.FULL);

        assertThat(output).contains("<?xml");
        assertThat(output).contains("<groupId>io.github.atunkodev</groupId>");
        assertThat(output).contains("<recipe>org.openrewrite.java.cleanup.RemoveUnusedImports</recipe>");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void noArgGradleMethodReturnsMinimalSnippet() {
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports")));

        String output = service.exportToGradle(config);

        assertThat(output).doesNotContain("plugins {");
        assertThat(output).doesNotContain("repositories {");
        assertThat(output).contains("rewrite {");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0009"})
    void noArgMavenMethodReturnsMinimalSnippet() {
        RunConfig config = new RunConfig(List.of(new RecipeEntry("org.openrewrite.java.cleanup.RemoveUnusedImports")));

        String output = service.exportToMaven(config);

        assertThat(output).doesNotContain("<?xml");
        assertThat(output).doesNotContain("<groupId>io.github.atunkodev</groupId>");
        assertThat(output).contains("<groupId>org.openrewrite.maven</groupId>");
    }
}
