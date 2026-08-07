package io.github.atunkodev.core.recipe;

import io.github.atunkodev.core.project.SourceCapability;
import io.github.reqstool.annotations.Requirements;
import java.util.Optional;

/**
 * Whether a recipe can act on a parsed source set, and — when it cannot — which capability is missing and why.
 *
 * @param applicable {@code true} when the recipe can act on the parsed sources
 * @param missingCapability the capability the recipe needs but the source set lacks; empty when applicable, and also
 *     empty for a composite that is inapplicable because none of its sub-recipes are applicable
 * @param reason human-readable explanation; empty when applicable
 */
@Requirements({"atunko:CORE_0015"})
public record RecipeApplicability(boolean applicable, Optional<SourceCapability> missingCapability, String reason) {

    /** Shared instance for recipes that can act on any source set. */
    public static final RecipeApplicability APPLICABLE = new RecipeApplicability(true, Optional.empty(), "");

    public static RecipeApplicability missing(SourceCapability capability, String reason) {
        return new RecipeApplicability(false, Optional.of(capability), reason);
    }

    public static RecipeApplicability inapplicable(String reason) {
        return new RecipeApplicability(false, Optional.empty(), reason);
    }

    /** Short UI label such as {@code needs Maven}, or empty when the recipe is applicable. */
    public String badgeLabel() {
        return missingCapability.map(c -> "needs " + c.displayName()).orElse(applicable ? "" : "not applicable");
    }
}
