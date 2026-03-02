package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Set;

@Requirements({"CLI_0001.14"})
public final class ConfirmRunView {

    private ConfirmRunView() {}

    public static Element render(TuiController controller) {
        List<DisplayRow> displayRows = controller.runDisplayRows();
        Set<String> selected = controller.selectedRecipes();
        boolean hasRecipes = !displayRows.isEmpty();
        String projectPath =
                controller.projectDir().toAbsolutePath().normalize().toString();

        Element centerContent;
        if (hasRecipes) {
            centerContent = column(
                    row(text("Project: ").bold(), text(projectPath)),
                    text(""),
                    renderRecipeList(controller, displayRows, selected));
        } else {
            centerContent = column(
                    text(""),
                    text(" No recipes selected.").bold().fg(Color.LIGHT_YELLOW),
                    text(" Use Space to select recipes, then press r to run."));
        }

        long selectedCount = displayRows.stream()
                .filter(r -> selected.contains(r.recipe().name()))
                .count();
        long totalCount = displayRows.size();
        String footer = hasRecipes
                ? " \u2191\u2193:nav +/-:reorder Space:toggle a:sel all/none \u2192:expand \u2190:collapse f:flatten"
                        + " r:run d:dry-run Esc:back"
                : " Esc:back";

        return column(dock().top(
                                row(
                                        text(" Run Recipes ")
                                                .bold()
                                                .fg(Color.WHITE)
                                                .bg(Color.BLUE),
                                        spacer(),
                                        text(selectedCount + "/" + totalCount + " selected ")
                                                .fg(Color.LIGHT_GREEN)),
                                Constraint.length(1))
                        .center(centerContent)
                        .bottom(text(" " + footer).fg(Color.WHITE).bg(Color.indexed(236)), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("confirm-run")
                .focusable()
                .onKeyEvent(event -> handleKeyEvent(controller, hasRecipes, event));
    }

    private static Element renderRecipeList(
            TuiController controller, List<DisplayRow> displayRows, Set<String> selected) {
        var recipeList =
                list().highlightStyle(Style.EMPTY.fg(Color.WHITE).bg(Color.BLUE).bold());

        int parentIndex = 0;
        for (DisplayRow displayRow : displayRows) {
            RecipeInfo r = displayRow.recipe();
            boolean isSelected = selected.contains(r.name());
            String displayName = BrowserView.cleanDisplayName(r.displayName());
            boolean isExpanded = controller.runExpandedRecipes().contains(r.name());

            if (displayRow.isSubRecipe()) {
                String indent = "  ".repeat(displayRow.depth());
                String indicator = r.isComposite() ? (isExpanded ? "\u25bc " : "\u25b6 ") : "  ";
                String prefix = "    " + indent + (isSelected ? "[x] " : "[ ] ") + indicator;
                var prefixEl = isSelected
                        ? text(prefix).fg(Color.LIGHT_GREEN)
                        : text(prefix).dim();
                var nameElement =
                        isSelected ? text(displayName) : text(displayName).dim();
                recipeList.add(row(prefixEl, nameElement));
            } else {
                parentIndex++;
                String check = isSelected ? "[x] " : "[ ] ";
                String indicator = r.isComposite() ? (isExpanded ? "\u25bc " : "\u25b6 ") : "  ";
                String prefix = String.format("%2d. %s%s", parentIndex, check, indicator);
                var prefixEl = isSelected
                        ? text(prefix).fg(Color.LIGHT_GREEN)
                        : text(prefix).dim();
                var nameElement =
                        isSelected ? text(displayName) : text(displayName).dim();
                recipeList.add(row(prefixEl, nameElement));
            }
        }

        return recipeList
                .selected(controller.runHighlightIndex())
                .title("Execution Order")
                .rounded()
                .borderColor(Color.LIGHT_CYAN)
                .autoScroll();
    }

    private static EventResult handleKeyEvent(
            TuiController controller, boolean hasRecipes, dev.tamboui.tui.event.KeyEvent event) {
        if (hasRecipes) {
            if (event.isDown()) {
                controller.moveRunHighlightDown();
                return EventResult.HANDLED;
            }
            if (event.isUp()) {
                controller.moveRunHighlightUp();
                return EventResult.HANDLED;
            }
            if (event.isChar('+') || (event.code() == dev.tamboui.tui.event.KeyCode.DOWN && event.hasCtrl())) {
                controller.moveRunRecipeDown();
                return EventResult.HANDLED;
            }
            if (event.isChar('-') || (event.code() == dev.tamboui.tui.event.KeyCode.UP && event.hasCtrl())) {
                controller.moveRunRecipeUp();
                return EventResult.HANDLED;
            }
            if (event.isChar(' ') || event.isConfirm()) {
                controller.toggleRunRecipe();
                return EventResult.HANDLED;
            }
            if (event.isChar('a')) {
                controller.cycleRunSelection();
                return EventResult.HANDLED;
            }
            if (event.isRight() || event.isChar('e')) {
                controller.expandRunRecipe();
                return EventResult.HANDLED;
            }
            if (event.isLeft() || event.isChar('c')) {
                controller.collapseRunRecipe();
                return EventResult.HANDLED;
            }
            if (event.isChar('f')) {
                controller.flattenRunRecipe();
                return EventResult.HANDLED;
            }
            if (event.isChar('r')) {
                controller.runSelectedRecipes(false);
                return EventResult.HANDLED;
            }
            if (event.isChar('d')) {
                controller.runSelectedRecipes(true);
                return EventResult.HANDLED;
            }
        }
        if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            controller.goBack();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}
