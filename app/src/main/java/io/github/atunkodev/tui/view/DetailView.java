package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;

@Requirements({"CLI_0001.4", "CLI_0001.7"})
public final class DetailView {

    private DetailView() {}

    public static Element render(TuiController controller) {
        return controller
                .highlightedRecipe()
                .map(recipe -> renderRecipeDetail(controller, recipe))
                .orElse(text("No recipe selected"));
    }

    private static Element renderRecipeDetail(TuiController controller, RecipeInfo recipe) {
        boolean selected = controller.selectedRecipes().contains(recipe.name());
        var selectionLabel = selected
                ? text("Selected ").fg(Color.LIGHT_GREEN)
                : text("Not selected ").dim();

        var detailContent = column(
                row(text("Name: ").bold(), text(recipe.name())),
                row(text("Display Name: ").bold(), text(BrowserView.cleanDisplayName(recipe.displayName()))),
                text(""),
                text("Description:").bold(),
                text(recipe.description() != null ? recipe.description() : "(none)"),
                text(""),
                text("Tags:").bold(),
                text(recipe.tags().isEmpty() ? "(none)" : String.join(", ", recipe.tags()))
                        .fg(Color.LIGHT_CYAN));

        if (recipe.isComposite()) {
            detailContent.add(text(""));
            detailContent.add(text("Recipe List:").bold());
            int index = 1;
            for (RecipeInfo sub : recipe.recipeList()) {
                detailContent.add(row(
                        text("  " + index + ". ").fg(Color.LIGHT_YELLOW),
                        text(BrowserView.cleanDisplayName(sub.displayName())).fg(Color.LIGHT_CYAN)));
                index++;
            }
        }

        return column(dock().top(
                                row(
                                        text(" " + BrowserView.cleanDisplayName(recipe.displayName()))
                                                .bold()
                                                .fg(Color.WHITE)
                                                .bg(Color.BLUE),
                                        spacer(),
                                        selectionLabel),
                                Constraint.length(1))
                        .center(panel("Recipe Detail", detailContent).rounded().borderColor(Color.LIGHT_CYAN))
                        .bottom(
                                text(" Esc/q:back Space:toggle selection")
                                        .fg(Color.WHITE)
                                        .bg(Color.indexed(236)),
                                Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("detail")
                .focusable()
                .onKeyEvent(event -> {
                    if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
                        controller.goBack();
                        return EventResult.HANDLED;
                    }
                    if (event.isChar(' ')) {
                        controller.toggleSelection();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }
}
