package io.github.atunkodev.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RunConfig(int version, List<String> recipes) {

    public static final int CURRENT_VERSION = 1;

    public RunConfig(List<String> recipes) {
        this(CURRENT_VERSION, recipes);
    }
}
