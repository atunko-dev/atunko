package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Set;

@Requirements({"atunko:TUI_0001.14", "atunko:TUI_0001.21", "atunko:TUI_0001.24", "atunko:TUI_0002.2"})
public final class ConfirmRunView {

    private ConfirmRunView() {}

    public static Element render(TuiController controller) {
        if (controller.isShowOptions()) {
            return RecipeOptionsView.render(controller);
        }
        if (controller.isShowExport()) {
            return ExportConfigView.render(controller);
        }

        List<DisplayRow> displayRows = controller.runDisplayRows();
        Set<String> selected = controller.selectedRecipes();
        boolean hasRecipes = !displayRows.isEmpty();

        Element centerContent;
        if (controller.isShowHelp()) {
            centerContent = row(spacer(), HelpOverlay.render(HelpOverlay.RUN_DIALOG_HELP), spacer());
        } else if (hasRecipes) {
            Element projectInfo = buildProjectInfo(controller);
            centerContent = column(projectInfo, text(""), renderRecipeList(controller, displayRows, selected));
        } else {
            centerContent = column(
                    text(""),
                    text(" No recipes selected.").addClass("warning"),
                    text(" Use Space to select recipes, then press r to run."));
        }

        long selectedCount = displayRows.stream()
                .filter(r -> selected.contains(r.recipe().name()))
                .count();
        long totalCount = displayRows.size();
        String footer = hasRecipes
                ? selectedCount + "/" + totalCount
                        + " selected | o:options  f:flatten  F:flatten-all  x:export  ?:help  Esc:back"
                : "Esc:back";

        return column(dock().top(
                                row(
                                        text(" Run Recipes ").addClass("screen-title"),
                                        spacer(),
                                        text(selectedCount + "/" + totalCount + " selected ")
                                                .addClass("coverage-indicator")),
                                Constraint.length(1))
                        .center(centerContent)
                        .bottom(text(" " + footer).addClass("status-bar"), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("confirm-run")
                .focusable()
                .onKeyEvent(event -> handleKeyEvent(controller, hasRecipes, event))
                .onMouseEvent(event -> handleMouseEvent(controller, hasRecipes, event));
    }

    private static EventResult handleMouseEvent(TuiController controller, boolean hasRecipes, MouseEvent event) {
        if (controller.isShowHelp() || controller.isShowOptions() || controller.isShowExport()) {
            return EventResult.UNHANDLED;
        }
        if (!hasRecipes) {
            return EventResult.UNHANDLED;
        }
        if (event.kind() == MouseEventKind.SCROLL_UP) {
            controller.moveRunHighlightUp();
            return EventResult.HANDLED;
        }
        if (event.kind() == MouseEventKind.SCROLL_DOWN) {
            controller.moveRunHighlightDown();
            return EventResult.HANDLED;
        }
        if (event.isPress()) {
            int idx = controller.mouseRowToIndex(
                    event.y(), 1, controller.runDisplayRows().size());
            if (idx >= 0) {
                controller.setRunHighlightIndex(idx);
                if (event.isRightButton()) {
                    controller.toggleRunRecipe();
                }
                return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
    }

    @Requirements({"atunko:TUI_0002.2"})
    private static Element buildProjectInfo(TuiController controller) {
        if (controller.isWorkspaceMode()) {
            List<ProjectEntry> entries = controller.workspaceProjects();
            Element[] projectLines = entries.stream()
                    .map(e -> (Element) row(
                            text("  • ").addClass("detail-label"),
                            text(e.projectDir().getFileName().toString())))
                    .toArray(Element[]::new);
            Element[] headerAndProjects = new Element[projectLines.length + 1];
            headerAndProjects[0] = row(text("Projects: ").addClass("detail-label"));
            System.arraycopy(projectLines, 0, headerAndProjects, 1, projectLines.length);
            return column(headerAndProjects);
        }
        String projectPath =
                controller.projectDir().toAbsolutePath().normalize().toString();
        return row(text("Project: ").addClass("detail-label"), text(projectPath));
    }

    private static Element renderRecipeList(
            TuiController controller, List<DisplayRow> displayRows, Set<String> selected) {
        return RecipeListRenderer.renderRecipeList(
                displayRows,
                selected,
                controller.runExpandedRecipes(),
                Set.of(),
                Set.of(),
                controller.runHighlightIndex(),
                "Execution Order",
                RecipeListRenderer.RenderOptions.RUN_DIALOG,
                null,
                controller::applicability,
                controller.favoriteRecipes());
    }

    private static EventResult handleKeyEvent(
            TuiController controller, boolean hasRecipes, dev.tamboui.tui.event.KeyEvent event) {
        if (controller.isShowHelp()) {
            controller.toggleHelp();
            return EventResult.HANDLED;
        }
        if (event.isChar('?')) {
            controller.toggleHelp();
            return EventResult.HANDLED;
        }
        if (hasRecipes) {
            if (event.isDown() || event.isChar('j')) {
                controller.moveRunHighlightDown();
                return EventResult.HANDLED;
            }
            if (event.isUp() || event.isChar('k')) {
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
                controller.selectAllRun();
                return EventResult.HANDLED;
            }
            if (event.isChar('A')) {
                controller.deselectAllRun();
                return EventResult.HANDLED;
            }
            if (event.isRight() || event.isChar('e') || event.isChar('>')) {
                controller.expandRunRecipe();
                return EventResult.HANDLED;
            }
            if (event.isLeft() || event.isChar('c') || event.isChar('<')) {
                controller.collapseRunRecipe();
                return EventResult.HANDLED;
            }
            if (event.isChar('f')) {
                controller.flattenRunRecipe();
                return EventResult.HANDLED;
            }
            if (event.isChar('F')) {
                controller.flattenAllRunRecipes();
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
            if (event.isChar('o')) {
                List<DisplayRow> rows = controller.runDisplayRows();
                int idx = controller.runHighlightIndex();
                if (idx >= 0 && idx < rows.size() && !rows.get(idx).isSubRecipe()) {
                    controller.openOptions(rows.get(idx).recipe().name());
                }
                return EventResult.HANDLED;
            }
            if (event.isChar('x')) {
                controller.openExport();
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
