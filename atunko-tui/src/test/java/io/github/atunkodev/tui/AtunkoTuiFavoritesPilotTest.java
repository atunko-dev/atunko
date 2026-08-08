package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.tui.pilot.Pilot;
import io.github.atunkodev.core.recipe.FavoritesFilter;
import io.github.atunkodev.core.recipe.FavoritesService;
import io.github.atunkodev.core.recipe.RecentRecipesService;
import io.github.reqstool.annotations.SVCs;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
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
            // The favorite toggle persists to disk inside the handler, so the redraw can land after
            // Pilot's fixed post-press pause — await the frame instead of trusting a single pause.
            awaitScreen(tui, "Alpha Recipe *");

            pilot.press('F');
            assertThat(tui.controller().favoritesFilter()).isEqualTo(FavoritesFilter.FAVORITES);
            awaitScreen(tui, "fav:only");
            assertThat(tui.screen()).contains("1 recipes");
            assertThat(tui.screen()).doesNotContain("Beta Recipe");

            pilot.press('F');
            assertThat(tui.controller().favoritesFilter()).isEqualTo(FavoritesFilter.ALL);
            awaitScreen(tui, "fav:all");
            assertThat(tui.screen()).contains("3 recipes");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0007.1"})
    void helpOverlayDocumentsFavoriteKeys(@TempDir Path configDir) throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(controller(configDir))) {
            tui.pilot().press('?');
            assertThat(tui.controller().isShowHelp()).isTrue();
            awaitScreen(tui, "Toggle favorite");
            assertThat(tui.screen()).contains("Favorites filter");
        }
    }

    /** Rendering is asynchronous to the test thread — poll the captured frame until it shows the expected text. */
    private static void awaitScreen(PilotTestSupport tui, String expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline && !tui.screen().contains(expected)) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertThat(tui.screen()).contains(expected);
    }
}
