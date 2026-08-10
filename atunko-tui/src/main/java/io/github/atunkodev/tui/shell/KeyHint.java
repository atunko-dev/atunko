package io.github.atunkodev.tui.shell;

import io.github.atunkodev.tui.view.HelpOverlay;
import io.github.reqstool.annotations.Requirements;

/**
 * One footer hint: the key the user presses and what it does.
 *
 * <p>Kept as two fields rather than one pre-formatted string so the shell can style them differently — atunko's old
 * footer concatenated everything into one uniformly styled sentence, which made no key findable at a glance.
 *
 * @param key the key as the user sees it, e.g. {@code "Esc"} or {@code "↑↓"}
 * @param label what pressing it does, in one or two words
 */
@Requirements({"atunko:TUI_0009.2"})
public record KeyHint(String key, String label) {

    /** Help entries and footer hints are the same information; this keeps one source feeding both. */
    public HelpOverlay.Entry toHelpEntry() {
        return new HelpOverlay.Entry(key, label);
    }

    public static KeyHint of(String key, String label) {
        return new KeyHint(key, label);
    }
}
