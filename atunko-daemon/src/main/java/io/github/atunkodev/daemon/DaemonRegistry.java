package io.github.atunkodev.daemon;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atunkodev.core.config.YamlMappers;
import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The set of running daemons, persisted as one YAML file per daemon under {@link DaemonDirs#registryDir()}.
 *
 * <p>A file, not a lock-free in-memory structure, because the participants are separate processes: the daemon writes
 * its entry at startup and short-lived CLI clients read it. Every read verifies the recorded pid is still alive, so an
 * entry left behind by a crash or a reboot is discarded rather than causing a connection attempt to a dead port — or,
 * worse, to a port some unrelated process has since taken.
 *
 * <p>Entry files are created with owner-only permissions where the filesystem supports it, because they carry the
 * daemon's auth token.
 */
public class DaemonRegistry {

    /** System property overriding how many daemons are retained. */
    public static final String MAX_DAEMONS_PROPERTY = "atunko.daemon.max";

    /** Matches the Gradle daemon's default; see design decision 2. */
    public static final int DEFAULT_MAX_DAEMONS = 3;

    private static final Set<PosixFilePermission> OWNER_ONLY = PosixFilePermissions.fromString("rw-------");

    private final Path dir;
    private final Consumer<DaemonEntry> stopper;
    private final ObjectMapper mapper = YamlMappers.configMapper();

    public DaemonRegistry() {
        this(DaemonDirs.registryDir());
    }

    public DaemonRegistry(Path dir) {
        this(dir, entry -> ProcessHandle.of(entry.pid()).ifPresent(ProcessHandle::destroy));
    }

    /**
     * @param stopper how an evicted daemon is terminated. Injected rather than hard-wired to {@code ProcessHandle},
     *     because killing a process is a lifecycle concern the registry should not own — and because a test cannot
     *     use a real pid here without the registry trying to destroy the test JVM itself.
     */
    public DaemonRegistry(Path dir, Consumer<DaemonEntry> stopper) {
        this.dir = dir;
        this.stopper = stopper;
    }

    /** Registry key: a short digest of the resolved root, so the filename is stable and filesystem-safe. */
    static String keyFor(Path projectRoot) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(resolve(projectRoot).toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JRE", e);
        }
    }

    /** Absolute and symlink-free, so two spellings of the same directory share one daemon. */
    static Path resolve(Path projectRoot) {
        try {
            return projectRoot.toRealPath();
        } catch (IOException e) {
            return projectRoot.toAbsolutePath().normalize();
        }
    }

    private Path fileFor(Path projectRoot) {
        return dir.resolve(keyFor(projectRoot) + ".yaml");
    }

    /** Writes (or replaces) the entry for its project root. */
    public void write(DaemonEntry entry) {
        try {
            Files.createDirectories(dir);
            Path file = fileFor(entry.projectRoot());
            Files.writeString(file, mapper.writeValueAsString(entry));
            restrictPermissions(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write daemon registry entry", e);
        }
    }

    /** Best effort — a filesystem without POSIX permissions (Windows, some network mounts) is not a failure. */
    private void restrictPermissions(Path file) {
        try {
            Files.setPosixFilePermissions(file, OWNER_ONLY);
        } catch (IOException | UnsupportedOperationException e) {
            // Nothing to do: the token is still only as exposed as the user's own state directory.
        }
    }

    /** The live entry for a project root, if any. A stale entry (dead process) is deleted and reported absent. */
    public Optional<DaemonEntry> find(Path projectRoot) {
        return read(fileFor(projectRoot));
    }

    private Optional<DaemonEntry> read(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        DaemonEntry entry;
        try {
            entry = mapper.readValue(Files.readString(file), DaemonEntry.class);
        } catch (IOException e) {
            // A truncated or hand-edited file is indistinguishable from no daemon at all.
            deleteQuietly(file);
            return Optional.empty();
        }
        if (!entry.isProcessAlive()) {
            deleteQuietly(file);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    /** Every live daemon, most recently used first. */
    public List<DaemonEntry> list() {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<DaemonEntry> entries = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(f -> f.getFileName().toString().endsWith(".yaml"))
                    .forEach(f -> read(f).ifPresent(entries::add));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to list daemon registry", e);
        }
        entries.sort(Comparator.comparingLong(DaemonEntry::lastUsedEpochMillis).reversed());
        return List.copyOf(entries);
    }

    /** Records that a client just used this daemon, which is what keeps it out of the eviction candidates. */
    public void touch(Path projectRoot) {
        find(projectRoot)
                .map(e -> new DaemonEntry(
                        e.projectRoot(), e.port(), e.pid(), e.atunkoVersion(), e.token(), System.currentTimeMillis()))
                .ifPresent(this::write);
    }

    /** Removes the entry for a project root. The daemon process itself is not touched. */
    public void remove(Path projectRoot) {
        deleteQuietly(fileFor(projectRoot));
    }

    /**
     * Trims the registry to {@code max} daemons before a new one is added, stopping the least recently used entries.
     * {@code busyRoots} are never candidates: a daemon mid-execution would fail its client's request if stopped.
     *
     * @return the entries that were stopped and removed
     */
    @Requirements({"atunko:CORE_0023.1"})
    public List<DaemonEntry> evictToFit(int max, Set<Path> busyRoots) {
        List<DaemonEntry> live = list();
        // list() is most-recent-first; eviction takes from the tail, which is the least recently used.
        List<DaemonEntry> candidates = new ArrayList<>(live);
        candidates.removeIf(e -> busyRoots.contains(resolve(e.projectRoot())));

        List<DaemonEntry> evicted = new ArrayList<>();
        int surplus = live.size() - (max - 1);
        for (int i = candidates.size() - 1; i >= 0 && evicted.size() < surplus; i--) {
            DaemonEntry victim = candidates.get(i);
            stopper.accept(victim);
            remove(victim.projectRoot());
            evicted.add(victim);
        }
        return List.copyOf(evicted);
    }

    /** Configured retention limit, at least 1 — a limit of 0 would evict the daemon being started. */
    public static int maxDaemons() {
        int configured = Integer.getInteger(MAX_DAEMONS_PROPERTY, DEFAULT_MAX_DAEMONS);
        return Math.max(1, configured);
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Another process may have removed it first; either way the entry is gone.
        }
    }
}
