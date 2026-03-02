package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.tabs;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.widgets.input.TextInputState;
import io.github.atunkodev.core.recipe.SortOrder;
import io.github.atunkodev.tui.AtunkoTui;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"atunko:TUI_0001.1", "atunko:TUI_0001.2", "atunko:TUI_0001.13"})
public final class BrowserView {

    private static final TextInputState SEARCH_STATE = new TextInputState();

    private BrowserView() {}

    public static Element render(TuiController controller, AtunkoTui app) {
        List<DisplayRow> displayRows = controller.displayRows();

        return column(dock().top(renderHeader(controller), Constraint.length(3))
                        .center(row(renderRecipeList(controller, displayRows), renderDetailPanel(controller))
                                .constraint(Constraint.fill()))
                        .bottom(renderStatusBar(controller, displayRows), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("browser")
                .focusable()
                .onKeyEvent(event -> handleKeyEvent(controller, app, event));
    }

    private static EventResult handleKeyEvent(
            TuiController controller, AtunkoTui app, dev.tamboui.tui.event.KeyEvent event) {
        if (controller.isSearchMode()) {
            return handleSearchModeKey(controller, event);
        }
        return handleBrowseModeKey(controller, app, event);
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
        if (event.isDown()) {
            controller.moveDown();
            return EventResult.HANDLED;
        }
        if (event.isUp()) {
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
            controller.cycleSelection();
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
        if (event.isRight() || event.isChar('e')) {
            controller.highlightedDisplayRow().ifPresent(row -> {
                if (row.recipe().isComposite()) {
                    controller.expandRecipe(row.recipe().name());
                }
            });
            return EventResult.HANDLED;
        }
        if (event.isLeft()) {
            controller.collapseHighlighted();
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
            controller.setSortOrder(controller.sortOrder() == SortOrder.NAME ? SortOrder.TAGS : SortOrder.NAME);
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static Element renderHeader(TuiController controller) {
        if (!controller.isSearchMode()) {
            SEARCH_STATE.setText(controller.searchQuery());
        }
        var headerLabel = controller.isSearchMode()
                ? text(" SEARCH ").bold().fg(Color.BLACK).bg(Color.LIGHT_YELLOW)
                : text(" atunko ").bold().fg(Color.WHITE).bg(Color.BLUE);
        var tagIndicator = controller.selectedTags().isEmpty()
                ? spacer()
                : text(" tags:" + String.join(",", controller.selectedTags()) + " ")
                        .fg(Color.BLACK)
                        .bg(Color.LIGHT_CYAN);
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
                tabs(SortOrder.NAME.name(), SortOrder.TAGS.name())
                        .selected(controller.sortOrder() == SortOrder.NAME ? 0 : 1));
    }

    private static Element renderRecipeList(TuiController controller, List<DisplayRow> displayRows) {
        return RecipeListRenderer.renderRecipeList(
                displayRows,
                controller.selectedRecipes(),
                controller.expandedRecipes(),
                controller.coveredRecipes(),
                controller.highlightedIndex(),
                "Recipes",
                RecipeListRenderer.RenderOptions.BROWSER,
                Constraint.fill(2));
    }

    @Requirements({"atunko:TUI_0001.16"})
    private static Element renderDetailPanel(TuiController controller) {
        return controller
                .highlightedRecipe()
                .map(recipe -> {
                    var content = column(
                            text(RecipeListRenderer.cleanDisplayName(recipe.displayName()))
                                    .bold()
                                    .fg(Color.LIGHT_CYAN),
                            text(""),
                            text(recipe.name()).dim(),
                            text(""),
                            text(recipe.description() != null ? recipe.description() : ""),
                            text(""),
                            row(
                                    text("Tags: ").bold(),
                                    text(recipe.tags().isEmpty() ? "none" : String.join(", ", recipe.tags()))
                                            .fg(Color.LIGHT_CYAN)),
                            recipe.isComposite()
                                    ? text("Composite: " + recipe.recipeList().size() + " sub-recipes")
                                            .fg(Color.LIGHT_CYAN)
                                    : text(""));
                    java.util.List<String> parents = controller.includedIn(recipe.name());
                    if (!parents.isEmpty()) {
                        content.add(text(""));
                        content.add(row(
                                text("Included in: ").bold().fg(Color.LIGHT_YELLOW),
                                text(String.join(", ", parents)).fg(Color.LIGHT_YELLOW)));
                    }
                    return (Element) panel("Detail", content)
                            .rounded()
                            .borderColor(Color.LIGHT_CYAN)
                            .constraint(Constraint.fill(1));
                })
                .orElse(panel("Detail", text("No recipe selected"))
                        .rounded()
                        .borderColor(Color.LIGHT_CYAN)
                        .constraint(Constraint.fill(1)));
    }

    private static Element renderStatusBar(TuiController controller, List<DisplayRow> displayRows) {
        int selected = controller.selectedRecipes().size();
        long parentCount = displayRows.stream().filter(r -> !r.isSubRecipe()).count();
        String status = parentCount + " recipes"
                + " | " + selected + " selected | \u2191\u2193:nav Space:sel a:all/none r:run Enter:detail"
                + " \u2192:expand \u2190:collapse t:tags s:sort /:search Esc:clear q:quit";
        return text(" " + status).fg(Color.WHITE).bg(Color.indexed(236));
    }
}
