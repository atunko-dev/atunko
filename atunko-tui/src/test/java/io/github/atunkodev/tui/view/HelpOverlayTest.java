package io.github.atunkodev.tui.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HelpOverlayTest {

    @Test
    void browserHelpContainsOptionsEntry() {
        boolean hasOptions = HelpOverlay.BROWSER_HELP.stream()
                .flatMap(s -> s.entries().stream())
                .anyMatch(e -> "o".equals(e.key()));
        assertThat(hasOptions).isTrue();
    }

    @Test
    void runDialogHelpContainsOptionsEntry() {
        boolean hasOptions = HelpOverlay.RUN_DIALOG_HELP.stream()
                .flatMap(s -> s.entries().stream())
                .anyMatch(e -> "o".equals(e.key()));
        assertThat(hasOptions).isTrue();
    }
}
