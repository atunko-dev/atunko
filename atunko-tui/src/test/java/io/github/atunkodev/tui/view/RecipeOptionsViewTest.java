package io.github.atunkodev.tui.view;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.toolkit.element.Element;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.RecipeOptionInfo;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_TUI_0001.24", "atunko:SVC_TUI_0001.25"})
class RecipeOptionsViewTest {

    private static final RecipeOptionInfo VERSION_OPT = new RecipeOptionInfo(
            "targetVersion",
            "String",
            "Target Java version",
            "The version to migrate to",
            "17",
            List.of("11", "17", "21"),
            true,
            null);

    private static final RecipeOptionInfo FLAG_OPT =
            new RecipeOptionInfo("addFlag", "boolean", "Add flag", "Whether to add the flag", null, null, false, null);

    private static final RecipeInfo RECIPE_WITH_OPTIONS = new RecipeInfo(
            "org.example.Migrate",
            "Migrate Java",
            "Migration recipe",
            Set.of("java"),
            List.of(),
            List.of(VERSION_OPT, FLAG_OPT));

    private static final RecipeInfo RECIPE_NO_OPTIONS =
            new RecipeInfo("org.example.Simple", "Simple Recipe", "No options", Set.of());

    @Test
    @SVCs({"atunko:SVC_TUI_0001.24"})
    void rendersOverlayForRecipeWithOptions() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");

        Element element = RecipeOptionsView.render(controller);

        assertThat(element).isNotNull();
        assertThat(controller.isShowOptions()).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.24"})
    void rendersNoOptionsMessageForRecipeWithNoOptions() {
        TuiController controller = new TuiController(List.of(RECIPE_NO_OPTIONS));
        controller.openOptions("org.example.Simple");

        Element element = RecipeOptionsView.render(controller);

        assertThat(element).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.24"})
    void optionNavigationUpdatesIndex() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");

        assertThat(controller.focusedOptionIndex()).isEqualTo(0);

        controller.moveOptionHighlightDown(2);
        assertThat(controller.focusedOptionIndex()).isEqualTo(1);

        controller.moveOptionHighlightDown(2);
        assertThat(controller.focusedOptionIndex()).isEqualTo(0);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.25"})
    void setOptionValueStoredOnController() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");

        controller.setRecipeOption("org.example.Migrate", "targetVersion", "17");

        assertThat(controller.getRecipeOptions("org.example.Migrate")).containsEntry("targetVersion", "17");
        Element element = RecipeOptionsView.render(controller);
        assertThat(element).isNotNull();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.25"})
    void clearOptionRemovesFromController() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");
        controller.setRecipeOption("org.example.Migrate", "targetVersion", "17");

        controller.clearRecipeOption("org.example.Migrate", "targetVersion");

        assertThat(controller.getRecipeOptions("org.example.Migrate")).doesNotContainKey("targetVersion");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.24"})
    void closeOptionsHidesOverlay() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");

        controller.closeOptions();

        assertThat(controller.isShowOptions()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.24"})
    void openOptionsResetsIndexToZero() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");
        controller.moveOptionHighlightDown(2);

        controller.openOptions("org.example.Migrate");

        assertThat(controller.focusedOptionIndex()).isEqualTo(0);
    }
}
