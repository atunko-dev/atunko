# Tasks

## 1. reqstool

- [x] 1.1 Add requirements CORE_0018 (in-process LST caching), CORE_0018.1
      (fingerprint invalidation), CORE_0018.2 (per-project scope), CORE_0018.3
      (disable) to `docs/reqstool/requirements.yml`
- [x] 1.2 Add SVCs SVC_CORE_0018(.1/.2/.3) to
      `docs/reqstool/software_verification_cases.yml`

## 2. Core: ParsedSourcesCache

- [x] 2.1 Tests first: `ParsedSourcesCacheTest` — unchanged project served from
      cache (parser invoked once); modified/added/removed source file triggers
      re-parse; build-file change triggers re-parse; two projects invalidate
      independently; disabled cache parses every call
- [x] 2.2 Implement `ParsedSourcesCache` per design (fingerprint of
      path→(size, mtime) over source/resource dirs + build files;
      `ConcurrentHashMap` keyed by projectDir; enabled flag)
- [x] 2.3 Wire shared instance through `AppServices` (system property
      `atunko.lst.cache.disabled`)
- [x] 2.4 `@Requirements` annotations on cache, `@SVCs` on tests

## 3. Call sites

- [x] 3.1 `WorkspaceExecutionEngine` parses through the cache
- [x] 3.2 `TuiController` run path parses through the cache
- [x] 3.3 `RecipeBrowserView` run/preview paths parse through the cache

## 4. Docs

- [x] 4.1 Mark `PLAN_LST.md` superseded; extend `PLAN_LST_DAEMON.md` with the
      Phase 1 implementation section
- [ ] 4.2 Update PR #53 title/description (docs → feat)

## 5. Verify

- [x] 5.1 `./gradlew spotlessApply` then `./gradlew build` green
