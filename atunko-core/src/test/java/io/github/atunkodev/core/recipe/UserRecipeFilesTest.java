package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.config.ConfigDirs;
import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_CORE_0020.2"})
class UserRecipeFilesTest {

    private String previousConfigDir;

    @BeforeEach
    void rememberConfigDirOverride() {
        previousConfigDir = System.getProperty(ConfigDirs.CONFIG_DIR_PROPERTY);
    }

    @AfterEach
    void restoreConfigDirOverride() {
        if (previousConfigDir != null) {
            System.setProperty(ConfigDirs.CONFIG_DIR_PROPERTY, previousConfigDir);
        } else {
            System.clearProperty(ConfigDirs.CONFIG_DIR_PROPERTY);
        }
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0020.2"})
    void discoverListsYamlFilesSortedByFilename(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("b.yml"), "");
        Files.writeString(dir.resolve("a.yaml"), "");
        Files.writeString(dir.resolve("notes.txt"), "");
        Files.createDirectory(dir.resolve("sub.yml"));

        assertThat(UserRecipeFiles.discover(dir)).containsExactly(dir.resolve("a.yaml"), dir.resolve("b.yml"));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0020.2"})
    void discoverReturnsEmptyForMissingDirectory(@TempDir Path dir) {
        assertThat(UserRecipeFiles.discover(dir.resolve("does-not-exist"))).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0020.2"})
    void discoverDefaultUsesOverridableConfigDir(@TempDir Path dir) throws IOException {
        System.setProperty(ConfigDirs.CONFIG_DIR_PROPERTY, dir.toString());
        assertThat(ConfigDirs.recipesDir()).isEqualTo(dir.resolve("recipes"));
        assertThat(UserRecipeFiles.discoverDefault()).isEmpty();

        Files.createDirectories(dir.resolve("recipes"));
        Files.writeString(dir.resolve("recipes").resolve("team.yml"), "");

        assertThat(UserRecipeFiles.discoverDefault())
                .containsExactly(dir.resolve("recipes").resolve("team.yml"));
    }
}
