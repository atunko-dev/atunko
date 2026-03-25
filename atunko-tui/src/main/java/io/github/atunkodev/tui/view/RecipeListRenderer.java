package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Set;

public final class RecipeListRenderer {

    public record RenderOptions(boolean showNumbering, boolean showTags, boolean dimUnselected) {
        public static final RenderOptions BROWSER = new RenderOptions(false, true, false);
        public static final RenderOptions RUN_DIALOG = new RenderOptions(true, false, true);
    }

    private RecipeListRenderer() {}

    @Requirements({"atunko:TUI_0001.16", "atunko:TUI_0001.17"})
    public static Element renderRecipeList(
            List<DisplayRow> displayRows,
            Set<String> selectedRecipes,
            Set<String> expandedRecipes,
            Set<String> coveredRecipes,
            Set<String> partialRecipes,
            int highlightedIndex,
            String title,
            RenderOptions options,
            Constraint constraint) {
        var recipeList = list().addClass("list-item");

        int parentIndex = 0;
        for (DisplayRow displayRow : displayRows) {
            RecipeInfo r = displayRow.recipe();
            boolean selected = selectedRecipes.contains(r.name());
            boolean partial = partialRecipes.contains(r.name());
            boolean covered = coveredRecipes.contains(r.name());
            boolean expanded = expandedRecipes.contains(r.name());
            String check = resolveCheckbox(selected, partial, covered);
            String indicator = r.isComposite() ? (expanded ? "\u25bc " : "\u25b6 ") : "  ";

            String prefix;
            if (displayRow.isSubRecipe()) {
                String indent = "  ".repeat(displayRow.depth());
                prefix = (options.showNumbering() ? "    " : "") + indent + check + indicator;
            } else {
                parentIndex++;
                prefix = options.showNumbering()
                        ? String.format("%2d. %s%s", parentIndex, check, indicator)
                        : check + indicator;
            }

            var prefixEl = resolvePrefixStyle(prefix, selected, partial, covered);
            String displayName = cleanDisplayName(r.displayName());
            var nameEl = resolveNameStyle(displayName, selected, partial, covered, options);

            if (options.showTags() && !r.tags().isEmpty() && !displayRow.isSubRecipe()) {
                var tags = text("  " + String.join(", ", r.tags())).addClass("tag");
                recipeList.add(row(prefixEl, nameEl, spacer(), tags));
            } else {
                recipeList.add(row(prefixEl, nameEl));
            }
        }

        var result = recipeList
                .selected(highlightedIndex)
                .title(title)
                .addClass("panel")
                .autoScroll();

        if (constraint != null) {
            return result.constraint(constraint);
        }
        return result;
    }

    private static String resolveCheckbox(boolean selected, boolean partial, boolean covered) {
        if (selected || covered) {
            return "[x] ";
        }
        if (partial) {
            return "[~] ";
        }
        return "[ ] ";
    }

    private static Element resolvePrefixStyle(String prefix, boolean selected, boolean partial, boolean covered) {
        if (selected || covered) {
            return text(prefix).addClass("selected");
        }
        if (partial) {
            return text(prefix).addClass("partial");
        }
        return text(prefix).addClass("unselected");
    }

    private static Element resolveNameStyle(
            String displayName, boolean selected, boolean partial, boolean covered, RenderOptions options) {
        if (options.dimUnselected() && !selected && !covered) {
            return text(displayName).addClass("unselected");
        }
        if (!selected && !covered && !partial) {
            return text(displayName).addClass("unselected");
        }
        return text(displayName);
    }

    public static String cleanDisplayName(String displayName) {
        return displayName.replace("`", "");
    }
}
