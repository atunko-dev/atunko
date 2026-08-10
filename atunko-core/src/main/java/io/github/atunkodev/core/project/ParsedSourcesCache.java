package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

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

    private record Entry(ProjectInfo info, InputFingerprint fingerprint, SoftReference<ParsedSources> parsed) {}

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
        InputFingerprint current = fingerprint(info);
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

    private static InputFingerprint fingerprint(ProjectInfo info) {
        InputFingerprint.Builder builder = InputFingerprint.builder();
        for (Path dir : info.parseRoots()) {
            builder.tree(dir, p -> true, true);
        }
        for (Path buildFile : info.buildFiles()) {
            builder.file(buildFile, true);
        }
        for (Path entry : info.classpath()) {
            builder.fileOrTree(entry, false);
        }
        return builder.build();
    }
}
