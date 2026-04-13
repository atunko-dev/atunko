package io.github.atunkodev.core.config;

import io.github.reqstool.annotations.Requirements;
import java.util.stream.Collectors;

/**
 * Exports run configurations to native Maven/Gradle plugin formats.
 */
@Requirements({"atunko:CORE_0009"})
public class ConfigExportService {

    private static final String MAVEN_PLUGIN_VERSION = "5.27.0";
    private static final String GRADLE_PLUGIN_VERSION = "6.27.1";
    private static final String EXPORT_GROUP_ID = "io.github.atunkodev";
    private static final String EXPORT_ARTIFACT_ID = "atunko-rewrite";
    private static final String EXPORT_VERSION = "0.1.0-SNAPSHOT";

    /** Controls whether to emit a minimal plugin snippet or a full standalone build file. */
    public enum ExportMode {
        MINIMAL,
        FULL
    }

    /**
     * Exports a RunConfig to Gradle rewrite plugin format (minimal snippet).
     *
     * @param config the run configuration to export
     * @return a Gradle configuration snippet
     */
    public String exportToGradle(RunConfig config) {
        return exportToGradle(config, ExportMode.MINIMAL);
    }

    /**
     * Exports a RunConfig to Gradle rewrite plugin format.
     *
     * @param config the run configuration to export
     * @param mode MINIMAL for a plugin snippet, FULL for a standalone build.gradle
     * @return a Gradle configuration string
     */
    public String exportToGradle(RunConfig config, ExportMode mode) {
        String recipes = config.recipes().stream()
                .map(RecipeEntry::name)
                .map(name -> "\"" + name + "\"")
                .collect(Collectors.joining(", "));

        if (mode == ExportMode.FULL) {
            return "plugins {\n"
                    + "    id 'java'\n"
                    + "    id 'org.openrewrite.rewrite' version '"
                    + GRADLE_PLUGIN_VERSION
                    + "'\n"
                    + "}\n"
                    + "\n"
                    + "repositories {\n"
                    + "    mavenCentral()\n"
                    + "}\n"
                    + "\n"
                    + "rewrite {\n"
                    + "    activeRecipe("
                    + recipes
                    + ")\n"
                    + "}\n";
        }

        return "rewrite {\n" + "    activeRecipe(" + recipes + ")\n" + "}\n";
    }

    /**
     * Exports a RunConfig to Maven openrewrite plugin format (minimal snippet).
     *
     * @param config the run configuration to export
     * @return a Maven configuration snippet
     */
    public String exportToMaven(RunConfig config) {
        return exportToMaven(config, ExportMode.MINIMAL);
    }

    /**
     * Exports a RunConfig to Maven openrewrite plugin format.
     *
     * @param config the run configuration to export
     * @param mode MINIMAL for a plugin snippet, FULL for a standalone pom.xml
     * @return a Maven configuration string
     */
    public String exportToMaven(RunConfig config, ExportMode mode) {
        String recipes = config.recipes().stream()
                .map(RecipeEntry::name)
                .map(name -> "      <recipe>" + name + "</recipe>")
                .collect(Collectors.joining("\n"));

        String pluginBlock = "<plugin>\n"
                + "  <groupId>org.openrewrite.maven</groupId>\n"
                + "  <artifactId>rewrite-maven-plugin</artifactId>\n"
                + "  <version>"
                + MAVEN_PLUGIN_VERSION
                + "</version>\n"
                + "  <configuration>\n"
                + "    <activeRecipes>\n"
                + recipes
                + "\n"
                + "    </activeRecipes>\n"
                + "  </configuration>\n"
                + "</plugin>\n";

        if (mode == ExportMode.FULL) {
            String indentedPlugin = pluginBlock
                    .lines()
                    .map(line -> line.isEmpty() ? line : "            " + line)
                    .collect(Collectors.joining("\n"));
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                    + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                    + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0"
                    + " http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                    + "    <modelVersion>4.0.0</modelVersion>\n"
                    + "\n"
                    + "    <groupId>"
                    + EXPORT_GROUP_ID
                    + "</groupId>\n"
                    + "    <artifactId>"
                    + EXPORT_ARTIFACT_ID
                    + "</artifactId>\n"
                    + "    <version>"
                    + EXPORT_VERSION
                    + "</version>\n"
                    + "\n"
                    + "    <build>\n"
                    + "        <plugins>\n"
                    + indentedPlugin
                    + "        </plugins>\n"
                    + "    </build>\n"
                    + "</project>\n";
        }

        return pluginBlock;
    }
}
