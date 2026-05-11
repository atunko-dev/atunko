# LST Caching — Design Plan

Tracks: [#11 feat: LST caching](https://github.com/atunko-dev/atunko/issues/11)

## 1. Goal

Serialize parsed OpenRewrite `SourceFile` LSTs to disk so repeated recipe runs
against the same project skip re-parsing unchanged files. Parsing is the
dominant cost of an atunko run; this removes it from the hot path during
iterative recipe development.

## 1.1 Benefits

- **Parse time disappears from iterative runs.** OpenRewrite parsing dominates
  recipe-execution cost — typically 10–60 s for a 500-file / ~50 kLoC Java
  project, vs. 0.5–5 s for the actual recipe visit pass.
- **Cached second run: parse ≈ 0** for unchanged files; only the changed delta
  re-parses. Typical speedup **10–50×** on the parse phase.
- **Target workflow is iterative recipe development** — try a recipe, tweak
  config, re-run. Today that loop is ~30 s per iteration. With cache:
  sub-second after the first run.
- **Persistent across process exits.** Every CLI re-invocation benefits, which
  matches how atunko is actually used.
- **CI warm-start.** A cache warmed once (e.g. on a scheduled nightly) is
  reused across jobs on the same revision.
- **No correctness trade-off.** Cache miss always falls back to parsing;
  content-hash + `LstProvenance` version guards prevent stale reads. Worst
  case on upstream drift is "cold parse on next run."
- **Cost side is small:** ~512 MiB disk (configurable), zero cost on first run,
  negligible cost at cache-hit time (SHA-256 ≪ parse).

## 2. Scope

- In scope: transparent, content-hash-keyed on-disk LST cache in `atunko-core`,
  wired into `ProjectSourceParser`; configurable location, size, enable flag.
- Out of scope: cross-machine or network-shared caches; TUI / Web UX for cache
  management; warm-start pre-seeding of caches.

## 3. Architecture

### 3.1 Integration point

Intercept inside `ProjectSourceParser.parse(ProjectInfo)`
(`atunko-core/src/main/java/io/github/atunkodev/core/project/ProjectSourceParser.java`).
Single shared entry point used by CLI, TUI and Web; per-type parsers
(`JavaSourceParser`, XML / YAML / JSON / Properties) remain untouched.

```
for each file f in projectInfo:
    key = LstCacheKey.of(f)
    sf  = cache.get(key)
    if sf == null:
        sf = delegate.parse(f)
        cache.put(key, sf)
    sources.add(sf)
```

### 3.2 Serialization

OpenRewrite's `Tree` interface is annotated with

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@c",
              include = JsonTypeInfo.As.PROPERTY)
public interface Tree { ... }
```

in `rewrite-core/src/main/java/org/openrewrite/Tree.java`. This is **public API**
and makes every concrete LST node identifiable by its fully-qualified class
name during Jackson round-trip.

**What we found in rewrite OSS.** `org.openrewrite.internal.ObjectMappers
.propertyBasedMapper(...)` exists but, across the entire `openrewrite` GitHub
org, is only used for **recipe configuration** (YAML / JSON) — never for LST
persistence. LST disk serialization lives in Moderne's closed-source CLI
(`-ast.jar` bundles). The OSS side only carries the *readers*
(`FindLstProvenance`, `FindDeserializationErrors`) and provenance markers
(`LstProvenance.lstSerializerVersion`, `OutdatedSerializer`,
`DeserializationError`).

**Design consequence: atunko avoids the `internal` package entirely.**
`SerializedLstCodec` builds its own `ObjectMapper` (~ 15 LOC) configured
identically to what OpenRewrite uses internally:

```java
JsonMapper.builder()
    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
    .configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER, true)
    .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
    .build()
    .registerModule(new ParameterNamesModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
```

Polymorphism rides on the public `@JsonTypeInfo` on `Tree` — no atunko-side
type registration required. The only rewrite-core types we reference are
`SourceFile` and the marker classes (all public API). **Zero `internal`-package
coupling.**

**Relationship to Moderne's `-ast.jar`.** Moderne's serializer also rides on
the same public `@JsonTypeInfo` scheme — it must, to interop with OSS-side
`LstProvenance` / `OutdatedSerializer` markers. Its packaging (jar container,
likely Jackson-Smile binary encoding, index files) is proprietary. atunko's
cache is *format-adjacent* but uses its own container (one gzip-per-entry);
we make no compat claim with `-ast.jar`.

Wire format (per cache entry):

```
magic:     4 bytes  "ATLK"
version:   int32    atunko cache schema version
rewriteV:  utf-8 length-prefixed string   (e.g. "3.25.0")
lstSerV:   utf-8 length-prefixed string   (from LstProvenance, nullable → "")
payload:   gzipped JSON (polymorphic) from atunko's ObjectMapper
```

On magic / schema-version / `rewriteV` / `lstSerV` mismatch the entry is
evicted and treated as a miss.

### 3.3 Key & invalidation

Checksum-based. Timestamps are unreliable across git checkouts and worktrees.

```
LstCacheKey = SHA-256( fileBytes
                     | "\0" | relativePath
                     | "\0" | parserTypeTag   // "java", "xml", "yaml", ...
                     | "\0" | rewriteBomVersion )
```

Including `parserTypeTag` prevents aliasing if a file is ever reclassified.
Including `rewriteBomVersion` invalidates the entire cache on OpenRewrite
upgrade without scanning.

Layout on disk:

```
<cacheDir>/
  lst/
    ab/cdef012345.../<key>.bin
```

Flat, hash-sharded (2 hex chars → ≤ 256 shard dirs). No separate index file.

### 3.4 Eviction

Pure filesystem-mtime LRU:

- **On cache hit** (`cache.get`): `Files.setLastModifiedTime(entry, now())` —
  one syscall (~μs). This is the LRU "touch". We do **not** rely on access-time
  (`atime`) because many filesystems mount with `noatime`.
- **On cache write** (`cache.put`): after the write, if `cacheDir` total size
  > `maxSizeBytes`, walk the flat shard tree, sort entries by mtime ascending,
  delete oldest until size < 90 % of the limit.
- No background thread.

**Scale analysis.** The cache structure is flat-hash-sharded — the source
project's directory count is irrelevant (we never mirror the project tree in
the cache). For a 10 k-entry cache, `Files.walk` + sort by mtime is sub-second
on any modern filesystem; the limit is `maxSizeBytes` (512 MiB default
≈ 50 k small LSTs), not inode count. No separate index file or embedded DB
needed.

**Concurrency.** Two atunko processes could trigger concurrent eviction
against the same cache dir (e.g. two shells in the same project). `cache.put`
writes via a `tmp → atomic rename` dance; eviction tolerates
`NoSuchFileException` races (skip the vanished entry). No global lock.

### 3.5 Location

Use **`dev.dirs:directories` (`directories-jvm`, v26)** — a ~30 KB
MIT-licensed library from dirs-dev that returns platform-correct directories:
XDG on Linux / BSD, Known Folder API on Windows, Standard Directories on
macOS.

Default cache dir resolves to:

| Platform | Path                                                    |
|----------|---------------------------------------------------------|
| Linux    | `$XDG_CACHE_HOME/atunko/lst` → `~/.cache/atunko/lst`    |
| macOS    | `~/Library/Caches/dev.atunkodev.atunko/lst`             |
| Windows  | `%LOCALAPPDATA%\atunkodev\atunko\cache\lst`             |

Rationale:

- The cache is **regenerable** and **machine-local** — XDG cache dir is the
  spec-defined home for exactly this.
- **Lifetime:** persistent across runs (the whole point), safe to nuke.
- **Visibility:** low. User never sees `~/.cache/`; appropriate for a
  transparent perf cache.
- **Per-project caches rejected** — forces re-parse after every `git clone`,
  doesn't amortise across worktrees / branches, adds `.gitignore` hygiene.
- **`java.io.tmpdir` rejected** — OS may wipe between reboots, defeating the
  cache on the next morning's first run.
- **Hand-rolled XDG util rejected** — three platforms, edge cases (BSD,
  Windows portable installs); directories-jvm is tiny, widely used,
  zero-maintenance.

### 3.6 Configuration

New `CacheConfig` record in `io.github.atunkodev.core.config`:

```java
public record CacheConfig(boolean enabled, Path location, long maxSizeBytes) {
    public static CacheConfig defaults() { /* directories-jvm, 512 MiB, enabled */ }
}
```

Three config surfaces, resolution highest-wins:

1. **CLI flags** on `run` / `list-cache` / `clear-cache`: `--cache-dir <path>`,
   `--no-cache`.
2. **Env vars:** `ATUNKO_CACHE_DIR`, `ATUNKO_CACHE_DISABLED=1` (CI-friendly).
3. **YAML config file:** `$XDG_CONFIG_HOME/atunko/config.yaml` (fallback
   `~/.config/atunko/config.yaml`), loaded via `CacheConfigService` mirroring
   `RunConfigService`.
4. Built-in defaults: 512 MiB, enabled, XDG cache dir.

## 4. Files to add / modify (implementation PR)

**New (atunko-core)**

- `cache/LstCache.java`
- `cache/LstCacheKey.java`
- `cache/SerializedLstCodec.java`
- `config/CacheConfig.java`
- `config/CacheConfigService.java`

**Modified**

- `project/ProjectSourceParser.java` — interpose cache read / write.
- `AppServices.java` — wire singletons.
- `atunko-cli/.../RunCommand.java` — add `--cache-dir` / `--no-cache`.
- `gradle/libs.versions.toml` + `atunko-core/build.gradle` — add
  `dev.dirs:directories:26`.

**Docs / traceability**

- `docs/reqstool/` — new requirement `CORE_0010` (LST caching) with SVCs.
- `openspec/changes/add-lst-caching/` — proposal + tasks + delta spec for
  `recipe-execution`.

## 5. Reuse

- `RunConfigService` (`core/config/RunConfigService.java`) — YAML IO pattern.
- `AppServices` (`core/AppServices.java`) — singleton wiring.
- Fixture layout in `atunko-core/src/test/resources/fixtures/` — integration
  tests.
- reqstool `@SVCs` annotation pattern from `ProjectSourceParserTest`.

## 6. Risks & open questions

- **No `internal`-package coupling.** atunko builds its own `ObjectMapper`
  and rides on the public `@JsonTypeInfo` on `Tree` (§3.2). The only
  rewrite-core API surface we depend on is public (`SourceFile`, marker
  classes, `@JsonTypeInfo` on `Tree`). If upstream refactors `Tree`'s
  polymorphism scheme or changes concrete LST class names, cache entries
  fail to deserialize → evicted → miss → fall back to parsing. No
  correctness loss; worst case is "cache rebuilds on next run." Detection
  (§7) ensures drift surfaces as a CI failure, not a production surprise.
  `rewriteBomVersion` and `LstProvenance.lstSerializerVersion` in the entry
  header provide versioned invalidation.
- **Jackson LST round-trip is not guaranteed byte-exact.** `printAll()` must
  still match original source bytes — verified with a round-trip test that
  parses, serializes, deserializes, and compares `printAll()` against the
  original file.
- **SHA-256 cost vs. parse cost.** For a 100 KB file, SHA-256 ≈ sub-ms vs.
  parse ≈ 10–100 ms. Net win by ~2 orders of magnitude; no mitigation
  needed.
- **Cross-version cache poisoning** (another atunko version using a different
  schema). 4-byte magic + schema version in the entry header handles it —
  mismatched entries are evicted on read.
- **Does polymorphic Jackson round-trip handle every `SourceFile` subtype we
  parse?** `Tree` is `@JsonTypeInfo(use = Id.CLASS)` so in principle yes:
  every subtype is identifiable by FQCN. **Unverified for the specific
  subtypes atunko produces** — `J.CompilationUnit`, `Xml.Document`,
  `Yaml.Documents`, `Json.Document`, `Properties.File`. Known edge cases
  from the OSS repo: `Quark` and `Binary` opaque sources, sources carrying
  `DeserializationError` markers from prior failed round-trips. **Gate:**
  the implementation PR must include a round-trip test *per subtype* (§7,
  §8). Any subtype that fails round-trip is downgraded to "not cached"
  rather than blocking the feature — pass-through is always safe.

## 7. Detecting upstream API drift

Layered defences so rewrite upstream changes surface as CI failures, not
silent bit-rot in the cache:

1. **Compile-time** — any rename / move / signature change breaks the
   `atunko-core` build. Baseline, free.
2. **Golden-blob contract test.** Commit serialized LST fixtures at
   `atunko-core/src/test/resources/cache/golden-<lang>-v<rewrite-version>.bin`
   for each parser type. A test deserializes each blob and asserts
   `printAll()` equals the expected source bytes. Wire-format drift fails
   loudly.
3. **Round-trip invariant test.** For each parser type, a fixture round-trip:
   parse → `codec.write` → `codec.read` → `printAll()` compared byte-for-byte
   with the original. Catches silent semantic regressions that still happen
   to parse as JSON.
4. **`LstProvenance.lstSerializerVersion` tracking.** OpenRewrite stamps this
   on parsed sources. `SerializedLstCodec` records it in the cache entry
   header alongside `rewriteBomVersion`; on read, mismatch → miss + evict.
   Free upstream-versioned invalidation.
5. **Renovate / Dependabot on the rewrite BOM.** Auto-PRs for version bumps
   run (2) and (3) in CI — an upstream breaking change appears as a red
   check on the bump PR, we notice before cutting an atunko release.
6. **Explicitly declined:** `japicmp` / `revapi` Gradle plugins. They compare
   binary compatibility between versions but are noisy on `internal`
   packages and would flag churn that does not affect our codec. Round-trip
   + golden-blob tests are higher-signal for our use case.

**Operational policy.** If (2) or (3) fails on a rewrite BOM upgrade, bump
`ATUNKO_CACHE_SCHEMA_VERSION` (the int32 in the entry header, §3.2). All
existing cached entries are invalidated on next read; users see a one-time
cache rebuild, then resumed speedup.

## 8. Verification plan (implementation PR)

- **Unit:** codec round-trip, LRU eviction, hash-key stability, disabled-cache
  pass-through.
- **Integration:** `ProjectSourceParserCacheTest` parses a fixture twice;
  asserts second run hits cache and returns `printAll()`-equivalent sources.
- **Invalidation:** mutate a fixture file between runs, assert re-parse.
- **Schema-mismatch:** write entry with bogus magic / version, assert miss +
  evict.
- **Manual timing:**
  `./gradlew :atunko-cli:run --args="run <recipe> --project-dir <proj>"` twice;
  second run visibly faster.
- **Full build:** `./gradlew build` green — Spotless + Checkstyle + Error
  Prone + all tests.
- **openspec:** `openspec validate add-lst-caching --strict` passes.

## 9. Implementation order (follow-up PR)

1. Branch `feat/lst-caching`; draft PR referencing #11 (DCO sign-off).
2. openspec change proposal; validate `--strict`.
3. reqstool requirement + SVC stubs.
4. `CacheConfig` + `CacheConfigService` + tests.
5. `SerializedLstCodec` + round-trip matrix (Java, XML, YAML, JSON,
   Properties).
6. `LstCache` (put / get / evict) + unit tests.
7. Wire into `ProjectSourceParser`; `ProjectSourceParserCacheTest`.
8. CLI flags in `RunCommand`; env-var + YAML-file resolution.
9. `./gradlew spotlessApply && ./gradlew build`; fix all static-analysis
   findings.
10. Mark PR ready — Conventional Commits title:
    `feat(core): LST caching with checksum-based invalidation (#11)`.
