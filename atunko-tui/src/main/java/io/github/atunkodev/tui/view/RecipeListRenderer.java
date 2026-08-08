package io.github.atunkodev.tui.view;

import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.spacer;
import static dev.tamboui.toolkit.Toolkit.text;

import dev.tamboui.layout.Constraint;
import dev.tamboui.toolkit.element.Element;
import io.github.atunkodev.core.recipe.RecipeApplicability;
import io.github.atunkodev.core.recipe.RecipeInfo;
import io.github.atunkodev.tui.TuiController.DisplayRow;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import java.util.Set;

public final class RecipeListRenderer {

    /** Glyph prefixing the badge of a recipe that cannot act on the parsed source set. */
    public static final String INAPPLICABLE_GLYPH = "⊘";

    /** Glyph marking a favorite recipe after its display name. */
    public static final String FAVORITE_GLYPH = "*";

    public record RenderOptions(boolean showNumbering, boolean showTags, boolean dimUnselected) {
        public static final RenderOptions BROWSER = new RenderOptions(false, true, false);
        public static final RenderOptions RUN_DIALOG = new RenderOptions(true, false, true);
    }

    /** Supplies the applicability of a recipe against the current source set. */
    @FunctionalInterface
    public interface ApplicabilityLookup {
        RecipeApplicability applicabilityOf(RecipeInfo recipe);
    }

    /** Lookup for callers that do not care about applicability — every recipe renders unbadged. */
    public static final ApplicabilityLookup ALL_APPLICABLE = recipe -> RecipeApplicability.APPLICABLE;

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
        return renderRecipeList(
                displayRows,
                selectedRecipes,
                expandedRecipes,
                coveredRecipes,
                partialRecipes,
                highlightedIndex,
                title,
                options,
                constraint,
                ALL_APPLICABLE);
    }

    @Requirements({"atunko:TUI_0001.16", "atunko:TUI_0001.17", "atunko:TUI_0004"})
    public static Element renderRecipeList(
            List<DisplayRow> displayRows,
            Set<String> selectedRecipes,
            Set<String> expandedRecipes,
            Set<String> coveredRecipes,
            Set<String> partialRecipes,
            int highlightedIndex,
            String title,
            RenderOptions options,
            Constraint constraint,
            ApplicabilityLookup applicability) {
        return renderRecipeList(
                displayRows,
                selectedRecipes,
                expandedRecipes,
                coveredRecipes,
                partialRecipes,
                highlightedIndex,
                title,
                options,
                constraint,
                applicability,
                Set.of());
    }

    @Requirements({"atunko:TUI_0001.16", "atunko:TUI_0001.17", "atunko:TUI_0004", "atunko:TUI_0007.1"})
    public static Element renderRecipeList(
            List<DisplayRow> displayRows,
            Set<String> selectedRecipes,
            Set<String> expandedRecipes,
            Set<String> coveredRecipes,
            Set<String> partialRecipes,
            int highlightedIndex,
            String title,
            RenderOptions options,
            Constraint constraint,
            ApplicabilityLookup applicability,
            Set<String> favoriteRecipes) {
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
            if (r.isComposite() && !displayRow.isSubRecipe()) {
                long coveredCount = r.recipeList().stream()
                        .filter(sub -> coveredRecipes.contains(sub.name()) || selectedRecipes.contains(sub.name()))
                        .count();
                if (coveredCount > 0) {
                    displayName += " [" + coveredCount + "/" + r.recipeList().size() + "]";
                }
            }
            if (favoriteRecipes.contains(r.name())) {
                displayName += " " + FAVORITE_GLYPH;
            }
            RecipeApplicability recipeApplicability = applicability.applicabilityOf(r);
            var nameEl = resolveNameStyle(displayName, selected, partial, covered, options, recipeApplicability);

            boolean withTags = options.showTags() && !r.tags().isEmpty() && !displayRow.isSubRecipe();
            if (recipeApplicability.applicable()) {
                if (withTags) {
                    recipeList.add(row(prefixEl, nameEl, spacer(), tagsElement(r)));
                } else {
                    recipeList.add(row(prefixEl, nameEl));
                }
            } else {
                var badge = text("  " + INAPPLICABLE_GLYPH + " " + recipeApplicability.badgeLabel())
                        .addClass("inapplicable");
                if (withTags) {
                    recipeList.add(row(prefixEl, nameEl, badge, spacer(), tagsElement(r)));
                } else {
                    recipeList.add(row(prefixEl, nameEl, badge));
                }
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

    private static Element tagsElement(RecipeInfo recipe) {
        return text("  " + String.join(", ", recipe.tags())).addClass("tag");
    }

    private static Element resolveNameStyle(
            String displayName,
            boolean selected,
            boolean partial,
            boolean covered,
            RenderOptions options,
            RecipeApplicability applicability) {
        if (!applicability.applicable()) {
            return text(displayName).addClass("inapplicable");
        }
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
