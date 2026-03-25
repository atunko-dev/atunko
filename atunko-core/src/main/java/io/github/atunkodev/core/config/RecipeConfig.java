package io.github.atunkodev.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record RecipeConfig(String name, Map<String, Object> options) {

    @JsonCreator
    public RecipeConfig(@JsonProperty("name") String name, @JsonProperty("options") Map<String, Object> options) {
        this.name = name;
        this.options = options != null ? Map.copyOf(options) : Map.of();
    }

    public RecipeConfig(String name) {
        this(name, Map.of());
    }

    public boolean hasOptions() {
        return !options.isEmpty();
    }
}
