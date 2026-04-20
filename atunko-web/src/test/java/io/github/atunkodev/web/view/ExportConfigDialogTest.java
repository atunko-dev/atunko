package io.github.atunkodev.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import io.github.atunkodev.core.config.ConfigExportService.ExportMode;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExportConfigDialogTest {

    private static final RecipeInfo ALPHA =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "First recipe", Set.of("java"));
    private static final RecipeInfo BETA =
            new RecipeInfo("org.test.Beta", "Beta Recipe", "Second recipe", Set.of("spring"));

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.29"})
    void gradleFormatGeneratesActiveRecipeBlock() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA));

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.GRADLE);

        assertThat(dialog.snippetArea.getValue()).contains("rewrite {");
        assertThat(dialog.snippetArea.getValue()).contains("activeRecipe(");
        assertThat(dialog.snippetArea.getValue()).contains("\"org.test.Alpha\"");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.29"})
    void mavenFormatGeneratesPluginBlock() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA));

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.MAVEN);

        assertThat(dialog.snippetArea.getValue()).contains("<groupId>org.openrewrite.maven</groupId>");
        assertThat(dialog.snippetArea.getValue()).contains("<activeRecipes>");
        assertThat(dialog.snippetArea.getValue()).contains("<recipe>org.test.Alpha</recipe>");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.31"})
    void switchingFormatUpdatesSnippetLive() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA));

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.GRADLE);
        String gradleSnippet = dialog.snippetArea.getValue();

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.MAVEN);
        String mavenSnippet = dialog.snippetArea.getValue();

        assertThat(gradleSnippet).contains("rewrite {");
        assertThat(mavenSnippet).contains("<groupId>org.openrewrite.maven</groupId>");
        assertThat(gradleSnippet).isNotEqualTo(mavenSnippet);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.29"})
    void multipleRecipesAllAppearInGradleSnippet() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA, BETA));

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.GRADLE);
        String snippet = dialog.snippetArea.getValue();

        assertThat(snippet).contains("\"org.test.Alpha\"");
        assertThat(snippet).contains("\"org.test.Beta\"");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.30"})
    void emptySelectionDisablesSnippetAreaAndCopyButton() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of());

        assertThat(dialog.snippetArea.isEnabled()).isFalse();
        assertThat(dialog.copyButton.isEnabled()).isFalse();
        assertThat(dialog.formatSelector.isEnabled()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.30"})
    void emptySelectionShowsNoRecipesMessage() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of());

        assertThat(dialog.snippetArea.getValue()).isEqualTo("No recipes selected.");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.29"})
    void nonEmptySelectionSnippetAreaIsEnabledAndNonEmpty() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA));

        assertThat(dialog.snippetArea.isEnabled()).isTrue();
        assertThat(dialog.snippetArea.getValue()).isNotBlank();
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.29"})
    void recipeNamesNotDisplayNamesUsedInSnippet() {
        RecipeInfo recipe = new RecipeInfo("org.foo.BarRecipe", "Bar", "desc", Set.of());
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(recipe));

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.GRADLE);

        assertThat(dialog.snippetArea.getValue()).contains("\"org.foo.BarRecipe\"");
        assertThat(dialog.snippetArea.getValue()).doesNotContain("\"Bar\"");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.29"})
    void defaultFormatIsGradle() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA));

        assertThat(dialog.formatSelector.getValue()).isEqualTo(ExportConfigDialog.ExportFormat.GRADLE);
        assertThat(dialog.snippetArea.getValue()).contains("rewrite {");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.29"})
    void multipleRecipesAllAppearInMavenSnippet() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA, BETA));

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.MAVEN);
        String snippet = dialog.snippetArea.getValue();

        assertThat(snippet).contains("<recipe>org.test.Alpha</recipe>");
        assertThat(snippet).contains("<recipe>org.test.Beta</recipe>");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.32"})
    void switchingModeUpdatesSnippet() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA));

        dialog.modeSelector.setValue(ExportMode.MINIMAL);
        String minimalSnippet = dialog.snippetArea.getValue();

        dialog.modeSelector.setValue(ExportMode.FULL);
        String fullSnippet = dialog.snippetArea.getValue();

        assertThat(minimalSnippet).isNotEqualTo(fullSnippet);
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.33"})
    void fullGradleModeIncludesPluginsBlock() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA));

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.GRADLE);
        dialog.modeSelector.setValue(ExportMode.FULL);

        assertThat(dialog.snippetArea.getValue()).contains("plugins {");
    }

    @Test
    @SVCs({"atunko:SVC_WEB_0001.34"})
    void fullMavenModeIncludesXmlDeclaration() {
        ExportConfigDialog dialog = new ExportConfigDialog(Set.of(ALPHA));

        dialog.formatSelector.setValue(ExportConfigDialog.ExportFormat.MAVEN);
        dialog.modeSelector.setValue(ExportMode.FULL);

        assertThat(dialog.snippetArea.getValue()).contains("<?xml");
    }
}
