package io.github.atunkodev.core.engine;

import java.nio.file.Path;

public record FileChange(Path path, String before, String after) {}
