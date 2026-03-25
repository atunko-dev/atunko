package io.github.atunkodev.core.recipe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;

public record RecipeInfo(
        @JsonIgnore RecipeDescriptor descriptor, @JsonIgnore List<RecipeInfo> recipeList) {

    @JsonProperty("name")
    public String name() {
        return descriptor.getName();
    }

    @JsonProperty("displayName")
    public String displayName() {
        return descriptor.getDisplayName();
    }

    @JsonProperty("description")
    public String description() {
        return descriptor.getDescription();
    }

    @JsonProperty("tags")
    public Set<String> tags() {
        return descriptor.getTags();
    }

    @JsonProperty("options")
    public List<OptionDescriptor> options() {
        return descriptor.getOptions();
    }

    @JsonProperty("composite")
    public boolean isComposite() {
        return !recipeList.isEmpty();
    }

    public static RecipeInfo of(String name, String displayName, String description, Set<String> tags) {
        return of(name, displayName, description, tags, List.of());
    }

    public static RecipeInfo of(
            String name, String displayName, String description, Set<String> tags, List<RecipeInfo> recipeList) {
        RecipeDescriptor desc = new RecipeDescriptor(
                name,
                displayName,
                null,
                description,
                tags,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null);
        return new RecipeInfo(desc, recipeList);
    }
}
