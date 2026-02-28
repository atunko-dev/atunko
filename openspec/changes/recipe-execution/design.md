## Context

`RecipeDiscoveryService` (CORE_0001, CORE_0002) lets users find recipes but not run them.
CORE_0003 adds the ability to execute a recipe against parsed source files. The core module
already depends on rewrite-core and rewrite-java — no new dependencies are needed.

OpenRewrite's execution model: parse source files into a `LargeSourceSet`, activate a `Recipe`
by name from the `Environment`, run it with an `ExecutionContext`, and collect `Result` objects
that contain before/after source.

## Goals / Non-Goals

**Goals:**
- Execute a single recipe by name against a set of parsed source files
- Return structured results: which files changed, with before/after content
- Keep the API simple — callers provide source files and a recipe name, get results back

**Non-Goals:**
- Classpath resolution or project scanning (CORE_0004, separate change)
- Writing modified files back to disk (caller's responsibility)
- Chaining multiple recipes in sequence
- Recipe option/parameter configuration (future enhancement)
- Progress reporting or cancellation during execution

## Decisions

### 1. New `RecipeExecutionEngine` class in `core.engine` package

**Decision**: Create `RecipeExecutionEngine` with an `execute(String recipeName, List<SourceFile> sources)`
method that returns an `ExecutionResult`.

**Rationale**: Keeps execution separate from discovery. The `engine` package is already scaffolded
and is the natural home. A single method accepting pre-parsed sources keeps the API decoupled from
project scanning (CORE_0004).

**Alternatives considered**:
- Adding `execute()` to `RecipeDiscoveryService` — violates single responsibility; discovery and
  execution are different concerns
- Accepting `Path` instead of `List<SourceFile>` — couples execution to file I/O and project scanning

### 2. Accept `List<SourceFile>` as input (not file paths)

**Decision**: The execution engine accepts already-parsed OpenRewrite `SourceFile` objects.

**Rationale**: Decouples execution from parsing/scanning. Project scanning (CORE_0004) will handle
parsing source files with the correct classpath. For testing, we can parse a small fixture project
inline. This separation makes each component independently testable.

### 3. Return a structured `ExecutionResult` record

**Decision**: Return an `ExecutionResult` record containing a list of `FileChange` records
(path, before content, after content) and summary metadata.

**Rationale**: Callers (CLI, TUI) need structured data to display diffs or write changes. A record
is immutable and serializable. Returning before/after lets callers decide what to do (display, write,
discard).

### 4. Use `InMemoryExecutionContext` for now

**Decision**: Create execution context internally using `InMemoryExecutionContext`.

**Rationale**: Simplest option that works. No need for external context configuration at this stage.
Can be made configurable later if recipe options or execution timeouts are needed.

### 5. Test with a minimal Java fixture project

**Decision**: Create a test fixture at `core/src/test/resources/fixtures/java-with-unused-imports/`
containing a Java file with unused imports. Parse it with `JavaParser` in the test.

**Rationale**: SVC_CORE_0003 specifies testing with `RemoveUnusedImports`. A real Java file with
unused imports is the simplest fixture that proves execution works end-to-end. Parsing in the test
(rather than in the engine) aligns with Decision #2.

## Risks / Trade-offs

- **JavaParser classpath**: `JavaParser` needs classpath JARs to resolve types. For the test fixture
  (simple file with only JDK imports), an empty classpath works. For real projects, CORE_0004 will
  supply the classpath. → Acceptable for PoC; revisit when integrating with project scanning.
- **Large source sets**: `LargeSourceSet.build()` vs `InMemoryLargeSourceSet` — need to check which
  API is available in rewrite-core 8.74.x. → Fall back to whichever is available; both work for our
  use case.
