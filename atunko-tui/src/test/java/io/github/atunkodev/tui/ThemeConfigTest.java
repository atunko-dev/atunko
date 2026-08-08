package io.github.atunkodev.tui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThemeConfigTest {

    @Test
    @SVCs({"atunko:SVC_TUI_0001.18"})
    void defaultThemeIsDark() {
        ThemeConfig config = ThemeConfig.resolve(null, null);

        assertThat(config.themeName()).isEqualTo("dark");
        assertThat(config.isUserCss()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.18.1", "atunko:SVC_TUI_0001.18.4"})
    void themeFlagSelectsLight() {
        ThemeConfig config = ThemeConfig.resolve("light", null);

        assertThat(config.themeName()).isEqualTo("light");
        assertThat(config.isUserCss()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.18.2"})
    void cssFileFlagOverridesBundledTheme(@TempDir Path tempDir) throws IOException {
        Path userCss = tempDir.resolve("custom.tcss");
        Files.writeString(userCss, ".app { background: red; }");

        ThemeConfig config = ThemeConfig.resolve("light", userCss);

        assertThat(config.isUserCss()).isTrue();
        assertThat(config.cssFile()).isEqualTo(userCss);
        assertThat(config.themeName()).isNull();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.18.4"})
    void themeFlagSelectsDark() {
        ThemeConfig config = ThemeConfig.resolve("dark", null);

        assertThat(config.themeName()).isEqualTo("dark");
        assertThat(config.isUserCss()).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.18.2"})
    void cssFileTakesPriorityOverThemeFlag(@TempDir Path tempDir) throws IOException {
        Path userCss = tempDir.resolve("theme.tcss");
        Files.writeString(userCss, ".app { color: white; }");

        ThemeConfig config = ThemeConfig.resolve("dark", userCss);

        assertThat(config.isUserCss()).isTrue();
        assertThat(config.cssFile()).isEqualTo(userCss);
    }

    @Test
    @SVCs({"atunko:SVC_TUI_0001.18.3"})
    void xdgConfigFileAutoLoaded(@TempDir Path tempDir) throws IOException {
        Path xdgTheme = tempDir.resolve("theme.tcss");
        Files.writeString(xdgTheme, ".app { background: blue; }");

        ThemeConfig config = ThemeConfig.resolve(null, null, xdgTheme);

        assertThat(config.isUserCss()).isTrue();
        assertThat(config.cssFile()).isEqualTo(xdgTheme);
    }

    @Test
    void cssFileTakesPriorityOverXdgConfig(@TempDir Path tempDir) throws IOException {
        Path xdgTheme = tempDir.resolve("xdg-theme.tcss");
        Files.writeString(xdgTheme, ".app { background: blue; }");
        Path userCss = tempDir.resolve("custom.tcss");
        Files.writeString(userCss, ".app { background: red; }");

        ThemeConfig config = ThemeConfig.resolve(null, userCss, xdgTheme);

        assertThat(config.cssFile()).isEqualTo(userCss);
    }

    @Test
    void cssFileReplacesNotLayers(@TempDir Path tempDir) throws IOException {
        Path userCss = tempDir.resolve("theme.tcss");
        Files.writeString(userCss, ".app { color: green; }");

        ThemeConfig config = ThemeConfig.resolve(null, userCss);

        assertThat(config.isUserCss()).isTrue();
        assertThat(config.themeName()).isNull();
    }
}
