package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.openrewrite.config.RecipeDescriptor;

public class RecipeDiscoveryService {

    private static final Set<RecipeField> ALL_FIELDS = Set.of(RecipeField.values());

    private final EnvironmentProvider environmentProvider;

    public RecipeDiscoveryService() {
        this(new EnvironmentProvider());
    }

    public RecipeDiscoveryService(EnvironmentProvider environmentProvider) {
        this.environmentProvider = environmentProvider;
    }

    @Requirements({"atunko:CORE_0001"})
    public List<RecipeInfo> discoverAll() {
        // A user jar redeclaring a bundled name contributes a duplicate descriptor; keep one entry per name so
        // the catalog never lists the same recipe twice.
        Set<String> seen = new HashSet<>();
        return environmentProvider.get().listRecipeDescriptors().stream()
                .filter(descriptor -> seen.add(descriptor.getName()))
                .map(this::toRecipeInfo)
                .toList();
    }

    @Requirements({"atunko:CORE_0019.2"})
    public List<RecipeInfo> discoverAll(RecipeSourceFilter filter) {
        return discoverAll().stream().filter(r -> filter.matches(r.source())).toList();
    }

    @Requirements({"atunko:CORE_0002"})
    public List<RecipeInfo> search(String query) {
        return search(query, ALL_FIELDS);
    }

    @Requirements({"atunko:CORE_0002"})
    public List<RecipeInfo> search(String query, Set<RecipeField> fields) {
        if (query == null || query.isBlank()) {
            return discoverAll();
        }
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        return discoverAll().stream()
                .filter(recipe -> matches(recipe, lowerQuery, fields))
                .toList();
    }

    @Requirements({"atunko:CORE_0019.2"})
    public List<RecipeInfo> search(String query, Set<RecipeField> fields, RecipeSourceFilter filter) {
        return search(query, fields).stream()
                .filter(r -> filter.matches(r.source()))
                .toList();
    }

    private boolean matches(RecipeInfo recipe, String lowerQuery, Set<RecipeField> fields) {
        if (fields.contains(RecipeField.NAME)
                && recipe.name().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            return true;
        }
        if (fields.contains(RecipeField.DISPLAY_NAME)
                && recipe.displayName().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            return true;
        }
        if (fields.contains(RecipeField.DESCRIPTION)
                && recipe.description() != null
                && recipe.description().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            return true;
        }
        return fields.contains(RecipeField.TAGS)
                && recipe.tags().stream()
                        .anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(lowerQuery));
    }

    @Requirements({"atunko:CORE_0001.1", "atunko:CORE_0014", "atunko:CORE_0019"})
    private RecipeInfo toRecipeInfo(RecipeDescriptor descriptor) {
        List<RecipeInfo> subRecipes = descriptor.getRecipeList() != null
                ? descriptor.getRecipeList().stream().map(this::toRecipeInfo).toList()
                : List.of();
        List<RecipeOptionInfo> options = descriptor.getOptions() != null
                ? descriptor.getOptions().stream()
                        .map(o -> new RecipeOptionInfo(
                                o.getName(),
                                o.getType(),
                                o.getDisplayName(),
                                o.getDescription(),
                                o.getExample(),
                                o.getValid(),
                                o.isRequired()))
                        .toList()
                : List.of();
        RecipeSource source =
                environmentProvider.isUserRecipe(descriptor.getName()) ? RecipeSource.USER : RecipeSource.BUNDLED;
        return new RecipeInfo(
                descriptor.getName(),
                descriptor.getDisplayName(),
                descriptor.getDescription(),
                descriptor.getTags(),
                subRecipes,
                options,
                source);
    }
}
