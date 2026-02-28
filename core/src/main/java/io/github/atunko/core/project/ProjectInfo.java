package io.github.atunko.core.project;

import java.nio.file.Path;
import java.util.List;

public record ProjectInfo(List<Path> classpath, List<Path> sourceDirs) {}
