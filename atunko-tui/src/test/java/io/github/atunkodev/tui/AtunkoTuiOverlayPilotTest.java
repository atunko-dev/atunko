package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.tui.shell.TuiShell;
import io.github.reqstool.annotations.SVCs;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Overlay behaviour through the real render pipeline.
 *
 * <p>Help used to <em>replace</em> the browser content and then swallow the keystroke that dismissed it, so reading
 * help and pressing {@code r} to run did nothing — you had to press it twice. These assertions pin both fixes.
 */
class AtunkoTuiOverlayPilotTest {

    private static List<String> rows(PilotTestSupport tui) {
        return tui.screen().lines().toList();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.4"})
    void helpDrawsOverTheScreenRatherThanReplacingIt() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            tui.pilot().press('?');

            String screen = tui.screen();
            assertThat(screen).as("the overlay itself").contains("Help");
            assertThat(String.join("\n", rows(tui).subList(0, TuiShell.HEADER_HEIGHT)))
                    .as("the header is still there behind it")
                    .contains("atunko");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.4"})
    void footerShowsTheOverlaysOwnHintsWhileItIsOpen() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(rows(tui).getLast()).contains("quit");

            tui.pilot().press('?');

            assertThat(rows(tui).getLast())
                    .as("while help is open the footer describes help, not the screen underneath")
                    .contains("close help")
                    .doesNotContain("quit");
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.4"})
    void theKeyThatClosesHelpAlsoPerformsItsAction() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            tui.pilot().press('?');
            assertThat(tui.controller().isShowHelp()).isTrue();

            // 't' opens the tag browser. Before this change it only closed the overlay and was then discarded.
            tui.pilot().press('t');

            assertThat(tui.controller().isShowHelp()).as("help closed").isFalse();
            assertThat(tui.controller().currentScreen())
                    .as("and the key did its job in the same press")
                    .isEqualTo(Screen.TAG_BROWSER);
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.4"})
    void questionMarkOnlyTogglesHelpOffAgain() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            tui.pilot().press('?');
            tui.pilot().press('?');

            assertThat(tui.controller().isShowHelp())
                    .as("? is the toggle, so closing is all it should do")
                    .isFalse();
            assertThat(tui.controller().currentScreen()).isEqualTo(Screen.BROWSER);
        }
    }
}
