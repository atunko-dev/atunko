package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.widgets.input.TextInputState;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Requirements({"atunko:TUI_0001.11"})
public final class TagBrowserView {

    private static final TextInputState TAG_SEARCH_STATE = new TextInputState();
    private static int tagIndex;
    private static boolean tagSearchMode;

    private TagBrowserView() {}

    public static Element render(TuiController controller) {
        List<String> allTags = controller.allTags();
        String query = TAG_SEARCH_STATE.text().toLowerCase(Locale.ROOT);
        List<String> tags = query.isBlank()
                ? allTags
                : allTags.stream()
                        .filter(t -> t.toLowerCase(Locale.ROOT).contains(query))
                        .toList();

        Set<String> selected = controller.selectedTags();

        var recipeList =
                list().highlightStyle(Style.EMPTY.fg(Color.WHITE).bg(Color.BLUE).bold());
        for (String tag : tags) {
            boolean isSelected = selected.contains(tag);
            String prefix = isSelected ? "[x] " : "[ ] ";
            var prefixEl = isSelected
                    ? text(prefix).fg(Color.LIGHT_GREEN)
                    : text(prefix).dim();
            recipeList.add(row(prefixEl, text(tag)));
        }

        var headerLabel = tagSearchMode
                ? text(" SEARCH TAGS ").bold().fg(Color.BLACK).bg(Color.LIGHT_YELLOW)
                : text(" Tag Browser ").bold().fg(Color.WHITE).bg(Color.BLUE);

        long selectedCount = selected.size();
        var selectedIndicator =
                selectedCount > 0 ? text(" " + selectedCount + " selected ").fg(Color.LIGHT_GREEN) : text("");

        Element header = row(
                headerLabel,
                text(" "),
                selectedIndicator,
                spacer(),
                textInput(TAG_SEARCH_STATE)
                        .placeholder("Filter tags...")
                        .rounded()
                        .focusable(false)
                        .cursorRequiresFocus(false)
                        .constraint(Constraint.fill(1)));

        String footer = tagSearchMode
                ? " Type to filter | Enter:apply Esc:clear search"
                : " \u2191\u2193/jk:nav Space:sel Enter:apply /:search Esc:clear q:back";

        return column(dock().top(header, Constraint.length(3))
                        .center(recipeList
                                .selected(tagIndex)
                                .title("Tags (" + tags.size() + ")")
                                .rounded()
                                .borderColor(Color.LIGHT_CYAN)
                                .autoScroll())
                        .bottom(text(" " + footer).fg(Color.WHITE).bg(Color.indexed(236)), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("tag-browser")
                .focusable()
                .onKeyEvent(event -> handleKeyEvent(controller, tags, event));
    }

    private static EventResult handleKeyEvent(
            TuiController controller, List<String> tags, dev.tamboui.tui.event.KeyEvent event) {
        if (tagSearchMode) {
            return handleSearchModeKey(tags, event);
        }
        return handleBrowseModeKey(controller, tags, event);
    }

    private static EventResult handleSearchModeKey(List<String> tags, dev.tamboui.tui.event.KeyEvent event) {
        if (event.isConfirm()) {
            tagSearchMode = false;
            return EventResult.HANDLED;
        }
        if (event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            TAG_SEARCH_STATE.clear();
            tagSearchMode = false;
            tagIndex = 0;
            return EventResult.HANDLED;
        }
        if (event.isDown() || event.isChar('j')) {
            tagIndex = Math.min(tagIndex + 1, Math.max(tags.size() - 1, 0));
            return EventResult.HANDLED;
        }
        if (event.isUp() || event.isChar('k')) {
            tagIndex = Math.max(tagIndex - 1, 0);
            return EventResult.HANDLED;
        }
        if (handleTextInputKey(TAG_SEARCH_STATE, event)) {
            tagIndex = 0;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static EventResult handleBrowseModeKey(
            TuiController controller, List<String> tags, dev.tamboui.tui.event.KeyEvent event) {
        if (event.isDown() || event.isChar('j')) {
            tagIndex = Math.min(tagIndex + 1, Math.max(tags.size() - 1, 0));
            return EventResult.HANDLED;
        }
        if (event.isUp() || event.isChar('k')) {
            tagIndex = Math.max(tagIndex - 1, 0);
            return EventResult.HANDLED;
        }
        if (event.isChar(' ') && !tags.isEmpty()) {
            controller.toggleTag(tags.get(tagIndex));
            return EventResult.HANDLED;
        }
        if (event.isConfirm()) {
            controller.applyTagFilter();
            tagIndex = 0;
            TAG_SEARCH_STATE.clear();
            return EventResult.HANDLED;
        }
        if (event.isChar('/')) {
            tagSearchMode = true;
            return EventResult.HANDLED;
        }
        if (event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            controller.clearTagFilter();
            tagIndex = 0;
            TAG_SEARCH_STATE.clear();
            return EventResult.HANDLED;
        }
        if (event.isChar('q')) {
            controller.goBack();
            tagIndex = 0;
            TAG_SEARCH_STATE.clear();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}
