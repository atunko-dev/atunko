package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import io.github.atunkodev.core.project.ProjectEntry;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.atunkodev.tui.shell.AtunkoBindings;
import io.github.atunkodev.tui.shell.KeyHint;
import io.github.atunkodev.tui.shell.TuiView;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Set;

@Requirements({"atunko:TUI_0001.14", "atunko:TUI_0001.21", "atunko:TUI_0001.24", "atunko:TUI_0002.2"})
public final class ConfirmRunView implements TuiView {

    @Override
    public String id() {
        return "confirm-run";
    }

    @Override
    public String title(TuiController controller) {
        return "Run Recipes";
    }

    @Override
    public String status(TuiController controller) {
        List<DisplayRow> rows = controller.runDisplayRows();
        if (rows.isEmpty()) {
            return "no recipes selected";
        }
        Set<String> selected = controller.selectedRecipes();
        long selectedCount =
                rows.stream().filter(r -> selected.contains(r.recipe().name())).count();
        return selectedCount + "/" + rows.size() + " selected";
    }

    @Override
    public List<KeyHint> keyHints(TuiController controller) {
        if (controller.runDisplayRows().isEmpty()) {
            return AtunkoBindings.hintsFor(AtunkoBindings.BACK);
        }
        return AtunkoBindings.hintsFor(
                AtunkoBindings.MOVE,
                AtunkoBindings.OPEN_OPTIONS,
                AtunkoBindings.FLATTEN,
                AtunkoBindings.EXPORT,
                AtunkoBindings.HELP,
                AtunkoBindings.BACK);
    }

    @Override
    public List<HelpOverlay.Section> helpSections() {
        return HelpOverlay.RUN_DIALOG_HELP;
    }

    @Override
    public EventResult handleKey(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        return handleKeyEvent(controller, !controller.runDisplayRows().isEmpty(), event);
    }

    @Override
    public EventResult handleMouse(TuiController controller, MouseEvent event) {
        return handleMouseEvent(controller, !controller.runDisplayRows().isEmpty(), event);
    }

    @Override
    public StyledElement<?> renderContent(TuiController controller) {
        if (controller.isShowExport()) {
            return (StyledElement<?>) ExportConfigView.render(controller);
        }

        List<DisplayRow> displayRows = controller.runDisplayRows();
        Set<String> selected = controller.selectedRecipes();
        if (displayRows.isEmpty()) {
            return column(
                    text(""),
                    text(" No recipes selected.").addClass("warning"),
                    text(" Use Space to select recipes, then press r to run."));
        }
        return column(buildProjectInfo(controller), text(""), renderRecipeList(controller, displayRows, selected));
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
            if (event.isChar('l')) {
                controller.flattenRunRecipe();
                return EventResult.HANDLED;
            }
            if (event.isChar('L')) {
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
