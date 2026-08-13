package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.tui.shell.TuiShell;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Header composition through the real render pipeline.
 *
 * <p>The shell owns the title, but {@code BrowserView} kept drawing its own pre-shell one as well, so the band read
 * {@code atunko … tabs … atunko … search}. Counting occurrences is what pins that down: asserting the header merely
 * <em>contains</em> the title — which the frame test already does — passes just as happily when it is drawn twice.
 */
class AtunkoTuiHeaderPilotTest {

    @Test
    @SVCs({"atunko:SVC_TUI_0009.8"})
    void browserHeaderShowsTheTitleOnce() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            assertThat(occurrencesInHeader(tui, "atunko"))
                    .as("the shell draws the title; no view may draw its own")
                    .isOne();
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.8"})
    void titleTabsAndHeaderControlShareOneRow() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            List<String> header = headerRows(tui);

            String titleRow = header.stream()
                    .filter(row -> row.contains("atunko"))
                    .findFirst()
                    .orElseThrow();

            assertThat(titleRow)
                    .as("title, tabs and the screen's own header control belong on one row")
                    .contains("Recipes:")
                    .contains("Search recipes");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.8"})
    void tagBrowserHeaderShowsItsTitleOnce() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            tui.pilot().press('t');

            List<String> header = headerRows(tui);
            String titleRow = header.stream()
                    .filter(row -> row.contains("Tags"))
                    .findFirst()
                    .orElseThrow();

            assertThat(occurrencesInHeader(tui, "Filter tags"))
                    .as("the tag filter is drawn once, in the header band")
                    .isOne();
            assertThat(titleRow).as("tab bar shares the title row").contains("Recipes:");
        }
    }

    private static List<String> headerRows(PilotTestSupport tui) {
        return tui.screen().lines().limit(TuiShell.HEADER_HEIGHT).toList();
    }

    private static int occurrencesInHeader(PilotTestSupport tui, String needle) {
        String header = String.join("\n", headerRows(tui));
        int count = 0;
        for (int from = header.indexOf(needle); from >= 0; from = header.indexOf(needle, from + needle.length())) {
            count++;
        }
        return count;
    }
}
