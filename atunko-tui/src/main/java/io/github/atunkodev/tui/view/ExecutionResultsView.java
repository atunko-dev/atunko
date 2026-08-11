package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import io.github.atunkodev.core.engine.ProjectExecutionResult;
import io.github.atunkodev.core.engine.WorkspaceExecutionResult;
import io.github.atunkodev.tui.TuiController;
import io.github.atunkodev.tui.shell.AtunkoBindings;
import io.github.atunkodev.tui.shell.KeyHint;
import io.github.atunkodev.tui.shell.TuiView;
import io.github.reqstool.annotations.Requirements;
import java.util.List;

@Requirements({"atunko:TUI_0001.8", "atunko:TUI_0001.9", "atunko:TUI_0002.4"})
public final class ExecutionResultsView implements TuiView {

    @Override
    public String id() {
        return "execution-results";
    }

    @Override
    public String title(TuiController controller) {
        if (controller.executionError().isPresent()) {
            return "Execution Failed";
        }
        if (controller.lastWorkspaceResult() != null) {
            return controller.lastRunWasDryRun() ? "Dry-Run Preview" : "Workspace Results";
        }
        return controller.lastRunWasDryRun() ? "Dry-Run Preview" : "Execution Results";
    }

    @Override
    public String status(TuiController c) {
        if (c.executionError().isPresent()) {
            return "execution failed | 0 file(s) changed";
        }
        if (c.lastWorkspaceResult() != null) {
            WorkspaceExecutionResult ws = c.lastWorkspaceResult();
            long failures = ws.failureCount();
            return ws.totalChanges() + " change(s)" + (failures > 0 ? ", " + failures + " failure(s)" : "");
        }
        return c.executionResult()
                .map(r -> r.changes().size() + " file(s) " + (c.lastRunWasDryRun() ? "would change" : "changed"))
                .orElse("no results");
    }

    @Override
    public List<KeyHint> keyHints(TuiController c) {
        boolean hasChanges =
                c.executionResult().map(r -> !r.changes().isEmpty()).orElse(false);
        return hasChanges
                ? AtunkoBindings.hintsFor(AtunkoBindings.MOVE, AtunkoBindings.OPEN_DIFF, AtunkoBindings.BACK)
                : AtunkoBindings.hintsFor(AtunkoBindings.BACK);
    }

    @Override
    public List<HelpOverlay.Section> helpSections() {
        return AtunkoBindings.helpSections(AtunkoBindings.MOVE, AtunkoBindings.OPEN_DIFF, AtunkoBindings.BACK);
    }

    @Override
    public EventResult handleKey(TuiController c, dev.tamboui.tui.event.KeyEvent event) {
        return handleKeyEvent(c, event);
    }

    @Override
    public EventResult handleMouse(TuiController c, MouseEvent event) {
        return handleMouseEvent(c, event);
    }

    @Override
    public StyledElement<?> renderContent(TuiController c) {
        if (c.executionError().isPresent()) {
            return list(List.of(c.executionError().get())).title("Error").addClass("panel", "error-message");
        }
        if (c.lastWorkspaceResult() != null) {
            List<String> rows = c.lastWorkspaceResult().results().stream()
                    .map(ExecutionResultsView::formatProjectRow)
                    .toList();
            return list(rows).title("Project Results").addClass("panel").autoScroll();
        }
        return c.executionResult()
                .<StyledElement<?>>map(result -> list(result.changes().stream()
                                .map(change -> change.path().toString())
                                .toList())
                        .selected(c.selectedFileIndex())
                        .title("Changed Files")
                        .addClass("panel")
                        .autoScroll())
                .orElseGet(() -> column(text("No results")));
    }

    private static String formatProjectRow(ProjectExecutionResult r) {
        String projectName = r.entry().projectDir().getFileName().toString();
        if (r.succeeded()) {
            int changes = r.result() != null ? r.result().changes().size() : 0;
            return String.format("%-40s  %4d change(s)  PASS", projectName, changes);
        }
        return String.format("%-40s     -           FAIL", projectName);
    }

    private static EventResult handleMouseEvent(TuiController controller, MouseEvent event) {
        if (event.kind() == MouseEventKind.SCROLL_UP) {
            controller.moveFileUp();
            return EventResult.HANDLED;
        }
        if (event.kind() == MouseEventKind.SCROLL_DOWN) {
            controller.moveFileDown();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private static EventResult handleKeyEvent(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        boolean hasChanges =
                controller.executionResult().map(r -> !r.changes().isEmpty()).orElse(false);
        if (hasChanges) {
            if (event.isDown()) {
                controller.moveFileDown();
                return EventResult.HANDLED;
            }
            if (event.isUp()) {
                controller.moveFileUp();
                return EventResult.HANDLED;
            }
            if (event.isConfirm()) {
                controller.openFileDiff();
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
