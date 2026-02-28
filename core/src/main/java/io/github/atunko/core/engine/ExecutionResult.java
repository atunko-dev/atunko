package io.github.atunko.core.engine;

import java.util.List;

public record ExecutionResult(List<FileChange> changes) {}
