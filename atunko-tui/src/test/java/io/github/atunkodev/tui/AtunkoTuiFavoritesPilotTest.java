package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.tui.pilot.Pilot;
import io.github.atunkodev.core.recipe.FavoritesFilter;
import io.github.atunkodev.core.recipe.FavoritesService;
import io.github.atunkodev.core.recipe.RecentRecipesService;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end coverage of favorites: the {@code f} key marks the highlighted recipe with a {@code *} through the real
 * render/event pipeline and the {@code F} key narrows the browser list to favorites only, updating the status bar.
 */
@SVCs({"atunko:SVC_TUI_0007"})
class AtunkoTuiFavoritesPilotTest {

    private static TuiController controller(Path configDir) {
        return new TuiController(
                PilotTestSupport.RECIPES,
                new FavoritesService(configDir.resolve("favorites.yml")),
                new RecentRecipesService(configDir.resolve("recent.yml")));
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0007", "atunko:SVC_TUI_0007.1"})
    void favoriteKeyMarksRecipeAndFilterKeyNarrowsListToFavorites(@TempDir Path configDir) throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(controller(configDir))) {
            Pilot pilot = tui.pilot();
            assertThat(tui.screen()).contains("fav:all").contains("3 recipes");
            assertThat(tui.screen()).doesNotContain("Alpha Recipe *");

            pilot.press('f');
            assertThat(tui.controller().isFavorite("org.test.Alpha")).isTrue();
            assertThat(tui.screen()).contains("Alpha Recipe *");

            pilot.press('F');
            assertThat(tui.controller().favoritesFilter()).isEqualTo(FavoritesFilter.FAVORITES);
            assertThat(tui.screen()).contains("fav:only").contains("1 recipes");
            assertThat(tui.screen()).doesNotContain("Beta Recipe");

            pilot.press('F');
            assertThat(tui.controller().favoritesFilter()).isEqualTo(FavoritesFilter.ALL);
            assertThat(tui.screen()).contains("fav:all").contains("3 recipes");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0007.1"})
    void helpOverlayDocumentsFavoriteKeys(@TempDir Path configDir) throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(controller(configDir))) {
            tui.pilot().press('?');
            assertThat(tui.controller().isShowHelp()).isTrue();
            assertThat(tui.screen()).contains("Toggle favorite").contains("Favorites filter");
        }
    }
}
