package io.github.atunko.core.recipe;

import io.github.reqstool.annotations.Requirements;
import java.util.List;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;

@Requirements({"CORE_0001"})
public class RecipeDiscoveryService {

    public List<RecipeInfo> discoverAll() {
        Environment env = Environment.builder()
            .scanRuntimeClasspath()
            .build();

        return env.listRecipeDescriptors().stream()
            .map(this::toRecipeInfo)
            .toList();
    }

    private RecipeInfo toRecipeInfo(RecipeDescriptor descriptor) {
        return new RecipeInfo(
            descriptor.getName(),
            descriptor.getDisplayName(),
            descriptor.getDescription(),
            descriptor.getTags()
        );
    }
}
