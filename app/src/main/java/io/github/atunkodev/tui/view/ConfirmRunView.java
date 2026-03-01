package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.tui.TuiController;
import java.util.List;

public final class ConfirmRunView {

    private ConfirmRunView() {}

    public static Element render(TuiController controller) {
        List<String> recipes = List.copyOf(controller.selectedRecipes());
        boolean hasRecipes = !recipes.isEmpty();
        String projectPath =
                controller.projectDir().toAbsolutePath().normalize().toString();

        Element centerContent;
        if (hasRecipes) {
            centerContent = column(
                    row(text("Project: ").bold(), text(projectPath)),
                    text(""),
                    list(recipes)
                            .highlightColor(Color.LIGHT_CYAN)
                            .title("Selected Recipes")
                            .rounded());
        } else {
            centerContent = column(
                    text(""),
                    text(" No recipes selected.").bold().fg(Color.LIGHT_YELLOW),
                    text(" Use Space to select recipes, then press r to run."));
        }

        String footer = hasRecipes ? " r:run  d:dry-run  Esc:back" : " Esc:back";

        return column(dock().top(
                                row(
                                        text(" Run Recipes").bold().fg(Color.LIGHT_CYAN),
                                        spacer(),
                                        text(recipes.size() + " selected ")),
                                Constraint.length(1))
                        .center(centerContent)
                        .bottom(text(footer).dim(), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("confirm-run")
                .focusable()
                .onKeyEvent(event -> {
                    if (hasRecipes && event.isChar('r')) {
                        controller.runSelectedRecipes(false);
                        return EventResult.HANDLED;
                    }
                    if (hasRecipes && event.isChar('d')) {
                        controller.runSelectedRecipes(true);
                        return EventResult.HANDLED;
                    }
                    if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
                        controller.goBack();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }
}
