package io.github.atunkodev.core.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.github.atunkodev.core.config.ConfigDirs;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Marks and unmarks recipes as favorites, persisted as a plain name list in the atunko config dir
 * ({@code favorites.yml} by default; the file is injectable so tests never touch the real home directory).
 *
 * <p>The file is loaded lazily once and kept in memory; every toggle persists immediately. A missing or malformed
 * favorites file reads as "no favorites" with a logged warning — favorites must never block startup.
 */
public class FavoritesService {

    private static final Logger LOG = Logger.getLogger(FavoritesService.class.getName());

    record FavoritesFile(List<String> favorites) {}

    private final Path file;
    private final ObjectMapper yamlMapper;
    private Set<String> favorites;

    public FavoritesService() {
        this(ConfigDirs.favoritesFile());
    }

    public FavoritesService(Path file) {
        this.file = file;
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build();
        this.yamlMapper = new ObjectMapper(yamlFactory).disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /** The favorite recipe names, in the order they were marked. */
    @Requirements({"atunko:CORE_0021"})
    public Set<String> favorites() {
        return Set.copyOf(loaded());
    }

    @Requirements({"atunko:CORE_0021"})
    public boolean isFavorite(String recipeName) {
        return loaded().contains(recipeName);
    }

    /** Marks or unmarks the recipe as favorite and persists the change. Returns the new favorite state. */
    @Requirements({"atunko:CORE_0021"})
    public boolean toggle(String recipeName) throws IOException {
        Set<String> names = loaded();
        boolean nowFavorite = !names.remove(recipeName);
        if (nowFavorite) {
            names.add(recipeName);
        }
        save(names);
        return nowFavorite;
    }

    private Set<String> loaded() {
        if (favorites == null) {
            favorites = load();
        }
        return favorites;
    }

    @Requirements({"atunko:CORE_0021"})
    private Set<String> load() {
        if (!Files.isRegularFile(file)) {
            return new LinkedHashSet<>();
        }
        try {
            FavoritesFile parsed = yamlMapper.readValue(file.toFile(), FavoritesFile.class);
            return parsed.favorites() != null ? new LinkedHashSet<>(parsed.favorites()) : new LinkedHashSet<>();
        } catch (IOException | RuntimeException e) {
            LOG.warning(() -> "Ignoring malformed favorites file " + file + ": " + e.getMessage());
            return new LinkedHashSet<>();
        }
    }

    private void save(Set<String> names) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        yamlMapper.writeValue(file.toFile(), new FavoritesFile(List.copyOf(names)));
    }
}
