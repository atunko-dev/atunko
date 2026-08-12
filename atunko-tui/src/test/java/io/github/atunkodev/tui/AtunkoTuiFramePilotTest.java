package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.tui.shell.TuiShell;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Frame geometry through the real render pipeline.
 *
 * <p>The frame used to be improvised per screen — 3 header rows in the browser, 1 in most screens, computed in the
 * detail view — so navigating shifted the content region under the user. These assertions pin the geometry that
 * {@link TuiShell} now owns.
 *
 * <p>Cross-screen equality is asserted once every screen is migrated (change task 8.x); until then the unmigrated
 * screens still render their own frames and comparing them would only restate work that has not happened yet.
 */
class AtunkoTuiFramePilotTest {

    @Test
    @SVCs({"atunko:SVC_TUI_0009.1"})
    void browserHeaderAndFooterOccupyTheShellsFixedRows() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            List<String> rows = tui.screen().lines().toList();

            assertThat(rows.size()).as("frame must fill the terminal").isGreaterThan(10);

            String header = String.join("\n", rows.subList(0, TuiShell.HEADER_HEIGHT));
            assertThat(header).as("title belongs in the header band").contains("atunko");

            List<String> footer = rows.subList(rows.size() - TuiShell.FOOTER_HEIGHT, rows.size());
            assertThat(footer.get(0)).as("state row").contains("recipes").contains("selected");
            assertThat(footer.get(1)).as("key-hint row, separate from state").contains("quit");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.2"})
    void stateAndKeyHintsAreOnSeparateRows() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            List<String> rows = tui.screen().lines().toList();
            List<String> footer = rows.subList(rows.size() - TuiShell.FOOTER_HEIGHT, rows.size());

            assertThat(footer.get(0))
                    .as("the state row must not carry key hints — that was the old single-row footer")
                    .doesNotContain("quit");
            assertThat(footer.get(1)).as("hints row names keys").contains("help");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.4"})
    void helpOverlayLeavesTheFrameInPlace() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            tui.pilot().press('?');

            List<String> rows = tui.screen().lines().toList();

            assertThat(String.join("\n", rows.subList(0, TuiShell.HEADER_HEIGHT)))
                    .as("header survives an open overlay")
                    .contains("atunko");
            assertThat(rows.get(rows.size() - 1))
                    .as("footer shows the overlay's own hints while it is open")
                    .contains("close help");
        }
    }
}
