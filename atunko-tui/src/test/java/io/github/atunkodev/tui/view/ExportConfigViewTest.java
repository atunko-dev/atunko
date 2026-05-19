package io.github.atunkodev.tui.view;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.toolkit.element.Element;
import io.github.atunkodev.core.config.ConfigExportService;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_TUI_0001.21"})
class ExportConfigViewTest {

    private static final RecipeInfo ALPHA = new RecipeInfo("org.example.Alpha", "Alpha", "Alpha recipe", Set.of());
    private static final RecipeInfo BETA = new RecipeInfo("org.example.Beta", "Beta", "Beta recipe", Set.of());

    private TuiController controllerWithSelectedRecipes() {
        TuiController controller = new TuiController(List.of(ALPHA, BETA));
        controller.toggleSelection(); // select ALPHA
        controller.moveDown();
        controller.toggleSelection(); // select BETA
        controller.openConfirmRun();
        return controller;
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.21"})
    void rendersGradleSnippetByDefault() {
        TuiController controller = controllerWithSelectedRecipes();

        Element element = ExportConfigView.render(controller);

        assertThat(element).isNotNull();
        assertThat(controller.exportFormat()).isEqualTo(TuiController.ExportFormat.GRADLE);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.22"})
    void setFormatToMavenProducesMavenSnippet() {
        TuiController controller = controllerWithSelectedRecipes();

        controller.setExportFormat(TuiController.ExportFormat.MAVEN);

        assertThat(controller.exportFormat()).isEqualTo(TuiController.ExportFormat.MAVEN);
        Element element = ExportConfigView.render(controller);
        assertThat(element).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.23"})
    void toggleExportModeProducesFullSnippet() {
        TuiController controller = controllerWithSelectedRecipes();

        controller.toggleExportMode();

        assertThat(controller.exportMode()).isEqualTo(ConfigExportService.ExportMode.FULL);
        Element element = ExportConfigView.render(controller);
        assertThat(element).isNotNull();
    }
}
