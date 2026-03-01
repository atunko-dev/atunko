package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.tabs;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.widgets.input.TextInputState;
import io.github.atunkodev.cli.SortOrder;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.AtunkoTui;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"CLI_0001.1", "CLI_0001.2"})
public final class BrowserView {

    private static final TextInputState SEARCH_STATE = new TextInputState();

    private BrowserView() {}

    public static Element render(TuiController controller, AtunkoTui app) {
        List<RecipeInfo> recipes = controller.recipes();

        return dock().top(renderHeader(controller), Constraint.length(3))
                .center(row(renderRecipeList(controller, recipes, app), renderDetailPanel(controller))
                        .constraint(Constraint.fill()))
                .bottom(renderStatusBar(controller, recipes), Constraint.length(1));
    }

    private static Element renderHeader(TuiController controller) {
        SEARCH_STATE.setText(controller.searchQuery());
        return row(
                text(" atunko").bold().cyan(),
                spacer(),
                textInput(SEARCH_STATE)
                        .placeholder("Search recipes...")
                        .rounded()
                        .constraint(Constraint.length(40))
                        .focusable()
                        .onKeyEvent(event -> {
                            if (event.code() == dev.tamboui.tui.event.KeyCode.ENTER) {
                                controller.setSearchQuery(SEARCH_STATE.text());
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        }),
                spacer(),
                tabs(SortOrder.NAME.name(), SortOrder.TAGS.name())
                        .selected(controller.sortOrder() == SortOrder.NAME ? 0 : 1)
                        .onKeyEvent(event -> {
                            if (event.isLeft() || event.isRight()) {
                                controller.setSortOrder(
                                        controller.sortOrder() == SortOrder.NAME ? SortOrder.TAGS : SortOrder.NAME);
                                return EventResult.HANDLED;
                            }
                            return EventResult.UNHANDLED;
                        }));
    }

    private static Element renderRecipeList(TuiController controller, List<RecipeInfo> recipes, AtunkoTui app) {
        var listItems = recipes.stream()
                .map(r -> {
                    boolean selected = controller.selectedRecipes().contains(r.name());
                    String prefix = selected ? "[x] " : "[ ] ";
                    String tags = r.tags().isEmpty() ? "" : " [" + String.join(", ", r.tags()) + "]";
                    return prefix + r.displayName() + tags;
                })
                .toList();

        return list(listItems)
                .selected(controller.highlightedIndex())
                .title("Recipes")
                .rounded()
                .autoScroll()
                .constraint(Constraint.fill(2))
                .focusable()
                .onKeyEvent(event -> {
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
                    if (event.character() == ' ') {
                        controller.toggleSelection();
                        return EventResult.HANDLED;
                    }
                    if (event.character() == 't') {
                        controller.openTagBrowser();
                        return EventResult.HANDLED;
                    }
                    if (event.character() == 'q') {
                        app.requestQuit();
                        return EventResult.HANDLED;
                    }
                    if (event.character() == '/') {
                        return EventResult.FOCUS_NEXT;
                    }
                    return EventResult.UNHANDLED;
                });
    }

    private static Element renderDetailPanel(TuiController controller) {
        return controller
                .highlightedRecipe()
                .map(recipe -> (Element) panel(
                                "Detail",
                                column(
                                        text(recipe.displayName()).bold(),
                                        text(""),
                                        text(recipe.name()).dim(),
                                        text(""),
                                        text(recipe.description() != null ? recipe.description() : ""),
                                        text(""),
                                        text("Tags: "
                                                + (recipe.tags().isEmpty()
                                                        ? "none"
                                                        : String.join(", ", recipe.tags())))))
                        .rounded()
                        .constraint(Constraint.fill(1)))
                .orElse(panel("Detail", text("No recipe selected")).rounded().constraint(Constraint.fill(1)));
    }

    private static Element renderStatusBar(TuiController controller, List<RecipeInfo> recipes) {
        int selected = controller.selectedRecipes().size();
        String status = recipes.size() + " recipes"
                + (selected > 0 ? " | " + selected + " selected" : "")
                + " | ↑↓:navigate Space:select Enter:detail t:tags q:quit /:search";
        return text(" " + status).dim();
    }
}
