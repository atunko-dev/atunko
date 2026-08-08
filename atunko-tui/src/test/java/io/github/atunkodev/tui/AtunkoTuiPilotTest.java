package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.pilot.Pilot;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.SVCs;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests driving the real {@link AtunkoTui} headlessly through TamboUI's Pilot API. Unlike the controller
 * and view unit tests, these exercise the full render/event pipeline: key routing, focus, mouse dispatch, and the
 * frames actually rendered to the (virtual) terminal.
 *
 * <p>Right-click interactions are covered at the handler level only ({@code BrowserViewMouseTest}): TamboUI's
 * {@code EventRouter} routes only left-button presses to mouse handlers, so right-click events never reach views
 * through the live pipeline.
 */
@SVCs({"atunko:SVC_TUI_0003"})
class AtunkoTuiPilotTest {

    @Test
    @SVCs({"atunko:SVC_TUI_0003"})
    void headlessLaunchRendersBrowserView() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            assertThat(tui.pilot().hasElement("browser")).isTrue();

            String screen = tui.screen();
            assertThat(screen).contains("Alpha Recipe").contains("Beta Recipe").contains("Gamma Recipe");
            // detail panel shows the highlighted recipe's fully qualified name
            assertThat(screen).contains("org.test.Alpha");
            assertThat(screen).contains("3 recipes | 0 selected");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0003.1"})
    void keyboardNavigationMovesHighlightAndOpensDetail() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            Pilot pilot = tui.pilot();

            pilot.press(KeyCode.DOWN);
            assertThat(tui.controller().highlightedIndex()).isEqualTo(1);
            pilot.press('j');
            assertThat(tui.controller().highlightedIndex()).isEqualTo(2);
            pilot.press('k');
            pilot.press(KeyCode.UP);
            assertThat(tui.controller().highlightedIndex()).isZero();

            pilot.press(KeyCode.ENTER);
            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.DETAIL);
            assertThat(tui.screen()).contains("org.test.Alpha");

            pilot.press(KeyCode.ESCAPE);
            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.BROWSER);
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0003.2"})
    void searchFiltersRecipeListLive() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            Pilot pilot = tui.pilot();

            pilot.press('/');
            assertThat(tui.controller().isSearchMode()).isTrue();
            assertThat(tui.screen()).contains("SEARCH");

            pilot.press("s", "p", "r", "i", "n", "g");
            assertThat(tui.controller().searchQuery()).isEqualTo("spring");
            assertThat(tui.controller().recipes()).extracting(RecipeInfo::name).containsExactly("org.test.Beta");
            assertThat(tui.screen()).contains("Beta Recipe").doesNotContain("Alpha Recipe");

            pilot.press(KeyCode.ENTER);
            assertThat(tui.controller().isSearchMode()).isFalse();
            assertThat(tui.controller().recipes()).hasSize(1);

            pilot.press(KeyCode.ESCAPE);
            assertThat(tui.controller().searchQuery()).isEmpty();
            assertThat(tui.controller().recipes()).hasSize(3);
            assertThat(tui.screen()).contains("Alpha Recipe");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0003.3"})
    void selectionFlowReachesConfirmRunAndBacksOut() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            Pilot pilot = tui.pilot();

            pilot.press(' ');
            assertThat(tui.controller().selectedRecipes()).containsExactly("org.test.Alpha");
            assertThat(tui.screen()).contains("[x]").contains("3 recipes | 1 selected");
            pilot.press(KeyCode.DOWN);
            pilot.press(' ');
            assertThat(tui.controller().selectedRecipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");

            pilot.press('r');
            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.CONFIRM_RUN);
            assertThat(tui.controller().runOrder()).containsExactly("org.test.Alpha", "org.test.Beta");
            assertThat(tui.screen()).contains("Alpha Recipe").contains("Beta Recipe");

            pilot.press(KeyCode.ESCAPE);
            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.BROWSER);
            assertThat(tui.controller().selectedRecipes()).containsExactlyInAnyOrder("org.test.Alpha", "org.test.Beta");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0003.4"})
    void mouseEventsDriveBrowserListThroughEventPipeline() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            tui.dispatch(MouseEvent.scrollDown(10, 10));
            assertThat(tui.controller().highlightedIndex()).isEqualTo(1);
            tui.dispatch(MouseEvent.scrollUp(10, 10));
            assertThat(tui.controller().highlightedIndex()).isZero();

            // header is 3 rows; row y=4 is list index 1
            tui.pilot().click(5, 4);
            assertThat(tui.controller().highlightedIndex()).isEqualTo(1);
            // detail panel follows the mouse highlight
            assertThat(tui.screen()).contains("org.test.Beta");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0003.5"})
    void helpOverlayShowsAndTuiSurvivesResize() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            Pilot pilot = tui.pilot();

            pilot.press('?');
            assertThat(tui.controller().isShowHelp()).isTrue();
            assertThat(tui.screen()).contains("Navigation").contains("Expand all");

            pilot.press(KeyCode.ESCAPE);
            assertThat(tui.controller().isShowHelp()).isFalse();
            assertThat(tui.screen()).doesNotContain("Expand all");

            pilot.resize(80, 24);
            pilot.press(KeyCode.DOWN);
            assertThat(tui.controller().highlightedIndex()).isEqualTo(1);
        }
    }
}
