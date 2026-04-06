package io.github.atunkodev.web.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.reqstool.annotations.Requirements;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Requirements({"atunko:WEB_0001.10"})
public class RunOrderDialog extends Dialog {

    private final List<RecipeInfo> orderedRecipes;
    private final List<RecipeInfo> originalRecipes;
    private final Grid<RecipeInfo> orderGrid = new Grid<>();
    private final Consumer<List<RecipeInfo>> onConfirm;
    private final Runnable onCancel;
    private boolean flattened = false;

    public RunOrderDialog(
            Set<RecipeInfo> selected, boolean dryRun, Consumer<List<RecipeInfo>> onConfirm, Runnable onCancel) {
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.originalRecipes = selected.stream()
                .sorted((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()))
                .toList();
        this.orderedRecipes = new ArrayList<>(originalRecipes);

        setHeaderTitle(dryRun ? "Review Dry Run Order" : "Review Execution Order");
        setWidth("600px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        orderGrid.setItems(orderedRecipes);
        orderGrid
                .addColumn(r -> orderedRecipes.indexOf(r) + 1)
                .setHeader("#")
                .setWidth("50px")
                .setFlexGrow(0);
        orderGrid.addColumn(RecipeInfo::displayName).setHeader("Recipe");
        orderGrid
                .addColumn(r -> r.isComposite() ? "composite" : "leaf")
                .setHeader("Type")
                .setWidth("90px");
        orderGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        orderGrid.setWidthFull();

        Button upButton = new Button(VaadinIcon.ARROW_UP.create(), e -> moveSelected(-1));
        upButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button downButton = new Button(VaadinIcon.ARROW_DOWN.create(), e -> moveSelected(1));
        downButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Checkbox flattenToggle = new Checkbox("Flatten composites");
        flattenToggle.addValueChangeListener(e -> {
            flattened = e.getValue();
            if (flattened) {
                orderedRecipes.clear();
                orderedRecipes.addAll(flatten(originalRecipes));
            } else {
                orderedRecipes.clear();
                orderedRecipes.addAll(originalRecipes);
            }
            orderGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout controls = new HorizontalLayout(upButton, downButton, flattenToggle);
        controls.setAlignItems(HorizontalLayout.Alignment.CENTER);

        VerticalLayout content = new VerticalLayout(controls, orderGrid);
        content.setSizeFull();
        content.setPadding(false);
        add(content);

        Button cancelButton = new Button("Cancel", VaadinIcon.CLOSE.create(), e -> {
            close();
            onCancel.run();
        });
        cancelButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button confirmButton = new Button("Confirm", VaadinIcon.CHECK.create(), e -> {
            close();
            onConfirm.accept(List.copyOf(orderedRecipes));
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        getFooter().add(cancelButton, confirmButton);
    }

    private void moveSelected(int direction) {
        orderGrid.getSelectedItems().stream().findFirst().ifPresent(selected -> {
            int index = orderedRecipes.indexOf(selected);
            int newIndex = index + direction;
            if (newIndex >= 0 && newIndex < orderedRecipes.size()) {
                orderedRecipes.set(index, orderedRecipes.get(newIndex));
                orderedRecipes.set(newIndex, selected);
                orderGrid.getDataProvider().refreshAll();
                orderGrid.select(selected);
            }
        });
    }

    static List<RecipeInfo> flatten(List<RecipeInfo> recipes) {
        LinkedHashSet<RecipeInfo> result = new LinkedHashSet<>();
        for (RecipeInfo recipe : recipes) {
            flattenRecursive(recipe, result, new java.util.HashSet<>());
        }
        return new ArrayList<>(result);
    }

    private static void flattenRecursive(RecipeInfo recipe, LinkedHashSet<RecipeInfo> result, Set<RecipeInfo> visited) {
        if (!visited.add(recipe)) {
            return;
        }
        if (recipe.isComposite()) {
            for (RecipeInfo child : recipe.recipeList()) {
                flattenRecursive(child, result, visited);
            }
        } else {
            result.add(recipe);
        }
    }

    // --- Testability hooks ---

    List<RecipeInfo> getOrderedRecipes() {
        return List.copyOf(orderedRecipes);
    }

    boolean isFlattened() {
        return flattened;
    }
}
