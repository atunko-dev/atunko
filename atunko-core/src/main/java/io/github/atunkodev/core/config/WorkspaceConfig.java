package io.github.atunkodev.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.reqstool.annotations.Requirements;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Optional workspace block in {@code .atunko.yml}. Absent means single-project mode. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Requirements({"atunko:CORE_0012"})
public record WorkspaceConfig(
        @Nullable String root,
        @Nullable List<String> include,
        @Nullable List<String> exclude) {

    public WorkspaceConfig(@Nullable String root) {
        this(root, null, null);
    }
}
