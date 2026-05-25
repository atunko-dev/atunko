package io.github.atunkodev.tui.view;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.AtunkoTui;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

@SVCs({"atunko:SVC_TUI_0001.27"})
class BrowserViewMouseTest {

    private static final RecipeInfo ALPHA =
            new RecipeInfo("org.test.Alpha", "Alpha Recipe", "First recipe", Set.of("java"));
    private static final RecipeInfo BETA =
            new RecipeInfo("org.test.Beta", "Beta Recipe", "Second recipe", Set.of("spring"));
    private static final RecipeInfo GAMMA =
            new RecipeInfo("org.test.Gamma", "Gamma Recipe", "Third recipe", Set.of("java"));

    private static final List<RecipeInfo> RECIPES = List.of(ALPHA, BETA, GAMMA);

    @Test
    @SVCs({"atunko:SVC_TUI_0001.27"})
    void scrollUpMovesHighlightUp() {
        TuiController controller = new TuiController(RECIPES);
        controller.moveDown();
        assertThat(controller.highlightedIndex()).isEqualTo(1);

        AtunkoTui app = new AtunkoTui(controller);
        Element el = BrowserView.render(controller, app);

        EventResult result = el.handleMouseEvent(MouseEvent.scrollUp(0, 5));

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(controller.highlightedIndex()).isEqualTo(0);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.27"})
    void scrollDownMovesHighlightDown() {
        TuiController controller = new TuiController(RECIPES);

        AtunkoTui app = new AtunkoTui(controller);
        Element el = BrowserView.render(controller, app);

        EventResult result = el.handleMouseEvent(MouseEvent.scrollDown(0, 5));

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(controller.highlightedIndex()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.27"})
    void leftClickSetsHighlightByRow() {
        TuiController controller = new TuiController(RECIPES);

        AtunkoTui app = new AtunkoTui(controller);
        Element el = BrowserView.render(controller, app);

        // header is 3 rows; row y=4 → list index 1 (second recipe)
        EventResult result = el.handleMouseEvent(MouseEvent.press(MouseButton.LEFT, 0, 4));

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(controller.highlightedIndex()).isEqualTo(1);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.27"})
    void rightClickHighlightsAndTogglesSelection() {
        TuiController controller = new TuiController(RECIPES);

        AtunkoTui app = new AtunkoTui(controller);
        Element el = BrowserView.render(controller, app);

        // header is 3 rows; row y=3 → list index 0 (first recipe)
        EventResult result = el.handleMouseEvent(MouseEvent.press(MouseButton.RIGHT, 0, 3));

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(controller.highlightedIndex()).isEqualTo(0);
        assertThat(controller.selectedRecipes()).contains("org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.27"})
    void mouseIgnoredWhenShowHelp() {
        TuiController controller = new TuiController(RECIPES);
        controller.toggleHelp();

        AtunkoTui app = new AtunkoTui(controller);
        Element el = BrowserView.render(controller, app);

        el.handleMouseEvent(MouseEvent.scrollDown(0, 5));

        assertThat(controller.highlightedIndex()).isEqualTo(0);
    }
}
