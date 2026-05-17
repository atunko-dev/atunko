package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.markupTextArea;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import io.github.atunkodev.core.engine.FileChange;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
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
        String before = change.before() != null ? change.before() : "<empty>";
        String after = change.after() != null ? change.after() : "<empty>";

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
                                markupTextArea(before)
                                        .title("Before")
                                        .showLineNumbers()
                                        .scrollbar()
                                        .wrapWord()
                                        .constraint(Constraint.fill()),
                                markupTextArea(after)
                                        .title("After")
                                        .showLineNumbers()
                                        .scrollbar()
                                        .wrapWord()
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
