package io.github.atunkodev.core.recipe;

import io.github.reqstool.annotations.Requirements;

/** Where a discovered recipe came from. */
@Requirements({"atunko:CORE_0019"})
public enum RecipeSource {
    /** Discovered from atunko's own runtime classpath — the bundled OpenRewrite catalog. */
    BUNDLED,
    /** Contributed by a recipe jar the user supplied (e.g. via {@code --recipe-jar}). */
    USER
}
