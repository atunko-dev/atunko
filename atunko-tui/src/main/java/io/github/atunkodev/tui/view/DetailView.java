package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.markdown.MarkdownElement.markdown;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"atunko:TUI_0001.4", "atunko:TUI_0001.7"})
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
                ? text("Selected ").addClass("selected")
                : text("Not selected ").addClass("unselected");

        var metadataContent = column(
                row(text("Name: ").addClass("detail-label"), text(recipe.name())),
                row(
                        text("Display Name: ").addClass("detail-label"),
                        text(RecipeListRenderer.cleanDisplayName(recipe.displayName()))),
                text(""),
                row(
                        text("Tags: ").addClass("detail-label"),
                        text(recipe.tags().isEmpty() ? "(none)" : String.join(", ", recipe.tags()))
                                .addClass("detail-value")));

        if (recipe.isComposite()) {
            metadataContent.add(text(""));
            metadataContent.add(text("Recipe List:").addClass("detail-label"));
            int index = 1;
            for (RecipeInfo sub : recipe.recipeList()) {
                metadataContent.add(row(
                        text("  " + index + ". ").addClass("included-in"),
                        text(RecipeListRenderer.cleanDisplayName(sub.displayName()))
                                .addClass("detail-value")));
                index++;
            }
        }

        List<String> parents = controller.includedIn(recipe.name());
        if (!parents.isEmpty()) {
            metadataContent.add(text(""));
            metadataContent.add(row(
                    text("Included in: ").addClass("detail-label", "included-in"),
                    text(String.join(", ", parents)).addClass("included-in")));
        }

        int metadataLines =
                4 + (recipe.isComposite() ? 2 + recipe.recipeList().size() : 0) + (!parents.isEmpty() ? 2 : 0);

        String description = recipe.description() != null ? recipe.description() : "*(no description)*";

        Element centerContent;
        if (controller.isShowHelp()) {
            centerContent = row(spacer(), HelpOverlay.render(HelpOverlay.DETAIL_HELP), spacer());
        } else {
            centerContent = panel(
                            "Detail",
                            dock().top(metadataContent, Constraint.length(metadataLines))
                                    .center(markdown(description))
                                    .constraint(Constraint.fill()))
                    .addClass("panel");
        }

        return column(dock().top(
                                row(
                                        text(" " + RecipeListRenderer.cleanDisplayName(recipe.displayName()))
                                                .addClass("screen-title"),
                                        spacer(),
                                        selectionLabel),
                                Constraint.length(1))
                        .center(centerContent)
                        .bottom(text(" ?:help Space:toggle Esc/q:back").addClass("status-bar"), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("detail")
                .focusable()
                .onKeyEvent(event -> {
                    if (controller.isShowHelp()) {
                        controller.toggleHelp();
                        return EventResult.HANDLED;
                    }
                    if (event.isChar('?')) {
                        controller.toggleHelp();
                        return EventResult.HANDLED;
                    }
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
