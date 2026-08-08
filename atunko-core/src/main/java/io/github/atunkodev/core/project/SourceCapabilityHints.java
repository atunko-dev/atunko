package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Best-effort capabilities for a project that has not been parsed yet.
 *
 * <p>Parsing only happens when a run is started, but the recipe browser badges inapplicable recipes from the moment it
 * opens. Without a hint every {@code org.openrewrite.maven.*} recipe would be badged "needs Maven" on a Maven project
 * until the user's first run — badges that are wrong in the common case.
 *
 * <p>The hint is deliberately narrow: it reports {@link SourceCapability#MAVEN} exactly when the project root has a
 * {@code pom.xml}, which is the same signal {@link ProjectScannerFactory} uses to choose the Maven scanner. It is
 * replaced wholesale by the real capability set as soon as {@link ProjectSourceParser} has parsed the project, so a
 * pom that turns out not to resolve corrects itself on the first run.
 *
 * <p>{@link SourceCapability#GRADLE} is never hinted: Gradle build files are not parsed at all, so no run can ever
 * make Gradle recipes applicable and pretending otherwise would be dishonest.
 */
@Requirements({"atunko:CORE_0016.3"})
public final class SourceCapabilityHints {

    private SourceCapabilityHints() {}

    /** Capabilities a project directory is expected to yield once parsed. */
    @Requirements({"atunko:CORE_0016.3"})
    public static Set<SourceCapability> forProjectDir(Path projectDir) {
        if (projectDir == null) {
            return Set.of();
        }
        Path pom = projectDir.toAbsolutePath().normalize().resolve("pom.xml");
        return Files.isRegularFile(pom) ? Set.of(SourceCapability.MAVEN) : Set.of();
    }
}
