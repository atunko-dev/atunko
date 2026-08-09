package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.tui.pilot.Pilot;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.RecipeSource;
import io.github.atunkodev.core.recipe.RecipeSourceFilter;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage of the recipe source toggle: the {@code u} key cycles the filter All → Bundled → User through
 * the real render/event pipeline, narrowing the browser list and updating the status bar.
 */
@SVCs({"atunko:SVC_TUI_0006"})
class AtunkoTuiSourceTogglePilotTest {

    private static final RecipeInfo USER_RECIPE = new RecipeInfo(
            "org.test.UserRecipe",
            "User Recipe",
            "A user-supplied recipe",
            Set.of("user-test"),
            List.of(),
            List.of(),
            RecipeSource.USER);

    private static final List<RecipeInfo> MIXED_RECIPES =
            List.of(PilotTestSupport.ALPHA, PilotTestSupport.BETA, USER_RECIPE);

    @Test
    @SVCs({"atunko:SVC_TUI_0006", "atunko:SVC_TUI_0006.1"})
    void sourceToggleKeyCyclesFilterNarrowsListAndUpdatesStatusBar() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(MIXED_RECIPES)) {
            Pilot pilot = tui.pilot();
            assertThat(tui.screen()).contains("src:all").contains("3 recipes");

            pilot.press('u');
            assertThat(tui.controller().sourceFilter()).isEqualTo(RecipeSourceFilter.BUNDLED);
            assertThat(tui.screen()).contains("src:bundled").contains("2 recipes");
            assertThat(tui.screen()).doesNotContain("User Recipe");

            pilot.press('u');
            assertThat(tui.controller().sourceFilter()).isEqualTo(RecipeSourceFilter.USER);
            assertThat(tui.screen()).contains("src:user").contains("1 recipes");
            assertThat(tui.screen()).contains("User Recipe").doesNotContain("Alpha Recipe");

            pilot.press('u');
            assertThat(tui.controller().sourceFilter()).isEqualTo(RecipeSourceFilter.ALL);
            assertThat(tui.screen()).contains("src:all").contains("3 recipes");
            assertThat(tui.screen()).contains("Alpha Recipe").contains("User Recipe");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0006.1"})
    void helpOverlayDocumentsSourceToggleKey() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(MIXED_RECIPES)) {
            tui.pilot().press('?');
            assertThat(tui.controller().isShowHelp()).isTrue();
            assertThat(tui.screen()).contains("Recipe source");
        }
    }
}
