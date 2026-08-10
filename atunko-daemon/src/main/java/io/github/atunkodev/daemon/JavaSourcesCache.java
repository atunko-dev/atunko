package io.github.atunkodev.daemon;

import io.github.atunkodev.core.project.InputFingerprint;
import io.github.atunkodev.core.project.JavaSourceParser;
import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import java.util.List;
import org.openrewrite.SourceFile;

/**
 * Holds one project's parsed sources for the daemon's lifetime, re-parsing only when the input files changed.
 *
 * <p>Deliberately parses through {@link JavaSourceParser}, the same parser {@code atunko run} uses in-process. A
 * daemon that parsed more thoroughly would produce different recipe results than {@code --no-daemon}, and "the
 * daemon changes your output" is a far worse bug than "the daemon parses no better than the CLI did".
 *
 * <p>Invalidation reuses {@link InputFingerprint}, so the daemon and {@code ParsedSourcesCache} answer "did the
 * input change?" the same way. The input set mirrors {@link JavaSourceParser}'s own walk: every {@code .java} file
 * under the project root.
 *
 * <p>Whole-project granularity: any change re-parses everything. Splicing one re-parsed file back into the list is
 * unsound, because Java types resolve across files.
 */
@Requirements({"atunko:CORE_0023"})
public class JavaSourcesCache {

    private final JavaSourceParser parser;
    private final Path projectRoot;

    private InputFingerprint fingerprint;
    private List<SourceFile> sources;

    public JavaSourcesCache(Path projectRoot) {
        this(projectRoot, new JavaSourceParser());
    }

    public JavaSourcesCache(Path projectRoot, JavaSourceParser parser) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.parser = parser;
    }

    /** Number of parses performed — the observable that proves a request was served from cache. */
    private int parseCount;

    public int parseCount() {
        return parseCount;
    }

    /**
     * The project's parsed sources, from cache when nothing changed since the last parse.
     *
     * <p>Synchronized rather than lock-free: the daemon serializes requests per project anyway, and a second
     * caller arriving mid-parse should wait for that parse rather than start a competing one.
     */
    @Requirements({"atunko:CORE_0023"})
    public synchronized List<SourceFile> get() {
        InputFingerprint current = fingerprintNow();
        if (sources != null && current.equals(fingerprint)) {
            return sources;
        }
        sources = parser.parse(projectRoot);
        fingerprint = current;
        parseCount++;
        return sources;
    }

    private InputFingerprint fingerprintNow() {
        return InputFingerprint.builder()
                .tree(projectRoot, p -> p.toString().endsWith(".java"), true)
                .build();
    }
}
