package io.github.atunkodev.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunConfig(int version, @Nullable String description, List<RecipeEntry> recipes) {

    public static final int CURRENT_VERSION = 1;

    public RunConfig(List<RecipeEntry> recipes) {
        this(CURRENT_VERSION, null, recipes);
    }

    public RunConfig(@Nullable String description, List<RecipeEntry> recipes) {
        this(CURRENT_VERSION, description, recipes);
    }
}
