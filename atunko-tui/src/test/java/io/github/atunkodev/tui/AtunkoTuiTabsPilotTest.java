package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.tui.shell.TuiShell;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The header tab bar.
 *
 * <p>atunko used to render {@code tabs(NAME, TAGS, RECENT)} — a sort order wearing the tab affordance — while having
 * eight screens and no tab navigation at all. Tabs now name screens and carry live counts; sort moved to the status
 * row beside the other filters.
 */
class AtunkoTuiTabsPilotTest {

    private static String header(PilotTestSupport tui) {
        List<String> rows = tui.screen().lines().toList();
        return String.join("\n", rows.subList(0, TuiShell.HEADER_HEIGHT));
    }

    private static String stateRow(PilotTestSupport tui) {
        List<String> rows = tui.screen().lines().toList();
        return rows.get(rows.size() - TuiShell.FOOTER_HEIGHT);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.3"})
    void tabsNameScreensRatherThanSortOrders() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(header(tui)).contains("Recipes").contains("Tags").contains("Run");

            assertThat(header(tui))
                    .as("sort orders are no longer tabs")
                    .doesNotContain("NAME")
                    .doesNotContain("RECENT");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.3"})
    void tabsCarryLiveCounts() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(header(tui)).as("three fixture recipes").contains("Recipes: 3");
            assertThat(header(tui)).as("nothing selected yet").contains("Run: 0");

            tui.pilot().press(' ');

            assertThat(header(tui)).as("the count follows the selection").contains("Run: 1");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.3"})
    void theActiveScreenIsMarked() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(header(tui))
                    .as("the browser is the active tab on launch")
                    .contains("▸ Recipes");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.3"})
    void sortOrderMovedToTheStatusRow() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(stateRow(tui)).as("sort sits with the other filters now").contains("sort:name");

            tui.pilot().press('s');

            assertThat(stateRow(tui)).contains("sort:tags");
        }
    }
}
