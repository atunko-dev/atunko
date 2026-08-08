package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.openrewrite.SourceFile;

/**
 * The result of parsing a project: the parsed {@link SourceFile}s together with the set of {@link SourceCapability}
 * they represent. The capability set is what recipe applicability is judged against, so it must describe what was
 * actually parsed — never what merely exists on disk.
 *
 * @param sources parsed source files, in parse order
 * @param capabilities capabilities actually produced by the parse
 */
@Requirements({"atunko:CORE_0015.1"})
public record ParsedSources(List<SourceFile> sources, Set<SourceCapability> capabilities) {

    public ParsedSources {
        sources = List.copyOf(sources);
        capabilities = capabilities.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }

    public static ParsedSources empty() {
        return new ParsedSources(List.of(), Set.of());
    }

    public boolean has(SourceCapability capability) {
        return capabilities.contains(capability);
    }
}
