package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import io.github.atunkodev.tui.shell.TuiShell;
import io.github.reqstool.annotations.SVCs;
import org.junit.jupiter.api.Test;

/**
 * Focus traversal through the real render pipeline.
 *
 * <p>Every screen used to mark only its root column focusable, so {@code Tab} did nothing anywhere and the browser's
 * two-pane layout had no focus model at all.
 */
class AtunkoTuiFocusPilotTest {

    @Test
    @SVCs({"atunko:SVC_TUI_0009.5"})
    void bothBrowserRegionsAreInTheFocusOrder() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            assertThat(tui.focusOrder())
                    .as("the list and the details pane must both be reachable by Tab")
                    .contains(TuiShell.CONTENT_REGION, TuiShell.DETAILS_REGION);
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.5"})
    void tabMovesFocusBetweenRegionsAndShiftTabComesBack() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            String first = tui.focusedId();

            tui.pilot().press(KeyCode.TAB);

            assertThat(tui.focusedId())
                    .as("Tab must move focus")
                    .isNotEqualTo(first)
                    .isIn(TuiShell.CONTENT_REGION, TuiShell.DETAILS_REGION);

            // No BACK_TAB key code exists; Shift-Tab is TAB carrying the shift modifier.
            tui.dispatch(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT));

            assertThat(tui.focusedId()).as("Shift-Tab must come back").isEqualTo(first);
        }
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0009.5"})
    void movingFocusDoesNotBreakListNavigation() throws Exception {
        // The regression this replaces: marking regions .focusable() routed keys to the focused element, which had
        // no handler, so the arrow keys stopped moving the highlight entirely.
        try (PilotTestSupport tui = PilotTestSupport.launch(new TuiController(PilotTestSupport.RECIPES))) {
            tui.pilot().press(KeyCode.TAB);
            tui.pilot().press(KeyCode.DOWN);

            assertThat(tui.controller().highlightedIndex())
                    .as("arrows must keep working regardless of which region is focused")
                    .isEqualTo(1);
        }
    }
}
