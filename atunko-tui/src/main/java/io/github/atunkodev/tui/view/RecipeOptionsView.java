package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.dock;
import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.MouseEventKind;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.RecipeOptionInfo;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Requirements({"atunko:TUI_0001.24", "atunko:TUI_0001.25"})
public final class RecipeOptionsView {

    private RecipeOptionsView() {}

    public static Element render(TuiController controller) {
        String recipeName = controller.focusedRecipeForOptions();
        Optional<RecipeInfo> recipeOpt = controller.findRecipe(recipeName);

        List<RecipeOptionInfo> options = recipeOpt.map(RecipeInfo::options).orElse(List.of());
        Map<String, Object> stored = controller.getRecipeOptions(recipeName);

        String title = recipeOpt
                .map(RecipeInfo::displayName)
                .filter(dn -> dn != null && !dn.isBlank())
                .orElse(recipeName);
        int focusIdx = controller.focusedOptionIndex();
        boolean editing = controller.isOptionsEditing();

        Element centerContent;
        if (options.isEmpty()) {
            centerContent = column(
                    text(""), text("  No configurable options for this recipe.").addClass("warning"));
        } else {
            centerContent = buildOptionsList(options, stored, focusIdx, editing, controller);
        }

        String statusBar = editing ? " Enter:save  Esc:cancel edit" : " j/k:navigate  Enter:edit  Del:clear  Esc:close";

        return column(dock().top(
                                row(
                                        text(" Recipe Options ").addClass("screen-title"),
                                        spacer(),
                                        text(" " + title + " ").addClass("detail-value")),
                                Constraint.length(1))
                        .center(centerContent)
                        .bottom(text(" " + statusBar).addClass("status-bar"), Constraint.length(1))
                        .constraint(Constraint.fill()))
                .id("recipe-options")
                .focusable()
                .onKeyEvent(event -> handleKeyEvent(controller, options, event))
                .onMouseEvent(event -> {
                    if (controller.isOptionsEditing()) {
                        return EventResult.UNHANDLED;
                    }
                    if (event.kind() == MouseEventKind.SCROLL_UP) {
                        controller.moveOptionHighlightUp(options.size());
                        return EventResult.HANDLED;
                    }
                    if (event.kind() == MouseEventKind.SCROLL_DOWN) {
                        controller.moveOptionHighlightDown(options.size());
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });
    }

    private static Element buildOptionsList(
            List<RecipeOptionInfo> options,
            Map<String, Object> stored,
            int focusIdx,
            boolean editing,
            TuiController controller) {
        Element[] rows = new Element[options.size()];
        for (int i = 0; i < options.size(); i++) {
            RecipeOptionInfo opt = options.get(i);
            boolean focused = i == focusIdx;
            Object currentVal = stored.get(opt.name());

            String typeTag = opt.required() ? "[" + opt.type() + "*]" : "[" + opt.type() + "]";
            Element nameCell = text("  " + opt.displayName() + " ").addClass(focused ? "highlight" : "detail-label");
            Element typeCell = text(typeTag + " ").addClass("detail-value");
            Element valCell = focused && editing
                    ? textInput(controller.optionsEditState()).constraint(Constraint.fill())
                    : text(currentVal != null ? String.valueOf(currentVal) : "(not set)")
                            .addClass(currentVal != null ? "detail-value" : "detail-label");

            Element descRow;
            if (focused) {
                var desc = column(row(text("  " + opt.description()).addClass("detail-label")));
                if (opt.example() != null) {
                    desc.add(row(
                            text("  Example: ").addClass("detail-label"),
                            text(opt.example()).addClass("detail-value")));
                }
                if (opt.valid() != null && !opt.valid().isEmpty()) {
                    desc.add(row(
                            text("  Valid:   ").addClass("detail-label"),
                            text(String.join(", ", opt.valid())).addClass("detail-value")));
                }
                descRow = desc.constraint(Constraint.fill());
            } else {
                descRow = text("");
            }

            rows[i] = column(row(nameCell, typeCell, spacer(), valCell), descRow);
        }
        return column(rows);
    }

    private static EventResult handleKeyEvent(
            TuiController controller, List<RecipeOptionInfo> options, dev.tamboui.tui.event.KeyEvent event) {

        if (controller.isOptionsEditing()) {
            if (event.code() == KeyCode.ESCAPE) {
                controller.stopOptionsEditing();
                return EventResult.HANDLED;
            }
            if (event.isConfirm()) {
                int idx = controller.focusedOptionIndex();
                if (idx < options.size()) {
                    RecipeOptionInfo opt = options.get(idx);
                    String inputText = controller.optionsEditState().text().trim();
                    if (inputText.isEmpty()) {
                        controller.clearRecipeOption(controller.focusedRecipeForOptions(), opt.name());
                    } else {
                        Object parsed = parseValue(opt.type(), inputText);
                        controller.setRecipeOption(controller.focusedRecipeForOptions(), opt.name(), parsed);
                    }
                }
                controller.stopOptionsEditing();
                return EventResult.HANDLED;
            }
            handleTextInputKey(controller.optionsEditState(), event);
            return EventResult.HANDLED;
        }

        if (event.isDown() || event.isChar('j')) {
            controller.moveOptionHighlightDown(options.size());
            return EventResult.HANDLED;
        }
        if (event.isUp() || event.isChar('k')) {
            controller.moveOptionHighlightUp(options.size());
            return EventResult.HANDLED;
        }
        if (!options.isEmpty() && event.isConfirm()) {
            int idx = controller.focusedOptionIndex();
            RecipeOptionInfo opt = options.get(idx);
            if (isBooleanType(opt.type())) {
                controller.cycleRecipeOptionBoolean(controller.focusedRecipeForOptions(), opt.name());
            } else {
                Object cur = controller
                        .getRecipeOptions(controller.focusedRecipeForOptions())
                        .get(opt.name());
                controller.optionsEditState().setText(cur != null ? String.valueOf(cur) : "");
                controller.startOptionsEditing();
            }
            return EventResult.HANDLED;
        }
        if (!options.isEmpty() && (event.code() == KeyCode.DELETE || event.code() == KeyCode.BACKSPACE)) {
            int idx = controller.focusedOptionIndex();
            if (idx < options.size()) {
                controller.clearRecipeOption(
                        controller.focusedRecipeForOptions(), options.get(idx).name());
            }
            return EventResult.HANDLED;
        }
        if (event.isChar('q') || event.code() == KeyCode.ESCAPE) {
            controller.closeOptions();
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    // TODO: move isBooleanType and parseValue to atunko-core when atunko-web needs option editing
    static boolean isBooleanType(String type) {
        return "boolean".equalsIgnoreCase(type) || "java.lang.Boolean".equalsIgnoreCase(type);
    }

    static Object parseValue(String type, String text) {
        try {
            if ("int".equalsIgnoreCase(type)
                    || "Integer".equalsIgnoreCase(type)
                    || "java.lang.Integer".equalsIgnoreCase(type)) {
                return Integer.parseInt(text);
            }
            if ("long".equalsIgnoreCase(type)
                    || "Long".equalsIgnoreCase(type)
                    || "java.lang.Long".equalsIgnoreCase(type)) {
                return Long.parseLong(text);
            }
        } catch (NumberFormatException ignored) {
            // Fall through to String
        }
        return text;
    }
}
