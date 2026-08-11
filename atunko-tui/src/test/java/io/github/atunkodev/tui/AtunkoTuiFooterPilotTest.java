package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.tui.shell.TuiShell;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The footer, through the real render pipeline.
 *
 * <p>The old footer was one row concatenating counts and key hints into a single uniformly styled sentence, so no key
 * was findable at a glance. These assertions pin the two properties that fixes: state and hints occupy separate rows,
 * and the key is emphasised while its label is not.
 */
class AtunkoTuiFooterPilotTest {

    private static int stateRow(PilotTestSupport tui) {
        return tui.rowCount() - TuiShell.FOOTER_HEIGHT;
    }

    private static int hintRow(PilotTestSupport tui) {
        return tui.rowCount() - 1;
    }

    private static String row(PilotTestSupport tui, int index) {
        return tui.screen().lines().toList().get(index);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.2"})
    void stateAndHintsOccupySeparateRows() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(row(tui, stateRow(tui)))
                    .as("state row carries counts and filters")
                    .contains("3 recipes")
                    .contains("0 selected")
                    .contains("src:")
                    .contains("fav:");

            assertThat(row(tui, hintRow(tui)))
                    .as("hint row carries keys")
                    .contains("run")
                    .contains("help")
                    .contains("quit");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.2"})
    void hintKeysAreEmphasisedAndLabelsAreNot() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            List<String> bold = tui.boldRunsOnRow(hintRow(tui));

            assertThat(bold)
                    .as("each hint key is emphasised")
                    .contains("r")
                    .contains("q")
                    .contains("?");
            assertThat(bold)
                    .as("labels must stay unemphasised, otherwise the row is uniform again")
                    .doesNotContain("run")
                    .doesNotContain("quit")
                    .doesNotContain("help");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.2"})
    void hintsChangeWithTheActiveMode() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(row(tui, hintRow(tui))).contains("search");

            tui.pilot().press('/');

            assertThat(row(tui, hintRow(tui)))
                    .as("search mode names what Enter and Esc do there, not the browse-mode keys")
                    .contains("apply")
                    .contains("clear")
                    .doesNotContain("quit");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.2"})
    void stateRowNeverCarriesKeyHints() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(row(tui, stateRow(tui)))
                    .as("this is what the old single-row footer got wrong")
                    .doesNotContain("?:help")
                    .doesNotContain("q:quit")
                    .doesNotContain("o:options");
        }
    }
}
