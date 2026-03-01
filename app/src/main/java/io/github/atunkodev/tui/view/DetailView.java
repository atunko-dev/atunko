package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
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
        String selectionStatus = selected ? "Selected" : "Not selected";

        return dock().top(
                        row(
                                text(" " + recipe.displayName()).bold().cyan(),
                                spacer(),
                                text(selectionStatus + " ").dim()),
                        Constraint.length(1))
                .center(panel(
                                "Recipe Detail",
                                column(
                                        text("Name: " + recipe.name()),
                                        text("Display Name: " + recipe.displayName()),
                                        text(""),
                                        text("Description:").bold(),
                                        text(recipe.description() != null ? recipe.description() : "(none)"),
                                        text(""),
                                        text("Tags:").bold(),
                                        text(recipe.tags().isEmpty() ? "(none)" : String.join(", ", recipe.tags()))))
                        .rounded())
                .bottom(text(" Esc/q:back Space:toggle selection").dim(), Constraint.length(1))
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
