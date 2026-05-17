package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.Arrays;
import java.util.List;

@Requirements({"atunko:TUI_0001.8", "atunko:TUI_0001.9"})
public final class FileDiffView {

    private FileDiffView() {}

    public static Element render(TuiController controller) {
        return controller
                .executionResult()
                .map(result -> {
                    List<FileChange> changes = result.changes();
                    int idx = controller.selectedFileIndex();
                    if (changes.isEmpty() || idx >= changes.size()) {
                        return (Element) text("No file selected");
                    }
                    return renderDiff(controller, changes.get(idx));
                })
                .orElse(text("No results"));
    }

    private static Element renderDiff(TuiController controller, FileChange change) {
        String filename = change.path().toString();

        List<String> beforeLines =
                change.before() != null ? Arrays.asList(change.before().split("\n", -1)) : List.of("<empty>");
        List<String> afterLines =
                change.after() != null ? Arrays.asList(change.after().split("\n", -1)) : List.of("<empty>");

        var titleElement = controller.lastRunWasDryRun()
                ? text(" File Diff ").addClass("screen-title", "dryrun-mode")
                : text(" File Diff ").addClass("screen-title", "success-mode");

        return column(dock().top(
                                row(
                                        titleElement,
                                        spacer(),
                                        text(" " + filename + " ").addClass("detail-value")),
                                Constraint.length(1))
                        .center(row(
                                list(beforeLines)
                                        .title("Before")
                                        .addClass("panel")
                                        .autoScroll()
                                        .constraint(Constraint.fill()),
                                list(afterLines)
                                        .title("After")
                                        .addClass("panel")
                                        .autoScroll()
                                        .constraint(Constraint.fill())))
                        .bottom(text(" Esc/q:back").addClass("status-bar"), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("file-diff")
                .focusable()
                .onKeyEvent(event -> {
                    if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
                        controller.returnFromFileDiff();
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }
}
