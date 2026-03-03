package io.github.atunkodev.core.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record ProjectInfo(
        List<Path> classpath,
        List<Path> sourceDirs,
        List<Path> resourceDirs,
        List<Path> testSourceDirs,
        List<Path> testResourceDirs) {

    public ProjectInfo(List<Path> classpath, List<Path> sourceDirs) {
        this(classpath, sourceDirs, List.of(), List.of(), List.of());
    }

    public List<Path> allSourceAndResourceDirs() {
        List<Path> all = new ArrayList<>();
        all.addAll(sourceDirs);
        all.addAll(resourceDirs);
        all.addAll(testSourceDirs);
        all.addAll(testResourceDirs);
        return List.copyOf(all);
    }
}
