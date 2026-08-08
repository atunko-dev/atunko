package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Session-lifetime in-memory cache of {@link ParsedSources}, keyed by project directory. Parsing dominates
 * recipe-execution cost, so long-lived sessions (TUI, Web UI) parse each project once and re-parse only when its
 * files actually changed.
 *
 * <p>Staleness is detected by fingerprinting the same files the parser would read — everything under the project's
 * source and resource directories plus its build files — as (size, mtime) per path. Any difference (modified, added
 * or removed file) discards the entry and re-parses the whole project; a directory walk costs milliseconds against a
 * parse that costs seconds to minutes. Per-file re-parse is deliberately not attempted: Java sources resolve types
 * across files, so splicing a single re-parsed file into a cached list is not sound.
 *
 * <p>Disabled via system property {@code atunko.lst.cache.disabled=true} (or the explicit constructor), in which case
 * every call delegates straight to the parser and nothing is stored.
 */
@Requirements({"atunko:CORE_0018"})
public class ParsedSourcesCache {

    /** System property that disables caching entirely when set to {@code true}. */
    public static final String DISABLE_PROPERTY = "atunko.lst.cache.disabled";

    private record Entry(ProjectInfo info, Map<Path, Fingerprint> fingerprint, ParsedSources parsed) {}

    private record Fingerprint(long size, long lastModifiedMillis) {}

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
     * Returns the parsed sources for the project, from cache when its files are unchanged since the last parse,
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
        Entry cached = entries.get(key);
        if (cached != null && cached.info().equals(info) && cached.fingerprint().equals(current)) {
            return cached.parsed();
        }
        ParsedSources parsed = parser.parseWithCapabilities(info);
        entries.put(key, new Entry(info, current, parsed));
        return parsed;
    }

    private static Map<Path, Fingerprint> fingerprint(ProjectInfo info) {
        Map<Path, Fingerprint> result = new HashMap<>();
        var dirs = info.allSourceAndResourceDirs();
        if (dirs.isEmpty()) {
            dirs = info.sourceDirs();
        }
        for (Path dir : dirs) {
            Path abs = dir.toAbsolutePath().normalize();
            if (!Files.isDirectory(abs)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(abs)) {
                walk.filter(Files::isRegularFile).forEach(p -> result.put(p, fingerprintOf(p)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        for (Path buildFile : info.buildFiles()) {
            Path abs = buildFile.toAbsolutePath().normalize();
            if (Files.isRegularFile(abs)) {
                result.put(abs, fingerprintOf(abs));
            }
        }
        return result;
    }

    private static Fingerprint fingerprintOf(Path file) {
        try {
            return new Fingerprint(
                    Files.size(file), Files.getLastModifiedTime(file).toMillis());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
