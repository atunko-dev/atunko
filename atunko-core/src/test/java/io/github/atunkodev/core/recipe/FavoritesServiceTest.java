package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_CORE_0021"})
class FavoritesServiceTest {

    @Test
    @SVCs({"atunko:SVC_CORE_0021"})
    void toggleMarksAndPersistsAcrossServiceInstances(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config").resolve("favorites.yml");
        FavoritesService service = new FavoritesService(file);

        assertThat(service.toggle("org.test.Alpha")).isTrue();
        assertThat(service.isFavorite("org.test.Alpha")).isTrue();
        assertThat(service.favorites()).containsExactly("org.test.Alpha");

        FavoritesService reloaded = new FavoritesService(file);
        assertThat(reloaded.isFavorite("org.test.Alpha")).isTrue();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0021"})
    void toggleAgainUnmarksAndPersists(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("favorites.yml");
        FavoritesService service = new FavoritesService(file);
        service.toggle("org.test.Alpha");

        assertThat(service.toggle("org.test.Alpha")).isFalse();

        assertThat(service.favorites()).isEmpty();
        assertThat(new FavoritesService(file).favorites()).isEmpty();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0021"})
    void missingFileReadsAsNoFavorites(@TempDir Path dir) {
        FavoritesService service = new FavoritesService(dir.resolve("does-not-exist.yml"));

        assertThat(service.favorites()).isEmpty();
        assertThat(service.isFavorite("org.test.Alpha")).isFalse();
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0021"})
    void malformedFileReadsAsNoFavoritesAndTogglingStillWorks(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("favorites.yml");
        Files.writeString(file, "favorites: [unclosed\n  ::: not yaml");
        FavoritesService service = new FavoritesService(file);

        assertThat(service.favorites()).isEmpty();
        assertThat(service.toggle("org.test.Alpha")).isTrue();
        assertThat(new FavoritesService(file).favorites()).containsExactly("org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0021.1"})
    void favoritesFilterMatchesAndCycles() {
        assertThat(FavoritesFilter.ALL.matches(true)).isTrue();
        assertThat(FavoritesFilter.ALL.matches(false)).isTrue();
        assertThat(FavoritesFilter.FAVORITES.matches(true)).isTrue();
        assertThat(FavoritesFilter.FAVORITES.matches(false)).isFalse();

        assertThat(FavoritesFilter.ALL.next()).isEqualTo(FavoritesFilter.FAVORITES);
        assertThat(FavoritesFilter.FAVORITES.next()).isEqualTo(FavoritesFilter.ALL);
    }
}
