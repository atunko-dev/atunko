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

class RecipeOptionsViewTest {

    private static final RecipeOptionInfo VERSION_OPT = new RecipeOptionInfo(
            "targetVersion",
            "String",
            "Target Java version",
            "The version to migrate to",
            "17",
            List.of("11", "17", "21"),
            true);

    private static final RecipeOptionInfo FLAG_OPT =
            new RecipeOptionInfo("addFlag", "boolean", "Add flag", "Whether to add the flag", null, null, false);

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
    void openOptionsResetsIndexToZero() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");
        controller.moveOptionHighlightDown(2);

        controller.openOptions("org.example.Migrate");

        assertThat(controller.focusedOptionIndex()).isEqualTo(0);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.24"})
    void moveOptionHighlightUpWrapsAroundToLast() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");

        controller.moveOptionHighlightUp(2);

        assertThat(controller.focusedOptionIndex()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.25"})
    void booleanCycleNullToTrueToFalseToClear() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.Migrate");

        // null → true
        controller.cycleRecipeOptionBoolean("org.example.Migrate", "addFlag");
        assertThat(controller.getRecipeOptions("org.example.Migrate")).containsEntry("addFlag", Boolean.TRUE);

        // true → false
        controller.cycleRecipeOptionBoolean("org.example.Migrate", "addFlag");
        assertThat(controller.getRecipeOptions("org.example.Migrate")).containsEntry("addFlag", Boolean.FALSE);

        // false → null (cleared)
        controller.cycleRecipeOptionBoolean("org.example.Migrate", "addFlag");
        assertThat(controller.getRecipeOptions("org.example.Migrate")).doesNotContainKey("addFlag");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.25"})
    void parseValueReturnsIntegerForIntType() {
        assertThat(RecipeOptionsView.parseValue("int", "42")).isEqualTo(42);
        assertThat(RecipeOptionsView.parseValue("Integer", "42")).isEqualTo(42);
        assertThat(RecipeOptionsView.parseValue("java.lang.Integer", "42")).isEqualTo(42);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.25"})
    void parseValueReturnsLongForLongType() {
        assertThat(RecipeOptionsView.parseValue("long", "123456789012")).isEqualTo(123456789012L);
        assertThat(RecipeOptionsView.parseValue("Long", "10")).isEqualTo(10L);
        assertThat(RecipeOptionsView.parseValue("java.lang.Long", "10")).isEqualTo(10L);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.25"})
    void parseValueFallsBackToStringForNonNumericInput() {
        assertThat(RecipeOptionsView.parseValue("int", "notANumber")).isEqualTo("notANumber");
        assertThat(RecipeOptionsView.parseValue("String", "hello")).isEqualTo("hello");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.24"})
    void isBooleanTypeMatchesSimpleAndFqnForms() {
        assertThat(RecipeOptionsView.isBooleanType("boolean")).isTrue();
        assertThat(RecipeOptionsView.isBooleanType("Boolean")).isTrue();
        assertThat(RecipeOptionsView.isBooleanType("java.lang.Boolean")).isTrue();
        assertThat(RecipeOptionsView.isBooleanType("String")).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.24"})
    void openOptionsForUnknownRecipeRendersNoOptionsMessage() {
        TuiController controller = new TuiController(List.of(RECIPE_WITH_OPTIONS));
        controller.openOptions("org.example.DoesNotExist");

        Element element = RecipeOptionsView.render(controller);

        assertThat(element).isNotNull();
        assertThat(controller.isShowOptions()).isTrue();
        assertThat(controller.focusedRecipeForOptions()).isEqualTo("org.example.DoesNotExist");
    }
}
