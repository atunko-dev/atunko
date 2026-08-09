package io.github.atunkodev.core.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.core.config.ConfigDirs;
import io.github.atunkodev.core.config.YamlMappers;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Tracks recently executed recipes, persisted newest-first in the atunko config dir ({@code recent.yml} by default;
 * the file and clock are injectable for tests). Recording deduplicates — a re-run moves the recipe to the front with
 * a fresh timestamp — and the list is capped at {@value #MAX_ENTRIES} entries.
 *
 * <p>Timestamps are stored as ISO-8601 strings. A missing or malformed recent file reads as empty with a logged
 * warning — recent tracking must never block startup.
 */
public class RecentRecipesService {

    /** Maximum number of recently used recipes kept in the file. */
    public static final int MAX_ENTRIES = 20;

    private static final Logger LOG = Logger.getLogger(RecentRecipesService.class.getName());

    /** One recently executed recipe: its name and the ISO-8601 instant it was last run. */
    public record RecentRecipe(String name, String lastUsed) {}

    record RecentFile(List<RecentRecipe> recent) {}

    private final Path file;
    private final Clock clock;
    private final ObjectMapper yamlMapper;
    private List<RecentRecipe> recent;

    public RecentRecipesService() {
        this(ConfigDirs.recentFile());
    }

    public RecentRecipesService(Path file) {
        this(file, Clock.systemUTC());
    }

    public RecentRecipesService(Path file, Clock clock) {
        this.file = file;
        this.clock = clock;
        this.yamlMapper = YamlMappers.configMapper();
    }

    /** The recently executed recipes, newest first. */
    @Requirements({"atunko:CORE_0022"})
    public List<RecentRecipe> recent() {
        return List.copyOf(loaded());
    }

    /** The recently executed recipe names, newest first — the shape sort orders consume. */
    @Requirements({"atunko:CORE_0022.1"})
    public List<String> recentNames() {
        return loaded().stream().map(RecentRecipe::name).toList();
    }

    /**
     * Records an execution of the given recipes: they move to the front (newest first, in the given order) with a
     * fresh timestamp, and the list is truncated to {@value #MAX_ENTRIES} entries and persisted.
     */
    @Requirements({"atunko:CORE_0022"})
    public void record(List<String> recipeNames) throws IOException {
        if (recipeNames.isEmpty()) {
            return;
        }
        String now = clock.instant().toString();
        List<RecentRecipe> updated = new ArrayList<>();
        recipeNames.stream().distinct().forEach(name -> updated.add(new RecentRecipe(name, now)));
        loaded().stream().filter(entry -> !recipeNames.contains(entry.name())).forEach(updated::add);
        List<RecentRecipe> truncated =
                updated.size() > MAX_ENTRIES ? new ArrayList<>(updated.subList(0, MAX_ENTRIES)) : updated;
        save(truncated);
        // Assigned only after a successful write, so the in-memory state can never diverge from disk.
        recent = truncated;
    }

    private List<RecentRecipe> loaded() {
        if (recent == null) {
            recent = load();
        }
        return recent;
    }

    @Requirements({"atunko:CORE_0022"})
    private List<RecentRecipe> load() {
        if (!Files.isRegularFile(file)) {
            return new ArrayList<>();
        }
        try {
            RecentFile parsed = yamlMapper.readValue(file.toFile(), RecentFile.class);
            return parsed.recent() != null ? new ArrayList<>(parsed.recent()) : new ArrayList<>();
        } catch (IOException | RuntimeException e) {
            LOG.warning(() -> "Ignoring malformed recent file " + file + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void save(List<RecentRecipe> entries) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        yamlMapper.writeValue(file.toFile(), new RecentFile(List.copyOf(entries)));
    }
}
