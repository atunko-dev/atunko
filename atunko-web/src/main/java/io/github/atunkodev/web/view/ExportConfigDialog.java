package io.github.atunkodev.web.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import io.github.atunkodev.core.config.ConfigExportService;
import io.github.atunkodev.core.config.RecipeEntry;
import io.github.atunkodev.core.config.RunConfig;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Set;

@Requirements({"atunko:WEB_0001.17"})
public class ExportConfigDialog extends Dialog {

    enum ExportFormat {
        GRADLE,
        MAVEN
    }

    private final ConfigExportService exportService = new ConfigExportService();
    private final Set<RecipeInfo> selectedRecipes;
    final RadioButtonGroup<ExportFormat> formatSelector;
    final TextArea snippetArea;
    final Button copyButton;

    public ExportConfigDialog(Set<RecipeInfo> selectedRecipes) {
        this.selectedRecipes = Set.copyOf(selectedRecipes);

        setHeaderTitle("Export Configuration");
        setWidth("600px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        formatSelector = new RadioButtonGroup<>();
        formatSelector.setItems(ExportFormat.GRADLE, ExportFormat.MAVEN);
        formatSelector.setValue(ExportFormat.GRADLE);
        formatSelector.setItemLabelGenerator(f -> f == ExportFormat.GRADLE ? "Gradle" : "Maven");

        snippetArea = new TextArea();
        snippetArea.setWidthFull();
        snippetArea.setReadOnly(true);
        snippetArea.setMinHeight("200px");

        copyButton = new Button("Copy to Clipboard", VaadinIcon.COPY.create());
        copyButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        if (this.selectedRecipes.isEmpty()) {
            snippetArea.setValue("No recipes selected.");
            snippetArea.setEnabled(false);
            copyButton.setEnabled(false);
            formatSelector.setEnabled(false);
        } else {
            updateSnippet();
            formatSelector.addValueChangeListener(e -> updateSnippet());
            copyButton.addClickListener(e -> {
                String text = snippetArea.getValue();
                UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", text);
                Notification.show("Copied to clipboard", 2000, Notification.Position.BOTTOM_START);
            });
        }

        add(new VerticalLayout(formatSelector, snippetArea));

        Button closeButton = new Button("Close", VaadinIcon.CLOSE.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        closeButton.addClickListener(e -> close());

        getFooter().add(new HorizontalLayout(copyButton, closeButton));
    }

    private void updateSnippet() {
        List<RecipeEntry> entries =
                selectedRecipes.stream().map(r -> new RecipeEntry(r.name())).toList();
        RunConfig config = new RunConfig(entries);
        snippetArea.setValue(
                formatSelector.getValue() == ExportFormat.GRADLE
                        ? exportService.exportToGradle(config)
                        : exportService.exportToMaven(config));
    }
}
