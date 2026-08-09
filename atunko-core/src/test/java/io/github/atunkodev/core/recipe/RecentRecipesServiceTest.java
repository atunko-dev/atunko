package io.github.atunkodev.core.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.atunkodev.core.recipe.RecentRecipesService.RecentRecipe;
import io.github.reqstool.annotations.SVCs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SVCs({"atunko:SVC_CORE_0022"})
class RecentRecipesServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");

    private static RecentRecipesService service(Path file) {
        return new RecentRecipesService(file, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0022"})
    void recordAddsTimestampedEntriesNewestFirstAndPersists(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config").resolve("recent.yml");
        RecentRecipesService service = service(file);

        service.record(List.of("org.test.Alpha"));
        service.record(List.of("org.test.Beta"));

        assertThat(service.recentNames()).containsExactly("org.test.Beta", "org.test.Alpha");
        assertThat(service.recent())
                .allSatisfy(entry -> assertThat(entry.lastUsed()).isEqualTo(NOW.toString()));
        assertThat(new RecentRecipesService(file).recentNames()).containsExactly("org.test.Beta", "org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0022"})
    void reRecordingMovesARecipeToTheFront(@TempDir Path dir) throws IOException {
        RecentRecipesService service = service(dir.resolve("recent.yml"));
        service.record(List.of("org.test.Alpha"));
        service.record(List.of("org.test.Beta"));

        service.record(List.of("org.test.Alpha"));

        assertThat(service.recentNames()).containsExactly("org.test.Alpha", "org.test.Beta");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0022"})
    void recentListIsCappedAtMaxEntries(@TempDir Path dir) throws IOException {
        RecentRecipesService service = service(dir.resolve("recent.yml"));

        for (int i = 0; i < RecentRecipesService.MAX_ENTRIES + 5; i++) {
            service.record(List.of("org.test.Recipe" + i));
        }

        List<RecentRecipe> recent = service.recent();
        assertThat(recent).hasSize(RecentRecipesService.MAX_ENTRIES);
        assertThat(recent.get(0).name()).isEqualTo("org.test.Recipe" + (RecentRecipesService.MAX_ENTRIES + 4));
        assertThat(IntStream.range(0, 5).mapToObj(i -> "org.test.Recipe" + i))
                .noneMatch(service.recentNames()::contains);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0022"})
    void missingOrMalformedFileReadsAsEmpty(@TempDir Path dir) throws IOException {
        assertThat(service(dir.resolve("does-not-exist.yml")).recent()).isEmpty();

        Path malformed = dir.resolve("recent.yml");
        Files.writeString(malformed, "recent: [unclosed\n  ::: not yaml");
        RecentRecipesService service = service(malformed);
        assertThat(service.recent()).isEmpty();
        service.record(List.of("org.test.Alpha"));
        assertThat(new RecentRecipesService(malformed).recentNames()).containsExactly("org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0022.1"})
    void recentSortOrderPutsRecentlyUsedFirstThenNameOrder() {
        RecipeInfo alpha = new RecipeInfo("org.test.Alpha", "Alpha", null, Set.of());
        RecipeInfo beta = new RecipeInfo("org.test.Beta", "Beta", null, Set.of());
        RecipeInfo gamma = new RecipeInfo("org.test.Gamma", "Gamma", null, Set.of());

        List<RecipeInfo> sorted = List.of(alpha, beta, gamma).stream()
                .sorted(SortOrder.RECENT.comparator(List.of("org.test.Gamma", "org.test.Beta")))
                .toList();

        assertThat(sorted)
                .extracting(RecipeInfo::name)
                .containsExactly("org.test.Gamma", "org.test.Beta", "org.test.Alpha");
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0022.1"})
    void recentSortOrderDegradesToNameOrderWithoutRecentContext() {
        RecipeInfo alpha = new RecipeInfo("org.test.Alpha", "Alpha", null, Set.of());
        RecipeInfo beta = new RecipeInfo("org.test.Beta", "Beta", null, Set.of());

        assertThat(List.of(beta, alpha).stream()
                        .sorted(SortOrder.RECENT.comparator())
                        .toList())
                .extracting(RecipeInfo::name)
                .containsExactly("org.test.Alpha", "org.test.Beta");
        assertThat(List.of(beta, alpha).stream()
                        .sorted(SortOrder.RECENT.comparator(List.of()))
                        .toList())
                .extracting(RecipeInfo::name)
                .containsExactly("org.test.Alpha", "org.test.Beta");
    }
}
