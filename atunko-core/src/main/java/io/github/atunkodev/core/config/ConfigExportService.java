package io.github.atunkodev.core.config;

import io.github.reqstool.annotations.Requirements;
import java.util.stream.Collectors;

/**
 * Exports run configurations to native Maven/Gradle plugin formats.
 */
@Requirements({"atunko:CORE_0009"})
public class ConfigExportService {

    /**
     * Exports a RunConfig to Gradle rewrite plugin format.
     *
     * @param config the run configuration to export
     * @return a Gradle configuration snippet
     */
    public String exportToGradle(RunConfig config) {
        String recipes = config.recipes().stream()
                .map(RecipeEntry::name)
                .map(name -> "\"" + name + "\"")
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("rewrite {\n");
        sb.append("  activeRecipe(").append(recipes).append(")\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Exports a RunConfig to Maven openrewrite plugin format.
     *
     * @param config the run configuration to export
     * @return a Maven configuration snippet
     */
    public String exportToMaven(RunConfig config) {
        String recipes = config.recipes().stream()
                .map(RecipeEntry::name)
                .map(name -> "      <recipe>" + name + "</recipe>")
                .collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder();
        sb.append("<plugin>\n");
        sb.append("  <groupId>org.openrewrite.maven</groupId>\n");
        sb.append("  <artifactId>rewrite-maven-plugin</artifactId>\n");
        sb.append("  <version>5.27.0</version>\n");
        sb.append("  <configuration>\n");
        sb.append("    <activeRecipes>\n");
        sb.append(recipes).append("\n");
        sb.append("    </activeRecipes>\n");
        sb.append("  </configuration>\n");
        sb.append("</plugin>\n");

        return sb.toString();
    }
}
