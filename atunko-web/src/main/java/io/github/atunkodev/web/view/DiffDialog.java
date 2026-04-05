package io.github.atunkodev.web.view;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.github.atunkodev.core.engine.ExecutionResult;
import io.github.atunkodev.core.engine.FileChange;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * GitHub/GitLab-style diff dialog powered by diff2html.
 *
 * <p>Left panel: filterable file list. Right panel: side-by-side diff for the selected file.
 */
@NpmPackage(value = "diff2html", version = "3.4.56")
@JsModule("./diff2html-init.js")
@CssImport("diff2html/bundles/css/diff2html.min.css")
public class DiffDialog extends Dialog {

    private static final int CONTEXT_LINES = 3;

    private final List<FileChange> changes;
    private FileChange selectedChange;
    private String filterText = "";

    private final Div fileListContainer = new Div();
    private final Div diffContainer = new Div();
    private final String diffContainerId =
            "diff-" + UUID.randomUUID().toString().replace("-", "");

    public DiffDialog(ExecutionResult result, boolean dryRun) {
        this.changes = result.changes();

        setHeaderTitle(
                (dryRun ? "Dry Run Preview" : "Execution Results") + " — " + changes.size() + " file(s) changed");
        setWidth("90vw");
        setHeight("85vh");

        if (changes.isEmpty()) {
            add(new Span("No changes produced."));
        } else {
            HorizontalLayout body = buildBody();
            body.setSizeFull();
            add(body);
            selectChange(changes.get(0));
        }

        getFooter().add(new Button("Close", e -> close()));
    }

    // --- Layout ---

    private HorizontalLayout buildBody() {
        HorizontalLayout body = new HorizontalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.add(buildFilePanel());
        body.addAndExpand(buildDiffPanel());
        return body;
    }

    private VerticalLayout buildFilePanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setWidth("280px");
        panel.setMinWidth("280px");
        panel.setMaxWidth("280px");
        panel.setHeightFull();
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.getStyle().set("border-right", "1px solid var(--lumo-contrast-20pct)");

        TextField filter = new TextField();
        filter.setPlaceholder("Filter files...");
        filter.setWidthFull();
        filter.setValueChangeMode(ValueChangeMode.EAGER);
        filter.addValueChangeListener(e -> {
            filterText = e.getValue();
            refreshFileList();
        });

        fileListContainer.getStyle().set("overflow-y", "auto");

        panel.add(filter);
        panel.addAndExpand(fileListContainer);
        return panel;
    }

    private Div buildDiffPanel() {
        diffContainer.setId(diffContainerId);
        diffContainer.getStyle().set("overflow", "auto").set("height", "100%");
        return diffContainer;
    }

    // --- File list ---

    private void refreshFileList() {
        fileListContainer.removeAll();
        changes.stream()
                .filter(c -> filterText.isBlank()
                        || c.path().toString().toLowerCase().contains(filterText.toLowerCase()))
                .forEach(c -> fileListContainer.add(buildFileItem(c)));
    }

    private Div buildFileItem(FileChange change) {
        boolean selected = change.equals(selectedChange);
        String fileName = change.path().getFileName().toString();
        String dir =
                change.path().getParent() != null ? change.path().getParent().toString() : "";

        Div item = new Div();
        item.getStyle()
                .set("padding", "6px 12px")
                .set("cursor", "pointer")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", selected ? "3px solid var(--lumo-primary-color)" : "3px solid transparent")
                .set("background-color", selected ? "var(--lumo-primary-color-10pct)" : "transparent");

        if (!dir.isEmpty()) {
            Div dirLabel = new Div();
            dirLabel.setText(dir);
            dirLabel.getStyle()
                    .set("font-size", "11px")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("white-space", "nowrap")
                    .set("overflow", "hidden")
                    .set("text-overflow", "ellipsis");
            item.add(dirLabel);
        }

        Div nameLabel = new Div();
        nameLabel.setText(fileName);
        nameLabel
                .getStyle()
                .set("font-family", "monospace")
                .set("font-size", "12px")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        item.add(nameLabel);

        item.addClickListener(e -> selectChange(change));
        return item;
    }

    // --- Diff rendering ---

    private void selectChange(FileChange change) {
        selectedChange = change;
        refreshFileList();
        String diffString = buildUnifiedDiff(change);
        UI.getCurrent()
                .getPage()
                .executeJs("window.atunkoDiff.render($0, $1, $2)", diffContainerId, diffString, "side-by-side");
    }

    static String buildUnifiedDiff(FileChange change) {
        List<String> before = splitLines(change.before());
        List<String> after = splitLines(change.after());

        String fromName = change.before() == null ? "/dev/null" : "a/" + change.path();
        String toName = change.after() == null ? "/dev/null" : "b/" + change.path();

        Patch<String> patch = DiffUtils.diff(before, after);
        List<String> lines = UnifiedDiffUtils.generateUnifiedDiff(fromName, toName, before, patch, CONTEXT_LINES);
        return String.join("\n", lines);
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(text.replace("\r\n", "\n").split("\n", -1));
    }
}
