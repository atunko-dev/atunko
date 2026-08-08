package io.github.atunkodev.core.recipe;

import io.github.atunkodev.core.project.SourceCapability;
import io.github.reqstool.annotations.Requirements;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Determines whether a recipe can act on a parsed source set, so that the UIs can tell a genuinely clean project apart
 * from a recipe that never had a chance to act.
 *
 * <p>Detection is by recipe-name prefix: every {@code org.openrewrite.maven.*} recipe needs a Maven build model and
 * every {@code org.openrewrite.gradle.*} recipe needs a Gradle build model. Any other recipe is assumed applicable.
 * A composite recipe is applicable when at least one of its transitive sub-recipes is applicable.
 *
 * <p>Results are cached per capability set; the cache is unbounded but bounded in practice by the number of distinct
 * capability sets in a session (one per project).
 */
@Requirements({"atunko:CORE_0015", "atunko:CORE_0015.2", "atunko:CORE_0015.3"})
public class RecipeApplicabilityService {

    private static final Map<String, SourceCapability> REQUIRED_BY_PREFIX = Map.of(
            "org.openrewrite.maven.", SourceCapability.MAVEN,
            "org.openrewrite.gradle.", SourceCapability.GRADLE);

    private static final Map<SourceCapability, String> MISSING_REASONS = Map.of(
            SourceCapability.MAVEN,
                    "Requires Maven build model — pom.xml is not parsed as Maven, so this recipe cannot act",
            SourceCapability.GRADLE,
                    "Requires Gradle build model — Gradle build files are not parsed, so this recipe cannot act");

    private final Map<Set<SourceCapability>, Map<String, RecipeApplicability>> cache = new ConcurrentHashMap<>();

    /**
     * Returns whether {@code recipe} can act on a source set providing {@code capabilities}, with a human-readable
     * reason when it cannot.
     */
    @Requirements({"atunko:CORE_0015"})
    public RecipeApplicability applicability(RecipeInfo recipe, Set<SourceCapability> capabilities) {
        Map<String, RecipeApplicability> byName =
                cache.computeIfAbsent(normalize(capabilities), key -> new ConcurrentHashMap<>());
        RecipeApplicability cached = byName.get(recipe.name());
        if (cached != null) {
            return cached;
        }
        RecipeApplicability computed = compute(recipe, capabilities, new HashSet<>());
        byName.put(recipe.name(), computed);
        return computed;
    }

    /** Convenience for callers that only need the boolean. */
    public boolean isApplicable(RecipeInfo recipe, Set<SourceCapability> capabilities) {
        return applicability(recipe, capabilities).applicable();
    }

    private static Set<SourceCapability> normalize(Set<SourceCapability> capabilities) {
        return capabilities.isEmpty() ? Set.of() : Set.copyOf(capabilities);
    }

    private RecipeApplicability compute(RecipeInfo recipe, Set<SourceCapability> capabilities, Set<String> visited) {
        if (recipe.isComposite()) {
            return computeComposite(recipe, capabilities, visited);
        }
        return computeLeaf(recipe.name(), capabilities);
    }

    private RecipeApplicability computeLeaf(String recipeName, Set<SourceCapability> capabilities) {
        for (Map.Entry<String, SourceCapability> entry : REQUIRED_BY_PREFIX.entrySet()) {
            if (recipeName.startsWith(entry.getKey()) && !capabilities.contains(entry.getValue())) {
                return RecipeApplicability.missing(entry.getValue(), MISSING_REASONS.get(entry.getValue()));
            }
        }
        return RecipeApplicability.APPLICABLE;
    }

    /**
     * A composite is applicable as soon as one transitive sub-recipe is. Traversal is guarded by recipe name so that
     * cyclic or repeated sub-recipe graphs terminate, mirroring {@link RecipeCoverageUtils}.
     */
    private RecipeApplicability computeComposite(
            RecipeInfo recipe, Set<SourceCapability> capabilities, Set<String> visited) {
        visited.add(recipe.name());
        Set<SourceCapability> missing = EnumSet.noneOf(SourceCapability.class);
        if (collectApplicability(recipe, capabilities, visited, missing)) {
            return RecipeApplicability.APPLICABLE;
        }
        if (missing.size() == 1) {
            SourceCapability only = missing.iterator().next();
            return RecipeApplicability.missing(only, MISSING_REASONS.get(only));
        }
        if (missing.isEmpty()) {
            return RecipeApplicability.inapplicable("No sub-recipe can act on the parsed source set");
        }
        String names = missing.stream().map(SourceCapability::displayName).collect(Collectors.joining(", "));
        return RecipeApplicability.inapplicable(
                "No sub-recipe can act on the parsed source set — they require: " + names);
    }

    /** @return {@code true} as soon as an applicable sub-recipe is found; otherwise records the missing capabilities */
    private boolean collectApplicability(
            RecipeInfo composite,
            Set<SourceCapability> capabilities,
            Set<String> visited,
            Set<SourceCapability> missing) {
        for (RecipeInfo child : composite.recipeList()) {
            if (!visited.add(child.name())) {
                continue;
            }
            if (child.isComposite()) {
                if (collectApplicability(child, capabilities, visited, missing)) {
                    return true;
                }
                continue;
            }
            RecipeApplicability childResult = computeLeaf(child.name(), capabilities);
            if (childResult.applicable()) {
                return true;
            }
            childResult.missingCapability().ifPresent(missing::add);
        }
        return false;
    }

    /** Applicability for every recipe in {@code recipes}, keyed by recipe name, preserving iteration order. */
    public Map<String, RecipeApplicability> applicabilityByName(
            Iterable<RecipeInfo> recipes, Set<SourceCapability> capabilities) {
        Map<String, RecipeApplicability> result = new LinkedHashMap<>();
        for (RecipeInfo recipe : recipes) {
            result.put(recipe.name(), applicability(recipe, capabilities));
        }
        return result;
    }
}
