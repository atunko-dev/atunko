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
import io.github.atunkodev.core.config.ConfigExportService;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;

@Requirements({"atunko:TUI_0001.21", "atunko:TUI_0001.22", "atunko:TUI_0001.23"})
public final class ExportConfigView {

    private static final ConfigExportService EXPORT_SERVICE = new ConfigExportService();

    private ExportConfigView() {}

    public static Element render(TuiController controller) {
        RunConfig config = controller.buildRunConfig();
        TuiController.ExportFormat format = controller.exportFormat();
        ConfigExportService.ExportMode mode = controller.exportMode();

        String snippet = format == TuiController.ExportFormat.GRADLE
                ? EXPORT_SERVICE.exportToGradle(config, mode)
                : EXPORT_SERVICE.exportToMaven(config, mode);

        String formatLabel = format == TuiController.ExportFormat.GRADLE ? "Gradle" : "Maven";
        String modeLabel = mode == ConfigExportService.ExportMode.MINIMAL ? "Minimal" : "Full";

        return column(dock().top(
                                row(
                                        text(" Export Config ").addClass("screen-title"),
                                        spacer(),
                                        text(" " + formatLabel + " · " + modeLabel + " ")
                                                .addClass("detail-value")),
                                Constraint.length(1))
                        .center(markupTextArea(snippet)
                                .title(formatLabel + " snippet")
                                .showLineNumbers()
                                .scrollbar()
                                .wrapWord()
                                .constraint(Constraint.fill()))
                        .bottom(
                                text(" g:Gradle  m:Maven  Tab:Minimal/Full  Esc:close")
                                        .addClass("status-bar"),
                                Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("export-config")
                .focusable()
                .onKeyEvent(event -> handleKeyEvent(controller, event));
    }

    private static EventResult handleKeyEvent(TuiController controller, dev.tamboui.tui.event.KeyEvent event) {
        if (event.isChar('g')) {
            controller.setExportFormat(TuiController.ExportFormat.GRADLE);
            return EventResult.HANDLED;
        }
        if (event.isChar('m')) {
            controller.setExportFormat(TuiController.ExportFormat.MAVEN);
            return EventResult.HANDLED;
        }
        if (event.code() == dev.tamboui.tui.event.KeyCode.TAB) {
            controller.toggleExportMode();
            return EventResult.HANDLED;
        }
        if (event.isChar('q') || event.code() == dev.tamboui.tui.event.KeyCode.ESCAPE) {
            controller.closeExport();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}
