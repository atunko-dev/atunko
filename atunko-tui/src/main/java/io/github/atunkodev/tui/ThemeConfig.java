package io.github.atunkodev.tui;

import java.nio.file.Path;

/**
 * Theme configuration resolved from CLI flags and XDG defaults.
 *
 * @param themeName the bundled theme name ("dark" or "light"), null if user CSS overrides
 * @param cssFile path to a user-provided CSS file, null if using a bundled theme
 */
public record ThemeConfig(String themeName, Path cssFile) {

    /** Default theme configuration — dark theme, no user CSS. */
    public static final ThemeConfig DEFAULT = new ThemeConfig("dark", null);

    private static final Path XDG_THEME_PATH =
            Path.of(System.getProperty("user.home"), ".config", "atunko", "theme.tcss");

    /**
     * Resolves a ThemeConfig from CLI options.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>{@code --css-file} flag — user-provided CSS replaces bundled theme</li>
     *   <li>{@code ~/.config/atunko/theme.tcss} exists — auto-loaded, replaces bundled</li>
     *   <li>{@code --theme} flag — selects bundled theme</li>
     *   <li>Default — dark theme</li>
     * </ol>
     */
    public static ThemeConfig resolve(String themeName, Path cssFile) {
        return resolve(themeName, cssFile, XDG_THEME_PATH);
    }

    static ThemeConfig resolve(String themeName, Path cssFile, Path xdgThemePath) {
        if (cssFile != null) {
            return new ThemeConfig(null, cssFile);
        }
        if (xdgThemePath.toFile().isFile()) {
            return new ThemeConfig(null, xdgThemePath);
        }
        if (themeName != null) {
            return new ThemeConfig(themeName, null);
        }
        return DEFAULT;
    }

    /** Returns true if a user-provided CSS file should be used instead of a bundled theme. */
    public boolean isUserCss() {
        return cssFile != null;
    }
}
