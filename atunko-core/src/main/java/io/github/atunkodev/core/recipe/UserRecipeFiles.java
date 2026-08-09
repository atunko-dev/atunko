package io.github.atunkodev.core.recipe;

import io.github.atunkodev.core.config.ConfigDirs;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Locates user-authored declarative recipe YAML files. Files are auto-discovered from the {@code recipes} directory
 * of the atunko config dir (see {@link ConfigDirs}); loading and failure isolation are {@link EnvironmentProvider}'s
 * job.
 */
public final class UserRecipeFiles {

    private static final Logger LOG = Logger.getLogger(UserRecipeFiles.class.getName());

    private UserRecipeFiles() {}

    /** The {@code *.yml}/{@code *.yaml} files in {@code dir}, sorted by filename — empty when it is no directory. */
    @Requirements({"atunko:CORE_0020.2"})
    public static List<Path> discover(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(UserRecipeFiles::isYamlFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            LOG.warning(() -> "Could not list user recipe files in " + dir + ": " + e);
            return List.of();
        }
    }

    /** Auto-discovered recipe YAML files from the config dir's {@code recipes} directory. */
    @Requirements({"atunko:CORE_0020.2"})
    public static List<Path> discoverDefault() {
        return discover(ConfigDirs.recipesDir());
    }

    private static boolean isYamlFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }
}
