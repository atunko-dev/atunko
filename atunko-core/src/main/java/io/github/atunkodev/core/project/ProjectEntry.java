package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;

/** Pairs a project's filesystem location with its resolved build metadata. */
@Requirements({"atunko:CORE_0010.5"})
public record ProjectEntry(Path projectDir, ProjectInfo info) {}
