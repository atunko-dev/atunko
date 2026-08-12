package io.github.atunkodev.tui.shell;

import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.KeyTrigger;
import io.github.atunkodev.tui.view.HelpOverlay;
import io.github.reqstool.annotations.Requirements;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every atunko key binding, declared once.
 *
 * <p>Built on {@link BindingSets#standard()}, whose own javadoc reads "No vim or emacs-style bindings … safe for use
 * with text input (no letter keys bound to navigation)" — the same constraint atunko is held to. atunko previously
 * hand-rolled its keys with 65 {@code isChar(...)} comparisons spread over ten view classes and never called
 * {@code ToolkitRunner.Builder.bindings(...)} at all, so there was no single place a key's meaning lived.
 *
 * <p>Each binding carries a <em>description</em>, and both the footer hints and the help screen are derived from
 * this list. That is what makes it impossible for them to disagree with the behaviour — the drift this replaces had
 * already reached the docs, which documented {@code c} for collapse when nothing bound it.
 *
 * <p>No consumer of TamboUI surveyed for this work generates hints from bindings (the framework gap is tamboui#168);
 * the pattern is borrowed from Textual and Bubble Tea.
 */
@Requirements({"atunko:TUI_0009.6"})
public final class AtunkoBindings {

    /**
     * Action ids are prefixed {@code ATUNKO_} so they cannot collide with the framework's, and are uppercase with
     * underscores because TamboUI rejects anything else ("Action name must be alphanumeric with underscores only").
     *
     * <p>Navigation reuses the framework's own action ids: {@link BindingSets#standard()} already binds the arrow keys
     * to them, and rebinding would either duplicate or fight that. These are declared here only so the footer and
     * help can describe them from the same list as everything else.
     */
    public static final String MOVE = dev.tamboui.tui.bindings.Actions.MOVE_UP;

    public static final String FOCUS_NEXT = dev.tamboui.tui.bindings.Actions.FOCUS_NEXT;
    public static final String EXPAND = "ATUNKO_EXPAND";
    public static final String COLLAPSE = "ATUNKO_COLLAPSE";
    public static final String EXPAND_ALL = "ATUNKO_EXPAND_ALL";
    public static final String COLLAPSE_ALL = "ATUNKO_COLLAPSE_ALL";
    public static final String TOGGLE_SELECTION = "ATUNKO_TOGGLE_SELECTION";
    public static final String SELECT_ALL = "ATUNKO_SELECT_ALL";
    public static final String DESELECT_ALL = "ATUNKO_DESELECT_ALL";
    public static final String OPEN_DETAIL = "ATUNKO_OPEN_DETAIL";
    public static final String OPEN_OPTIONS = "ATUNKO_OPEN_OPTIONS";
    public static final String OPEN_RUN = "ATUNKO_OPEN_RUN";
    public static final String OPEN_TAGS = "ATUNKO_OPEN_TAGS";
    public static final String SEARCH = "ATUNKO_SEARCH";
    public static final String NEXT_MATCH = "ATUNKO_NEXT_MATCH";
    public static final String PREV_MATCH = "ATUNKO_PREV_MATCH";
    public static final String CYCLE_SORT = "ATUNKO_CYCLE_SORT";
    public static final String CYCLE_SOURCE = "ATUNKO_CYCLE_SOURCE";
    public static final String TOGGLE_FAVORITE = "ATUNKO_TOGGLE_FAVORITE";
    public static final String CYCLE_FAVORITES_FILTER = "ATUNKO_CYCLE_FAVORITES_FILTER";
    public static final String SAVE_CONFIG = "ATUNKO_SAVE_CONFIG";
    public static final String LOAD_CONFIG = "ATUNKO_LOAD_CONFIG";
    public static final String HELP = "ATUNKO_HELP";
    public static final String QUIT = "ATUNKO_QUIT";
    public static final String BACK = "ATUNKO_BACK";
    public static final String OPEN_DIFF = "ATUNKO_OPEN_DIFF";
    public static final String FLATTEN = "ATUNKO_FLATTEN";
    public static final String EXPORT = "ATUNKO_EXPORT";
    public static final String DRY_RUN = "ATUNKO_DRY_RUN";

    /**
     * One binding.
     *
     * @param section the help section it belongs under, which is also the grouping the help screen renders
     * @param hintKey how the key is written for the user — {@code "↑↓"} reads better than two separate entries
     */
    public record Binding(String action, KeyTrigger trigger, String hintKey, String description, String section) {

        /** {@code null} trigger means the framework's standard set already binds it; we only describe it. */
        boolean isFrameworkProvided() {
            return trigger == null;
        }
    }

    private static final List<Binding> BINDINGS = List.of(
            new Binding(MOVE, null, "\u2191\u2193", "Move", "Navigation"),
            new Binding(FOCUS_NEXT, null, "Tab", "Next pane", "Navigation"),
            new Binding(EXPAND, KeyTrigger.ch('e'), "e/\u2192", "Expand", "Navigation"),
            new Binding(COLLAPSE, KeyTrigger.ch('<'), "</\u2190", "Collapse", "Navigation"),
            new Binding(EXPAND_ALL, KeyTrigger.ch('E'), "E", "Expand all", "Navigation"),
            new Binding(COLLAPSE_ALL, KeyTrigger.ch('W'), "W", "Collapse all", "Navigation"),
            new Binding(TOGGLE_SELECTION, KeyTrigger.ch(' '), "Space", "Toggle", "Selection"),
            new Binding(SELECT_ALL, KeyTrigger.ch('a'), "a", "Select all", "Selection"),
            new Binding(DESELECT_ALL, KeyTrigger.ch('A'), "A", "Deselect all", "Selection"),
            new Binding(OPEN_DETAIL, null, "Enter", "Detail view", "Actions"),
            new Binding(OPEN_OPTIONS, KeyTrigger.ch('o'), "o", "Recipe options", "Actions"),
            new Binding(OPEN_RUN, KeyTrigger.ch('r'), "r", "Run dialog", "Actions"),
            new Binding(OPEN_TAGS, KeyTrigger.ch('t'), "t", "Tag browser", "Actions"),
            new Binding(SEARCH, KeyTrigger.ch('/'), "/", "Search", "Actions"),
            new Binding(NEXT_MATCH, KeyTrigger.ch('n'), "n", "Next match", "Actions"),
            new Binding(PREV_MATCH, KeyTrigger.ch('N'), "N", "Previous match", "Actions"),
            new Binding(CYCLE_SORT, KeyTrigger.ch('s'), "s", "Sort order", "Actions"),
            new Binding(CYCLE_SOURCE, KeyTrigger.ch('u'), "u", "Recipe source", "Actions"),
            new Binding(TOGGLE_FAVORITE, KeyTrigger.ch('f'), "f", "Toggle favorite", "Actions"),
            new Binding(CYCLE_FAVORITES_FILTER, KeyTrigger.ch('F'), "F", "Favorites filter", "Actions"),
            new Binding(SAVE_CONFIG, KeyTrigger.ch('S'), "S", "Save run config", "Actions"),
            new Binding(LOAD_CONFIG, KeyTrigger.ch('L'), "L", "Load run config", "Actions"),
            new Binding(HELP, KeyTrigger.ch('?'), "?", "Help", "Actions"),
            new Binding(QUIT, KeyTrigger.ch('q'), "q", "Quit", "Actions"),
            new Binding(BACK, null, "Esc", "Back", "Actions"),
            new Binding(OPEN_DIFF, null, "Enter", "Show diff", "Actions"),
            // 'f' is Toggle favorite in the browser and used to be Flatten in the run dialog — the cross-screen
            // collision issue #87 flagged. A single registry cannot hold both, so flatten moves to 'l'.
            new Binding(FLATTEN, KeyTrigger.ch('l'), "l", "Flatten composite", "Actions"),
            new Binding(EXPORT, KeyTrigger.ch('x'), "x", "Export config", "Actions"),
            new Binding(DRY_RUN, KeyTrigger.ch('d'), "d", "Dry-run", "Actions"));

    private AtunkoBindings() {}

    /** All declared bindings, in declaration order. */
    public static List<Binding> all() {
        return BINDINGS;
    }

    /** The framework binding set with atunko's actions added. Deliberately built from the vim-free standard set. */
    @Requirements({"atunko:TUI_0009.6"})
    public static Bindings bindings() {
        Bindings.Builder builder = BindingSets.standard().toBuilder();
        for (Binding binding : BINDINGS) {
            if (!binding.isFrameworkProvided()) {
                builder.bind(binding.trigger(), binding.action());
            }
        }
        return builder.build();
    }

    /** Footer hints for the named actions, in the order given. */
    @Requirements({"atunko:TUI_0009.2", "atunko:TUI_0009.6"})
    public static List<KeyHint> hintsFor(String... actions) {
        List<KeyHint> hints = new ArrayList<>();
        for (String action : actions) {
            find(action)
                    .ifPresent(b ->
                            hints.add(KeyHint.of(b.hintKey(), b.description().toLowerCase(java.util.Locale.ROOT))));
        }
        return List.copyOf(hints);
    }

    /** Help sections, grouped as declared — the same data the footer hints come from. */
    @Requirements({"atunko:TUI_0009.6", "atunko:TUI_0009.7"})
    public static List<HelpOverlay.Section> helpSections(String... actions) {
        Map<String, List<HelpOverlay.Entry>> grouped = new LinkedHashMap<>();
        for (String action : actions) {
            find(action)
                    .ifPresent(b -> grouped.computeIfAbsent(b.section(), k -> new ArrayList<>())
                            .add(new HelpOverlay.Entry(b.hintKey(), b.description())));
        }
        return grouped.entrySet().stream()
                .map(e -> new HelpOverlay.Section(e.getKey(), List.copyOf(e.getValue())))
                .toList();
    }

    public static java.util.Optional<Binding> find(String action) {
        return BINDINGS.stream().filter(b -> b.action().equals(action)).findFirst();
    }
}
