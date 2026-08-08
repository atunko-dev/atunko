package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;

/**
 * A kind of source model that {@link ProjectSourceParser} can produce. Recipes require specific capabilities to be
 * able to act: for example every {@code org.openrewrite.maven.*} recipe needs a Maven resolution model, which is only
 * present when at least one {@code pom.xml} was parsed with OpenRewrite's Maven parser.
 */
@Requirements({"atunko:CORE_0015.1"})
public enum SourceCapability {
    JAVA("Java"),
    XML("XML"),
    YAML("YAML"),
    JSON("JSON"),
    PROPERTIES("Properties"),
    MAVEN("Maven"),
    GRADLE("Gradle");

    private final String displayName;

    SourceCapability(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable name for use in UI badges and reason messages. */
    public String displayName() {
        return displayName;
    }
}
