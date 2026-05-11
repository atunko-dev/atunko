package io.github.atunkodev.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunConfig(
        int version, @Nullable String description, @Nullable WorkspaceConfig workspace, List<RecipeEntry> recipes) {

    public static final int CURRENT_VERSION = 1;

    public RunConfig(List<RecipeEntry> recipes) {
        this(CURRENT_VERSION, null, null, recipes);
    }

    public RunConfig(@Nullable String description, List<RecipeEntry> recipes) {
        this(CURRENT_VERSION, description, null, recipes);
    }

    public RunConfig(@Nullable String description, @Nullable WorkspaceConfig workspace, List<RecipeEntry> recipes) {
        this(CURRENT_VERSION, description, workspace, recipes);
    }
}
