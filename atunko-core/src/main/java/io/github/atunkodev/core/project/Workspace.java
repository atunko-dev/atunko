package io.github.atunkodev.core.project;

import io.github.reqstool.annotations.Requirements;
import java.nio.file.Path;
import java.util.List;

/** A set of discovered projects rooted at a common directory. */
@Requirements({"atunko:CORE_0010"})
public record Workspace(Path root, List<ProjectEntry> projects) {

    public Workspace(Path root, List<ProjectEntry> projects) {
        this.root = root;
        this.projects = List.copyOf(projects);
    }
}
