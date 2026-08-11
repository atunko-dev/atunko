package io.github.atunkodev.core.project;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * A stamp of the files a parse consumed, used to decide whether a cached parse result is still valid.
 *
 * <p>Two independent caches need this — {@link ParsedSourcesCache} over a scanned {@link ProjectInfo}, and the
 * daemon over the plain {@code **}{@code /*.java} set that {@link JavaSourceParser} walks — and "has this input
 * changed?" is the load-bearing correctness question in both. It lives here so there is one answer, not two that
 * can drift apart.
 *
 * <p>Content is hashed for sources, whose bytes decide recipe output. Classpath entries are stamped by size and
 * mtime only: a rebuilt jar still invalidates, without paying to read every dependency on every lookup.
 */
public record InputFingerprint(Map<Path, FileStamp> stamps) {

    /** {@code contentCrc} is {@link #STAT_ONLY} when the file's bytes were deliberately not read. */
    public record FileStamp(long size, long lastModifiedMillis, long contentCrc) {}

    /** Sentinel {@code contentCrc} for files stamped without reading their content. */
    public static final long STAT_ONLY = -1;

    public InputFingerprint {
        stamps = Map.copyOf(stamps);
    }

    public static InputFingerprint empty() {
        return new InputFingerprint(Map.of());
    }

    /** Builder-free accumulation, since callers stamp several roots of differing kinds into one fingerprint. */
    public static final class Builder {
        private final Map<Path, FileStamp> stamps = new HashMap<>();

        /** Stamps one file, if it is a regular file. Missing files are simply absent from the fingerprint. */
        public Builder file(Path file, boolean hashContent) {
            Path abs = file.toAbsolutePath().normalize();
            if (Files.isRegularFile(abs)) {
                stamps.put(abs, stampOf(abs, hashContent));
            }
            return this;
        }

        /** Stamps every regular file under {@code dir} matching {@code filter}. A non-directory is ignored. */
        public Builder tree(Path dir, Predicate<Path> filter, boolean hashContent) {
            Path abs = dir.toAbsolutePath().normalize();
            if (!Files.isDirectory(abs)) {
                return this;
            }
            try (Stream<Path> walk = Files.walk(abs)) {
                walk.filter(Files::isRegularFile).filter(filter).forEach(p -> stamps.put(p, stampOf(p, hashContent)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return this;
        }

        /** Stamps {@code path} as a file when it is one, otherwise as a whole tree. */
        public Builder fileOrTree(Path path, boolean hashContent) {
            Path abs = path.toAbsolutePath().normalize();
            return Files.isRegularFile(abs) ? file(abs, hashContent) : tree(abs, p -> true, hashContent);
        }

        public InputFingerprint build() {
            return new InputFingerprint(stamps);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static FileStamp stampOf(Path file, boolean hashContent) {
        try {
            long size = Files.size(file);
            long mtime = Files.getLastModifiedTime(file).toMillis();
            if (!hashContent) {
                return new FileStamp(size, mtime, STAT_ONLY);
            }
            CRC32 crc = new CRC32();
            crc.update(Files.readAllBytes(file));
            return new FileStamp(size, mtime, crc.getValue());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
