package io.github.atunkodev.daemon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * The identity a daemon is keyed on: a client only reuses a daemon whose version string equals its own.
 *
 * <p>From a released JAR this is the manifest's {@code Implementation-Version}. From a development run there is no
 * manifest, and "no version" would be actively dangerous — every rebuild would keep talking to a daemon still
 * holding classes compiled from the previous edit, so a developer would test their change against old code. The
 * development identity therefore folds in the newest modification time on the classpath, which changes on every
 * rebuild and retires the stale daemon automatically.
 */
public final class AtunkoVersion {

    private static final String DEVELOPMENT_PREFIX = "dev-";

    private AtunkoVersion() {}

    public static String current() {
        String declared = AtunkoVersion.class.getPackage().getImplementationVersion();
        if (declared != null && !declared.isBlank()) {
            return declared;
        }
        return DEVELOPMENT_PREFIX + Long.toHexString(newestClasspathModification());
    }

    /** Stat-only walk: cheap enough to run once per CLI invocation, and it never reads file content. */
    private static long newestClasspathModification() {
        long newest = 0L;
        for (String entry : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
            if (entry.isBlank()) {
                continue;
            }
            newest = Math.max(newest, newestUnder(Path.of(entry)));
        }
        return newest;
    }

    private static long newestUnder(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                return Files.getLastModifiedTime(path).toMillis();
            }
            if (!Files.isDirectory(path)) {
                return 0L;
            }
            try (Stream<Path> walk = Files.walk(path)) {
                return walk.filter(Files::isRegularFile)
                        .mapToLong(AtunkoVersion::lastModifiedOrZero)
                        .max()
                        .orElse(0L);
            }
        } catch (IOException e) {
            // An unreadable classpath entry simply does not contribute to the identity.
            return 0L;
        }
    }

    private static long lastModifiedOrZero(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
