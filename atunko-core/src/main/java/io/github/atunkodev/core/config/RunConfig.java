package io.github.atunkodev.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RunConfig(int version, List<RecipeConfig> recipes) {

    public static final int CURRENT_VERSION = 1;

    public RunConfig(List<RecipeConfig> recipes) {
        this(CURRENT_VERSION, recipes);
    }

    public List<String> recipeNames() {
        return recipes.stream().map(RecipeConfig::name).toList();
    }
}
