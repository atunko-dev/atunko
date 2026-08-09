package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * Session-lifetime in-memory cache of {@link ParsedSources}, keyed by project directory. Parsing dominates
 * recipe-execution cost, so long-lived sessions (TUI, Web UI) parse each project once and re-parse only when its
 * inputs actually changed.
 *
 * <p>Staleness is detected by fingerprinting the parser's inputs: every file under {@link ProjectInfo#parseRoots()}
 * and every build file as (size, mtime, content CRC), plus every classpath entry as (size, mtime) — a rebuilt
 * dependency jar changes recipe output just like an edited source. Any difference (modified, added or removed file)
 * discards the entry and re-parses the whole project; the fingerprint walk costs a fraction of a parse that costs
 * seconds to minutes. Per-file re-parse is deliberately not attempted: Java sources resolve types across files, so
 * splicing a single re-parsed file into a cached list is not sound.
 *
 * <p>Entries hold their sources via {@link SoftReference}, so under memory pressure the GC reclaims cached LSTs and
 * the next {@link #get} re-parses instead of the JVM running out of heap. Concurrent callers for the same project
 * share a single parse.
 *
 * <p>Disabled via system property {@code atunko.lst.cache.disabled=true} (or the explicit constructor), in which case
 * every call delegates straight to the parser and nothing is stored.
 */
@Requirements({"atunko:CORE_0018"})
public class ParsedSourcesCache {

    /** System property that disables caching entirely when set to {@code true}. */
    public static final String DISABLE_PROPERTY = "atunko.lst.cache.disabled";

    private record Entry(ProjectInfo info, Map<Path, Fingerprint> fingerprint, SoftReference<ParsedSources> parsed) {}

    /** {@code contentCrc} is {@link #STAT_ONLY} for classpath entries, whose bytes are never read. */
    private record Fingerprint(long size, long lastModifiedMillis, long contentCrc) {}

    private static final long STAT_ONLY = -1;

    private final ProjectSourceParser parser;
    private final boolean enabled;
    private final ConcurrentHashMap<Path, Entry> entries = new ConcurrentHashMap<>();

    public ParsedSourcesCache(ProjectSourceParser parser) {
        this(parser, !Boolean.getBoolean(DISABLE_PROPERTY));
    }

    public ParsedSourcesCache(ProjectSourceParser parser, boolean enabled) {
        this.parser = parser;
        this.enabled = enabled;
    }

    @Requirements({"atunko:CORE_0018.2"})
    public ParsedSources get(ProjectEntry entry) {
        return get(entry.projectDir(), entry.info());
    }

    /**
     * Returns the parsed sources for the project, from cache when its inputs are unchanged since the last parse,
     * otherwise freshly parsed. A cached entry is only served for the same {@link ProjectInfo} it was parsed with —
     * a re-scan that changes classpath or source dirs invalidates it like a file change does.
     */
    @Requirements({"atunko:CORE_0018", "atunko:CORE_0018.1", "atunko:CORE_0018.3"})
    public ParsedSources get(Path projectDir, ProjectInfo info) {
        if (!enabled) {
            return parser.parseWithCapabilities(info);
        }
        Path key = projectDir.toAbsolutePath().normalize();
        Map<Path, Fingerprint> current = fingerprint(info);
        // compute() serializes callers per key: two sessions hitting the same cold project share one parse
        // instead of racing check-then-put into two. The holder keeps a strong reference so the soft one
        // cannot be reclaimed between compute() returning and this method returning.
        ParsedSources[] result = new ParsedSources[1];
        entries.compute(key, (k, cached) -> {
            if (cached != null
                    && cached.info().equals(info)
                    && cached.fingerprint().equals(current)) {
                result[0] = cached.parsed().get();
                if (result[0] != null) {
                    return cached;
                }
            }
            result[0] = parser.parseWithCapabilities(info);
            return new Entry(info, current, new SoftReference<>(result[0]));
        });
        return result[0];
    }

    private static Map<Path, Fingerprint> fingerprint(ProjectInfo info) {
        Map<Path, Fingerprint> result = new HashMap<>();
        for (Path dir : info.parseRoots()) {
            walkRegularFiles(dir, p -> result.put(p, fingerprintOf(p, true)));
        }
        for (Path buildFile : info.buildFiles()) {
            Path abs = buildFile.toAbsolutePath().normalize();
            if (Files.isRegularFile(abs)) {
                result.put(abs, fingerprintOf(abs, true));
            }
        }
        for (Path entry : info.classpath()) {
            Path abs = entry.toAbsolutePath().normalize();
            if (Files.isRegularFile(abs)) {
                result.put(abs, fingerprintOf(abs, false));
            } else {
                walkRegularFiles(abs, p -> result.put(p, fingerprintOf(p, false)));
            }
        }
        return result;
    }

    private static void walkRegularFiles(Path dir, java.util.function.Consumer<Path> action) {
        Path abs = dir.toAbsolutePath().normalize();
        if (!Files.isDirectory(abs)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(abs)) {
            walk.filter(Files::isRegularFile).forEach(action);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Fingerprint fingerprintOf(Path file, boolean hashContent) {
        try {
            long size = Files.size(file);
            long mtime = Files.getLastModifiedTime(file).toMillis();
            if (!hashContent) {
                return new Fingerprint(size, mtime, STAT_ONLY);
            }
            CRC32 crc = new CRC32();
            crc.update(Files.readAllBytes(file));
            return new Fingerprint(size, mtime, crc.getValue());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
