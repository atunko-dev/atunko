package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.input.TextInputState;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.shell.AtunkoBindings;
import io.github.atunkodev.tui.shell.KeyHint;
import io.github.atunkodev.tui.shell.TuiView;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Requirements({"atunko:TUI_0001.11"})
public final class TagBrowserView implements TuiView {

    private static final TextInputState TAG_SEARCH_STATE = new TextInputState();
    private static int tagIndex;
    private static boolean tagSearchMode;

    @Override
    public String id() {
        return "tag-browser";
    }

    @Override
    public String title(TuiController controller) {
        return tagSearchMode ? "SEARCH TAGS" : "Tag Browser";
    }

    @Override
    public String status(TuiController controller) {
        List<String> tags = visibleTags(controller);
        return tags.size() + " tags | " + controller.selectedTags().size() + " selected";
    }

    @Override
    public List<KeyHint> keyHints(TuiController controller) {
        if (tagSearchMode) {
            return List.of(KeyHint.of("Enter", "apply"), KeyHint.of("Esc", "clear search"));
        }
        return AtunkoBindings.hintsFor(
                AtunkoBindings.MOVE,
                AtunkoBindings.TOGGLE_SELECTION,
                AtunkoBindings.SEARCH,
                AtunkoBindings.HELP,
                AtunkoBindings.BACK);
    }

    @Override
    public List<HelpOverlay.Section> helpSections() {
        return AtunkoBindings.helpSections(
                AtunkoBindings.MOVE,
                AtunkoBindings.TOGGLE_SELECTION,
                AtunkoBindings.SEARCH,
                AtunkoBindings.HELP,
                AtunkoBindings.BACK);
    }

    @Override
    public EventResult handleKey(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        if (controller.isShowHelp()) {
            controller.toggleHelp();
            return event.isChar('?') ? EventResult.HANDLED : EventResult.UNHANDLED;
        }
        if (event.isChar('?')) {
            controller.toggleHelp();
            return EventResult.HANDLED;
        }
        return handleKeyEvent(controller, visibleTags(controller), event);
    }

    @Override
    public EventResult handleMouse(TuiController controller, MouseEvent event) {
        return handleMouseEvent(controller, visibleTags(controller), event);
    }

    /** Tags after the filter box, shared by rendering, status and event handling. */
    private static List<String> visibleTags(TuiController controller) {
        String query = TAG_SEARCH_STATE.text().toLowerCase(Locale.ROOT);
        return query.isBlank()
                ? controller.allTags()
                : controller.allTags().stream()
                        .filter(t -> t.toLowerCase(Locale.ROOT).contains(query))
                        .toList();
    }

    @Override
    public StyledElement<?> renderHeaderExtras(TuiController controller) {
        return textInput(TAG_SEARCH_STATE)
                .placeholder("Filter tags...")
                .focusable(false)
                .cursorRequiresFocus(false)
                .constraint(Constraint.fill(1));
    }

    @Override
    public StyledElement<?> renderContent(TuiController controller) {
        List<String> tags = visibleTags(controller);
        Set<String> selected = controller.selectedTags();

        var recipeList = list().addClass("list-item");
        for (String tag : tags) {
            boolean isSelected = selected.contains(tag);
            String prefix = isSelected ? "[x] " : "[ ] ";
            var prefixEl = isSelected
                    ? text(prefix).addClass("selected")
                    : text(prefix).addClass("unselected");
            recipeList.add(row(prefixEl, text(tag)));
        }

        return recipeList
                .selected(tagIndex)
                .title("Tags (" + tags.size() + ")")
                .addClass("panel")
                .autoScroll();
    }

    private static EventResult handleMouseEvent(TuiController controller, List<String> tags, MouseEvent event) {
        if (event.kind() == MouseEventKind.SCROLL_UP) {
            tagIndex = Math.max(tagIndex - 1, 0);
            return EventResult.HANDLED;
        }
        if (event.kind() == MouseEventKind.SCROLL_DOWN) {
            tagIndex = Math.min(tagIndex + 1, Math.max(tags.size() - 1, 0));
            return EventResult.HANDLED;
        }
        if (event.isPress() && event.isLeftButton()) {
            int idx = controller.mouseRowToIndex(event.y(), 3, tags.size());
            if (idx >= 0) {
                tagIndex = idx;
                return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
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
        if (event.isDown()) {
            tagIndex = Math.min(tagIndex + 1, Math.max(tags.size() - 1, 0));
            return EventResult.HANDLED;
        }
        if (event.isUp()) {
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
        if (event.isDown()) {
            tagIndex = Math.min(tagIndex + 1, Math.max(tags.size() - 1, 0));
            return EventResult.HANDLED;
        }
        if (event.isUp()) {
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
