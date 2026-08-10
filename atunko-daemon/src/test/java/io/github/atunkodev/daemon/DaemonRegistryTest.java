package io.github.atunkodev.daemon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.reqstool.annotations.SVCs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class DaemonRegistryTest {

    @TempDir
    Path registryDir;

    @TempDir
    Path projectRoot;

    private DaemonRegistry registry;

    /** Evicted daemons recorded instead of killed — the test entries carry this JVM's own pid. */
    private final List<DaemonEntry> stopped = new ArrayList<>();

    @BeforeEach
    void setUp() {
        registry = new DaemonRegistry(registryDir, stopped::add);
    }

    /** An entry for a process that is definitely running — this JVM. */
    private DaemonEntry liveEntry(Path root, long lastUsed) {
        return new DaemonEntry(root, 12345, ProcessHandle.current().pid(), "0.1.0-TEST", "token", lastUsed);
    }

    /** Starts and reaps a trivial process so its pid is known to belong to no live process. */
    private long deadPid() throws Exception {
        Process p = new ProcessBuilder("true").start();
        p.waitFor(10, TimeUnit.SECONDS);
        return p.pid();
    }

    @Test
    void writesAndReadsBackAnEntry() {
        DaemonEntry entry = liveEntry(projectRoot, 1_000L);

        registry.write(entry);

        assertThat(registry.find(projectRoot)).contains(entry);
    }

    @Test
    void findsTheSameDaemonThroughADifferentSpellingOfTheRoot() {
        registry.write(liveEntry(projectRoot, 1_000L));

        Path indirect = projectRoot.resolve("sub").resolve("..");

        assertThat(registry.find(indirect)).isPresent();
    }

    @Test
    void unknownProjectRootHasNoEntry() {
        assertThat(registry.find(projectRoot)).isEmpty();
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void entryFileIsReadableOnlyByItsOwner() throws Exception {
        registry.write(liveEntry(projectRoot, 1_000L));

        Path file = Files.list(registryDir).findFirst().orElseThrow();

        assertThat(Files.getPosixFilePermissions(file))
                .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    }

    @Test
    void staleEntryOfADeadProcessIsDiscardedAndDeleted() throws Exception {
        DaemonEntry stale = new DaemonEntry(projectRoot, 12345, deadPid(), "0.1.0-TEST", "token", 1_000L);
        registry.write(stale);

        assertThat(registry.find(projectRoot)).isEmpty();
        assertThat(Files.list(registryDir)).isEmpty();
    }

    @Test
    void unreadableEntryIsDiscardedAndDeleted() throws Exception {
        registry.write(liveEntry(projectRoot, 1_000L));
        Path file = Files.list(registryDir).findFirst().orElseThrow();
        Files.writeString(file, "this: is: not: a valid entry");

        assertThat(registry.find(projectRoot)).isEmpty();
        assertThat(Files.list(registryDir)).isEmpty();
    }

    @Test
    void listReturnsLiveEntriesMostRecentlyUsedFirst(@TempDir Path otherRoot) {
        registry.write(liveEntry(projectRoot, 1_000L));
        registry.write(liveEntry(otherRoot, 5_000L));

        assertThat(registry.list()).extracting(DaemonEntry::lastUsedEpochMillis).containsExactly(5_000L, 1_000L);
    }

    @Test
    void touchMakesADaemonTheMostRecentlyUsed(@TempDir Path otherRoot) {
        registry.write(liveEntry(projectRoot, 1_000L));
        registry.write(liveEntry(otherRoot, 5_000L));

        registry.touch(projectRoot);

        assertThat(registry.list().getFirst().projectRoot()).isEqualTo(projectRoot);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0023.1"})
    void evictsLeastRecentlyUsedIdleDaemon(@TempDir Path second, @TempDir Path third) {
        registry.write(liveEntry(projectRoot, 1_000L)); // least recently used
        registry.write(liveEntry(second, 5_000L));
        registry.write(liveEntry(third, 9_000L));

        List<DaemonEntry> evicted = registry.evictToFit(3, Set.of());

        assertThat(evicted).extracting(DaemonEntry::projectRoot).containsExactly(projectRoot);
        assertThat(stopped).extracting(DaemonEntry::projectRoot).containsExactly(projectRoot);
        assertThat(registry.find(projectRoot)).isEmpty();
        assertThat(registry.list()).extracting(DaemonEntry::projectRoot).containsExactlyInAnyOrder(second, third);
    }

    @Test
    @SVCs({"atunko:SVC_CORE_0023.1"})
    void neverEvictsABusyDaemon(@TempDir Path second, @TempDir Path third) {
        registry.write(liveEntry(projectRoot, 1_000L)); // least recently used, but busy
        registry.write(liveEntry(second, 5_000L));
        registry.write(liveEntry(third, 9_000L));

        List<DaemonEntry> evicted = registry.evictToFit(3, Set.of(DaemonRegistry.resolve(projectRoot)));

        assertThat(evicted).extracting(DaemonEntry::projectRoot).containsExactly(second);
        assertThat(registry.find(projectRoot)).isPresent();
        assertThat(stopped).extracting(DaemonEntry::projectRoot).doesNotContain(projectRoot);
    }

    @Test
    void evictsNothingWhenBelowTheLimit(@TempDir Path second) {
        registry.write(liveEntry(projectRoot, 1_000L));
        registry.write(liveEntry(second, 5_000L));

        assertThat(registry.evictToFit(3, Set.of())).isEmpty();
        assertThat(registry.list()).hasSize(2);
        assertThat(stopped).isEmpty();
    }

    @Test
    void removeDeletesTheEntry() {
        registry.write(liveEntry(projectRoot, 1_000L));

        registry.remove(projectRoot);

        assertThat(registry.find(projectRoot)).isEmpty();
    }

    @Test
    void maxDaemonsIsNeverBelowOne() {
        System.setProperty(DaemonRegistry.MAX_DAEMONS_PROPERTY, "0");
        try {
            assertThat(DaemonRegistry.maxDaemons()).isEqualTo(1);
        } finally {
            System.clearProperty(DaemonRegistry.MAX_DAEMONS_PROPERTY);
        }
    }
}
