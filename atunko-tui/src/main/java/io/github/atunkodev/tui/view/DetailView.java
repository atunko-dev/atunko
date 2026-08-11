package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.markdown.MarkdownElement.markdown;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.recipe.RecipeApplicability;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.shell.AtunkoBindings;
import io.github.atunkodev.tui.shell.KeyHint;
import io.github.atunkodev.tui.shell.TuiView;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"atunko:TUI_0001.4", "atunko:TUI_0001.7", "atunko:TUI_0001.20", "atunko:TUI_0004.1"})
public final class DetailView implements TuiView {

    @Override
    public String id() {
        return "detail";
    }

    @Override
    public String title(TuiController controller) {
        return "Recipe Detail";
    }

    @Override
    public String status(TuiController controller) {
        return controller
                .highlightedRecipe()
                .map(recipe -> controller.selectedRecipes().contains(recipe.name()) ? "selected" : "not selected")
                .orElse("no recipe selected");
    }

    @Override
    public List<KeyHint> keyHints(TuiController controller) {
        return AtunkoBindings.hintsFor(AtunkoBindings.TOGGLE_SELECTION, AtunkoBindings.HELP, AtunkoBindings.BACK);
    }

    @Override
    public List<HelpOverlay.Section> helpSections() {
        return AtunkoBindings.helpSections(AtunkoBindings.TOGGLE_SELECTION, AtunkoBindings.HELP, AtunkoBindings.BACK);
    }

    @Override
    public StyledElement<?> renderContent(TuiController controller) {
        return controller
                .highlightedRecipe()
                .<StyledElement<?>>map(recipe -> renderRecipeDetail(controller, recipe))
                .orElseGet(() -> column(text("No recipe selected")));
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
        if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            controller.goBack();
            return EventResult.HANDLED;
        }
        if (event.isChar(' ')) {
            controller.toggleSelection();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    // Name(1) + DisplayName(1) + blank(1) + Tags(1) = 4 base rows
    // composite:     blank(1) + "Recipe List:"(1) + N sub-recipes
    // parents:       blank(1) + "Included in:"(1)
    // applicability: "Applicability:"(1)
    // +1 buffer so a wrapping name/tag line doesn't push the description off-screen
    static int metadataLineCount(RecipeInfo recipe, int parentCount) {
        return metadataLineCount(recipe, parentCount, false);
    }

    static int metadataLineCount(RecipeInfo recipe, int parentCount, boolean hasApplicabilityLine) {
        return 4
                + (recipe.isComposite() ? 2 + recipe.recipeList().size() : 0)
                + (parentCount > 0 ? 2 : 0)
                + (hasApplicabilityLine ? 1 : 0)
                + 1;
    }

    private static StyledElement<?> renderRecipeDetail(TuiController controller, RecipeInfo recipe) {
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

        RecipeApplicability applicability = controller.applicability(recipe);
        if (!applicability.applicable()) {
            metadataContent.add(row(
                    text("Applicability: ").addClass("detail-label"),
                    text(applicability.reason()).addClass("inapplicable")));
        }

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

        String description = recipe.description() != null ? recipe.description() : "*(no description)*";

        return panel(
                        "Recipe Detail",
                        dock().top(
                                        metadataContent,
                                        Constraint.length(
                                                metadataLineCount(recipe, parents.size(), !applicability.applicable())))
                                .center(markdown(description))
                                .constraint(Constraint.fill()))
                .addClass("panel");
    }
}
