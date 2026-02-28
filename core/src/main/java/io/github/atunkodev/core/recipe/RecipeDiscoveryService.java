package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;
import java.util.List;
import org.openrewrite.config.RecipeDescriptor;

public class RecipeDiscoveryService {

    private final EnvironmentProvider environmentProvider;

    public RecipeDiscoveryService() {
        this(new EnvironmentProvider());
    }

    public RecipeDiscoveryService(EnvironmentProvider environmentProvider) {
        this.environmentProvider = environmentProvider;
    }

    @Requirements({"CORE_0001"})
    public List<RecipeInfo> discoverAll() {
        return environmentProvider.get().listRecipeDescriptors().stream()
                .map(this::toRecipeInfo)
                .toList();
    }

    @Requirements({"CORE_0002"})
    public List<RecipeInfo> search(String query) {
        if (query == null || query.isBlank()) {
            return discoverAll();
        }
        String lowerQuery = query.toLowerCase();
        return discoverAll().stream()
                .filter(recipe -> matches(recipe, lowerQuery))
                .toList();
    }

    private boolean matches(RecipeInfo recipe, String lowerQuery) {
        if (recipe.name().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        if (recipe.displayName().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        if (recipe.description() != null && recipe.description().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        return recipe.tags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerQuery));
    }

    private RecipeInfo toRecipeInfo(RecipeDescriptor descriptor) {
        return new RecipeInfo(
                descriptor.getName(), descriptor.getDisplayName(), descriptor.getDescription(), descriptor.getTags());
    }
}
