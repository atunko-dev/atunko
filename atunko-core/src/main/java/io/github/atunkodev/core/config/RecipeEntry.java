package io.github.atunkodev.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecipeEntry(
        String name,
        @Nullable Map<String, Object> options,
        @Nullable List<String> exclude) {

    public RecipeEntry(String name) {
        this(name, null, null);
    }
}
