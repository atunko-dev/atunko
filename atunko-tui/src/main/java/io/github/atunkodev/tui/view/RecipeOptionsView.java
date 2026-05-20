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
import dev.tamboui.widgets.input.TextInputState;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.core.recipe.RecipeOptionInfo;
import io.github.atunkodev.tui.TuiController;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Requirements({"atunko:TUI_0001.24", "atunko:TUI_0001.25"})
public final class RecipeOptionsView {

    private static final TextInputState EDIT_STATE = new TextInputState();
    private static boolean editing = false;

    private RecipeOptionsView() {}

    public static Element render(TuiController controller) {
        String recipeName = controller.focusedRecipeForOptions();
        Optional<RecipeInfo> recipeOpt = controller.findRecipe(recipeName);

        List<RecipeOptionInfo> options = recipeOpt.map(RecipeInfo::options).orElse(List.of());
        Map<String, Object> stored = controller.getRecipeOptions(recipeName);

        String title = recipeOpt.map(RecipeInfo::displayName).orElse(recipeName);
        int focusIdx = controller.focusedOptionIndex();

        Element centerContent;
        if (options.isEmpty()) {
            centerContent = column(
                    text(""), text("  No configurable options for this recipe.").addClass("warning"));
        } else {
            centerContent = buildOptionsList(options, stored, focusIdx);
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
                .onKeyEvent(event -> handleKeyEvent(controller, options, stored, event));
    }

    private static Element buildOptionsList(List<RecipeOptionInfo> options, Map<String, Object> stored, int focusIdx) {
        Element[] rows = new Element[options.size()];
        for (int i = 0; i < options.size(); i++) {
            RecipeOptionInfo opt = options.get(i);
            boolean focused = i == focusIdx;
            Object currentVal = stored.get(opt.name());

            String typeTag = opt.required() ? "[" + opt.type() + "*]" : "[" + opt.type() + "]";
            String valDisplay = currentVal != null ? String.valueOf(currentVal) : "";
            if (focused && editing) {
                valDisplay = EDIT_STATE.text();
            }

            Element nameCell = text("  " + opt.displayName() + " ").addClass(focused ? "highlight" : "detail-label");
            Element typeCell = text(typeTag + " ").addClass("detail-value");
            Element valCell = focused && editing
                    ? textInput(EDIT_STATE).constraint(Constraint.fill())
                    : text(valDisplay.isEmpty() ? "(not set)" : valDisplay)
                            .addClass(currentVal != null ? "detail-value" : "detail-label");

            Element descRow = focused
                    ? row(text("  " + opt.description()).addClass("detail-label"))
                            .constraint(Constraint.fill())
                    : text("");

            rows[i] = column(row(nameCell, typeCell, spacer(), valCell), descRow);
        }
        return column(rows);
    }

    private static EventResult handleKeyEvent(
            TuiController controller,
            List<RecipeOptionInfo> options,
            Map<String, Object> stored,
            dev.tamboui.tui.event.KeyEvent event) {

        if (editing) {
            if (event.code() == KeyCode.ESCAPE) {
                editing = false;
                EDIT_STATE.clear();
                return EventResult.HANDLED;
            }
            if (event.isConfirm()) {
                int idx = controller.focusedOptionIndex();
                if (idx < options.size()) {
                    RecipeOptionInfo opt = options.get(idx);
                    String text = EDIT_STATE.text().trim();
                    if (text.isEmpty()) {
                        controller.clearRecipeOption(controller.focusedRecipeForOptions(), opt.name());
                    } else {
                        Object parsed = parseValue(opt.type(), text);
                        controller.setRecipeOption(controller.focusedRecipeForOptions(), opt.name(), parsed);
                    }
                }
                editing = false;
                EDIT_STATE.clear();
                return EventResult.HANDLED;
            }
            handleTextInputKey(EDIT_STATE, event);
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
                cycleBooleanValue(controller, opt);
            } else {
                Object cur = stored.get(opt.name());
                EDIT_STATE.setText(cur != null ? String.valueOf(cur) : "");
                editing = true;
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

    private static boolean isBooleanType(String type) {
        return "boolean".equalsIgnoreCase(type) || "Boolean".equals(type);
    }

    private static void cycleBooleanValue(TuiController controller, RecipeOptionInfo opt) {
        String recipe = controller.focusedRecipeForOptions();
        Object cur = controller.getRecipeOptions(recipe).get(opt.name());
        if (cur == null) {
            controller.setRecipeOption(recipe, opt.name(), Boolean.TRUE);
        } else if (Boolean.TRUE.equals(cur)) {
            controller.setRecipeOption(recipe, opt.name(), Boolean.FALSE);
        } else {
            controller.clearRecipeOption(recipe, opt.name());
        }
    }

    private static Object parseValue(String type, String text) {
        try {
            if ("int".equalsIgnoreCase(type) || "Integer".equals(type)) {
                return Integer.parseInt(text);
            }
            if ("long".equalsIgnoreCase(type) || "Long".equals(type)) {
                return Long.parseLong(text);
            }
        } catch (NumberFormatException ignored) {
            // Fall through to String
        }
        return text;
    }
}
