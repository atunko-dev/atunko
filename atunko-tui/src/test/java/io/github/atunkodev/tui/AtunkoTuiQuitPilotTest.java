package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * The quit key, through the live pipeline.
 *
 * <p>Every other Pilot test ends by tearing the harness down itself, so none of them ever proved that {@code q} ends
 * the run loop — and it did not: {@code ToolkitApp.quit()} forwards to a runner reference that only the base class's
 * own {@code run()} assigns, which {@link AtunkoTui} overrides.
 */
class AtunkoTuiQuitPilotTest {

    private static final Duration EXIT_TIMEOUT = Duration.ofSeconds(5);

    @Test
    @SVCs({"atunko:SVC_TUI_0001.28"})
    void quitKeyEndsTheRunLoop() throws Exception {
        try (PilotTestSupport tui = PilotTestSupport.launch()) {
            tui.pilot().press('q');

            assertThat(tui.awaitExit(EXIT_TIMEOUT)).as("q must quit the TUI").isTrue();
        }
    }
}
