package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.tabs;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.input.TextInputState;
import io.github.atunkodev.core.recipe.FavoritesFilter;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.atunkodev.tui.AtunkoTui;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.atunkodev.tui.shell.KeyHint;
import io.github.atunkodev.tui.shell.TuiView;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

@Requirements({"atunko:TUI_0001.1", "atunko:TUI_0001.2", "atunko:TUI_0001.13", "atunko:TUI_0009"})
public final class BrowserView implements TuiView {

    private static final Logger LOG = Logger.getLogger(BrowserView.class.getName());
    private static final TextInputState SEARCH_STATE = new TextInputState();
    private static final TextInputState SAVE_NAME_STATE = new TextInputState();

    private final AtunkoTui app;

    public BrowserView(AtunkoTui app) {
        this.app = app;
    }

    @Override
    public String id() {
        return "browser";
    }

    @Override
    public String title() {
        return controllerIsSearching ? "SEARCH" : "atunko";
    }

    // Set on each render so title() can reflect search mode without taking a controller argument.
    private boolean controllerIsSearching;

    /** State only — key hints live on their own footer row now. */
    @Override
    @Requirements({"atunko:TUI_0006.1", "atunko:TUI_0009.2"})
    public String status(TuiController controller) {
        int selected = controller.selectedRecipes().size();
        long parentCount =
                controller.displayRows().stream().filter(r -> !r.isSubRecipe()).count();
        String source = controller.sourceFilter().name().toLowerCase(Locale.ROOT);
        String favorites = controller.favoritesFilter() == FavoritesFilter.FAVORITES ? "only" : "all";
        String sort = controller.sortOrder().name().toLowerCase(Locale.ROOT);
        return parentCount + " recipes | " + selected + " selected | sort:" + sort + " | src:" + source + " | fav:"
                + favorites;
    }

    @Override
    @Requirements({"atunko:TUI_0009.2"})
    public List<KeyHint> keyHints(TuiController controller) {
        if (controller.isSaveConfigMode()) {
            return List.of(KeyHint.of("Enter", "save"), KeyHint.of("Esc", "cancel"));
        }
        if (controller.isSearchMode()) {
            return List.of(KeyHint.of("Enter", "apply"), KeyHint.of("Esc", "clear"));
        }
        return List.of(
                KeyHint.of("\u2191\u2193", "move"),
                KeyHint.of("Space", "select"),
                KeyHint.of("Enter", "detail"),
                KeyHint.of("r", "run"),
                KeyHint.of("/", "search"),
                KeyHint.of("t", "tags"),
                KeyHint.of("o", "options"),
                KeyHint.of("?", "help"),
                KeyHint.of("q", "quit"));
    }

    @Override
    public List<HelpOverlay.Section> helpSections() {
        return HelpOverlay.BROWSER_HELP;
    }

    @Override
    public StyledElement<?> renderContent(TuiController controller) {
        controllerIsSearching = controller.isSearchMode();
        if (controller.isShowOptions()) {
            return (StyledElement<?>) RecipeOptionsView.render(controller);
        }
        return (StyledElement<?>) renderRecipeList(controller, controller.displayRows());
    }

    @Override
    public StyledElement<?> renderDetails(TuiController controller) {
        return controller.isShowOptions() ? null : (StyledElement<?>) renderDetailPanel(controller);
    }

    @Override
    public StyledElement<?> renderHeaderExtras(TuiController controller) {
        if (controller.isSaveConfigMode()) {
            return (StyledElement<?>) row(
                    text(" Save config: ").addClass("detail-label"),
                    textInput(SAVE_NAME_STATE)
                            .placeholder("config-name")
                            .rounded()
                            .constraint(Constraint.fill()));
        }
        return (StyledElement<?>) renderHeader(controller);
    }

    @Override
    public EventResult handleKey(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        return handleKeyEvent(controller, app, event);
    }

    @Override
    public EventResult handleMouse(TuiController controller, MouseEvent event) {
        return handleMouseEvent(controller, event);
    }

    private static EventResult handleMouseEvent(TuiController controller, MouseEvent event) {
        if (controller.isShowHelp()
                || controller.isSaveConfigMode()
                || controller.isSearchMode()
                || controller.isShowOptions()) {
            return EventResult.UNHANDLED;
        }
        if (event.kind() == MouseEventKind.SCROLL_UP) {
            controller.moveUp();
            return EventResult.HANDLED;
        }
        if (event.kind() == MouseEventKind.SCROLL_DOWN) {
            controller.moveDown();
            return EventResult.HANDLED;
        }
        if (event.isPress()) {
            int idx = controller.mouseRowToIndex(
                    event.y(), 3, controller.displayRows().size());
            if (idx >= 0) {
                controller.setBrowserHighlightIndex(idx);
                if (event.isRightButton()) {
                    controller.toggleSelection();
                }
                return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
    }

    private static EventResult handleKeyEvent(
            TuiController controller, AtunkoTui app, dev.tamboui.tui.event.KeyEvent event) {
        if (controller.isShowHelp()) {
            controller.toggleHelp();
            return EventResult.HANDLED;
        }
        if (controller.isSaveConfigMode()) {
            return handleSaveConfigModeKey(controller, event);
        }
        if (controller.isSearchMode()) {
            return handleSearchModeKey(controller, event);
        }
        return handleBrowseModeKey(controller, app, event);
    }

    private static EventResult handleSaveConfigModeKey(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        if (event.isConfirm()) {
            try {
                controller.confirmSaveConfig();
            } catch (java.io.IOException e) {
                LOG.log(java.util.logging.Level.WARNING, "Failed to save run config", e);
            }
            return EventResult.HANDLED;
        }
        if (event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            controller.exitSaveConfigMode();
            return EventResult.HANDLED;
        }
        if (handleTextInputKey(SAVE_NAME_STATE, event)) {
            controller.setSaveConfigName(SAVE_NAME_STATE.text());
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static EventResult handleSearchModeKey(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        if (event.isConfirm()) {
            controller.setSearchQuery(SEARCH_STATE.text());
            controller.exitSearchMode();
            return EventResult.HANDLED;
        }
        if (event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            SEARCH_STATE.clear();
            controller.setSearchQuery("");
            controller.exitSearchMode();
            return EventResult.HANDLED;
        }
        if (handleTextInputKey(SEARCH_STATE, event)) {
            controller.setSearchQuery(SEARCH_STATE.text());
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static EventResult handleBrowseModeKey(
            TuiController controller, AtunkoTui app, dev.tamboui.tui.event.KeyEvent event) {
        if (event.isDown() || event.isChar('j')) {
            controller.moveDown();
            return EventResult.HANDLED;
        }
        if (event.isUp() || event.isChar('k')) {
            controller.moveUp();
            return EventResult.HANDLED;
        }
        if (event.isConfirm()) {
            controller.openDetail();
            return EventResult.HANDLED;
        }
        if (event.isChar(' ')) {
            controller.toggleSelection();
            return EventResult.HANDLED;
        }
        if (event.isChar('a')) {
            controller.selectAll();
            return EventResult.HANDLED;
        }
        if (event.isChar('A')) {
            controller.deselectAll();
            return EventResult.HANDLED;
        }
        if (event.isChar('n') && !controller.searchQuery().isBlank()) {
            controller.nextSearchMatch();
            return EventResult.HANDLED;
        }
        if (event.isChar('N') && !controller.searchQuery().isBlank()) {
            controller.prevSearchMatch();
            return EventResult.HANDLED;
        }
        if (event.isChar('o')) {
            controller
                    .highlightedDisplayRow()
                    .filter(row -> !row.isSubRecipe())
                    .ifPresent(row -> controller.openOptions(row.recipe().name()));
            return EventResult.HANDLED;
        }
        if (event.isChar('r')) {
            controller.openConfirmRun();
            return EventResult.HANDLED;
        }
        if (event.isChar('t')) {
            controller.openTagBrowser();
            return EventResult.HANDLED;
        }
        if (event.isRight() || event.isChar('e') || event.isChar('>')) {
            controller.highlightedDisplayRow().ifPresent(row -> {
                if (row.recipe().isComposite()) {
                    controller.expandRecipe(row.recipe().name());
                }
            });
            return EventResult.HANDLED;
        }
        if (event.isLeft() || event.isChar('<')) {
            controller.collapseHighlighted();
            return EventResult.HANDLED;
        }
        if (event.isChar('E')) {
            controller.expandAll();
            return EventResult.HANDLED;
        }
        if (event.isChar('W')) {
            controller.collapseAll();
            return EventResult.HANDLED;
        }
        if (event.isQuit() || event.isChar('q')) {
            app.requestQuit();
            return EventResult.HANDLED;
        }
        if (event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            SEARCH_STATE.clear();
            controller.clearAll();
            return EventResult.HANDLED;
        }
        if (event.isChar('/')) {
            controller.enterSearchMode();
            return EventResult.HANDLED;
        }
        if (event.isChar('s')) {
            controller.cycleSortOrder();
            return EventResult.HANDLED;
        }
        if (event.isChar('u')) {
            controller.cycleSourceFilter();
            return EventResult.HANDLED;
        }
        if (event.isChar('f')) {
            controller.toggleFavorite();
            return EventResult.HANDLED;
        }
        if (event.isChar('F')) {
            controller.cycleFavoritesFilter();
            return EventResult.HANDLED;
        }
        if (event.isChar('?')) {
            controller.toggleHelp();
            return EventResult.HANDLED;
        }
        if (event.isChar('S')) {
            SAVE_NAME_STATE.clear();
            controller.enterSaveConfigMode();
            return EventResult.HANDLED;
        }
        if (event.isChar('L')) {
            controller.openLoadConfig();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static Element renderHeader(TuiController controller) {
        if (!controller.isSearchMode()) {
            SEARCH_STATE.setText(controller.searchQuery());
        }
        var headerLabel = controller.isSearchMode()
                ? text(" SEARCH ").addClass("screen-title", "search-mode")
                : text(" atunko ").addClass("screen-title");
        var tagIndicator = controller.selectedTags().isEmpty()
                ? spacer()
                : text(" tags:" + String.join(",", controller.selectedTags()) + " ")
                        .addClass("tag-indicator");
        return row(
                headerLabel,
                text(" "),
                tagIndicator,
                text(" "),
                textInput(SEARCH_STATE)
                        .placeholder("Search recipes...")
                        .rounded()
                        .focusable(false)
                        .cursorRequiresFocus(false)
                        .constraint(Constraint.fill(3)),
                text(" "),
                tabs(SortOrder.NAME.name(), SortOrder.TAGS.name(), SortOrder.RECENT.name())
                        .selected(controller.sortOrder().ordinal()));
    }

    @Requirements({"atunko:TUI_0007.1"})
    private static Element renderRecipeList(TuiController controller, List<DisplayRow> displayRows) {
        return RecipeListRenderer.renderRecipeList(
                displayRows,
                controller.selectedRecipes(),
                controller.expandedRecipes(),
                controller.coveredRecipes(),
                controller.partialRecipes(),
                controller.highlightedIndex(),
                "Recipes",
                RecipeListRenderer.RenderOptions.BROWSER,
                Constraint.fill(2),
                controller::applicability,
                controller.favoriteRecipes());
    }

    @Requirements({"atunko:TUI_0001.16"})
    private static Element renderDetailPanel(TuiController controller) {
        return controller
                .highlightedRecipe()
                .map(recipe -> {
                    var content = column(
                            text(RecipeListRenderer.cleanDisplayName(recipe.displayName()))
                                    .addClass("recipe-name"),
                            text(""),
                            text(recipe.name()).addClass("unselected"),
                            text(""),
                            text(recipe.description() != null ? recipe.description() : ""),
                            text(""),
                            row(
                                    text("Tags: ").addClass("detail-label"),
                                    text(recipe.tags().isEmpty() ? "none" : String.join(", ", recipe.tags()))
                                            .addClass("detail-value")),
                            recipe.isComposite()
                                    ? text(compositeLabel(recipe, controller)).addClass("detail-value")
                                    : text(""));
                    java.util.List<String> parents = controller.includedIn(recipe.name());
                    if (!parents.isEmpty()) {
                        content.add(text(""));
                        content.add(row(
                                text("Included in: ").addClass("detail-label", "included-in"),
                                text(String.join(", ", parents)).addClass("included-in")));
                    }
                    return (Element) panel("Detail", content).addClass("panel").constraint(Constraint.fill(1));
                })
                .orElse(panel("Detail", text("No recipe selected"))
                        .addClass("panel")
                        .constraint(Constraint.fill(1)));
    }

    @Requirements({"atunko:TUI_0001.16"})
    private static String compositeLabel(RecipeInfo recipe, TuiController controller) {
        int total = recipe.recipeList().size();
        Set<String> covered = controller.coveredRecipes();
        Set<String> selected = controller.selectedRecipes();
        long coveredCount = recipe.recipeList().stream()
                .filter(sub -> covered.contains(sub.name()) || selected.contains(sub.name()))
                .count();
        return "Composite: " + coveredCount + "/" + total + " covered";
    }

    @Requirements({"atunko:TUI_0006.1", "atunko:TUI_0007.1"})
    private static Element renderStatusBar(TuiController controller, List<DisplayRow> displayRows) {
        int selected = controller.selectedRecipes().size();
        long parentCount = displayRows.stream().filter(r -> !r.isSubRecipe()).count();
        String source = controller.sourceFilter().name().toLowerCase(Locale.ROOT);
        String favorites = controller.favoritesFilter() == FavoritesFilter.FAVORITES ? "only" : "all";
        String status = parentCount + " recipes | " + selected + " selected | src:" + source + " | fav:" + favorites
                + " | o:options  ?:help  q:quit";
        return text(" " + status).addClass("status-bar");
    }
}
