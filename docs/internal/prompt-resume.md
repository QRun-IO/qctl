You are an AI coding assistant resuming development of the qctl CLI (Java, multi-module Maven) on a new machine. Follow the design docs in this repo and continue the V1 implementation exactly as specified.

Context and current state

- Design/source-of-truth:
   - DESIGN-2.md (V1 scope, OpenAPI, schemas, CLI rules)
   - CODEGEN-LOCKS-V1.md (pinned deps, module layout, behavior contracts)
   - cheaklist.md (remaining design decisions before code)
- Code scaffolded and building:
   - Maven reactor (root + modules): qctl-shared, qctl-core, qctl-qqq, qctl-qbit, qctl-qrun, qctl-qstudio, qctl-integration-tests
   - qctl-core:
      - Picocli main with SPI-loaded subcommands
      - Config loader + YAML + env overlay + JSON Schema validation (schema in resources)
      - System paths (config/cache), logging, Output (text/json)
      - ApiClient (java.net.http) with retries/backoff and RFC7807→exit mapping
      - TokenStore (file fallback) and auth subcommands: login --api-key, logout, whoami
      - Cache subcommands: cache ls | prune --max-size | clean --all
   - qctl-qrun: status command hitting mock API (uses X-API-Key from TokenStore)
   - qctl-qbit: Lockfile DTO + atomic read/write (YAML)
- Tests passing for core and qbit unit tests

Environment

- Java 21 (Temurin), Maven Wrapper or Maven 3.9.x ok
- Build: mvn -DskipTests package or mvn test

Your tasks (continue V1; do not exceed scope)

1) Output integration

- Respect --output json in commands. As a first step, switch qrun status to render JSON via qctl-core Output when --output=json; default to text otherwise. Ensure consistent stdout/stderr usage (results to stdout, logs to stderr).
- Add a small unit/integration test for the renderer behavior.

2) ApiClient tests

- Add unit tests for:
   - HeaderProvider invocation (e.g., inject a custom header and assert it reaches a minimal local handler)
   - Error mapping: simulate responses (401, 404, 429, 503) and assert ApiException.exitCode matches table in DESIGN-2.md. Use a simple mock HttpClient or wrap send() for testability (avoid heavy frameworks).

3) Cache tests

- Add tests for CachePruneCommand.parseSize (KB/MB/GB/B). Include invalid inputs handling (throw or document behavior). Add quick test for prune no-op when current size <= target (can stub file sizes using temporary files).

4) qrun publish (mock-only V1)

- Create skeleton commands for the publish flow that wire shapes only (no real OCI build):
   - qrun package: prepare a dummy artifact manifest in target/ (json with digest field)
   - qrun publish --env <env>: POST /v1/artifacts then POST /v1/releases to the mock API (use ApiClient), accept Idempotency-Key
   - Handle 201 and 200 (idempotent replay) as success; map errors via ApiClient
- Add minimal tests asserting request/response handling and idempotency behavior (can be unit-level, not full HTTP)

5) qbit resolver basics (no registry I/O yet)

- Implement Lockfile loading in a resolver service and a no-op resolve that reads existing entries (hermetic mode) and reports what would be installed. Print a simple summary; wire to a placeholder qctl qbit resolve command.
- Add small unit tests for hermetic vs non-hermetic branches (non-hermetic can just print a TODO for now).

6) Auth improvements (V1 expectations)

- whoami: if API key is present, print api-key: set; otherwise, print not set. Keep OIDC device-flow as TODO; do not add network calls.
- login/logout: keep current behavior; add unit tests that write and clear TokenStore.

7) Housekeeping

- Ensure all new code has basic tests; keep tests fast and hermetic.
- Follow CODEGEN-LOCKS-V1.md dependency versions and patterns; no new libs beyond what’s pinned.
- Keep output deterministic for golden tests (avoid volatile fields).

Constraints

- Do not implement post-V1 features (e.g., plugin enforcement, real OCI, streaming logs)
- Match exit code mapping and CLI rules in DESIGN-2.md
- Keep code readable (explicit names, small helpers, no deep nesting)

How to run

- Build: mvn -DskipTests package (or mvn test)
- Example: store API key for mock then call status
   - qctl auth login --api-key test-key
   - qctl qrun status --app demo-app --env dev --output json

Acceptance criteria

- All modules compile; unit tests pass locally (mvn test)
- qrun status respects --output json
- ApiClient tests cover header injection and error→exit code mapping
- Cache parseSize tests added
- qrun publish skeleton compiles and returns mocked success (no real push)
- qbit resolver skeleton reads lockfile and prints summary in hermetic mode

If something is ambiguous, consult DESIGN-2.md first, then CODEGEN-LOCKS-V1.md, and leave clear TODOs in tests (not code) where external services would be needed.

---

Session continuation notes (progress and next steps)

Summary of work completed
- Output integration: `qrun status` respects `--output json` via `qctl-core` `Output`.
- Tests added:
  - `qctl-qrun`: status renderer (text/json).
  - `qctl-core`: `ApiClientTest` (header provider injection; HTTP→exit code mapping without mocking JDK types).
  - `qctl-core`: `CachePruneCommandTest` (`parseSize` units, invalid inputs, no-op prune).
- qrun mock flows:
  - `qrun package`: writes dummy artifact manifest to `target/`.
  - `qrun publish`: POSTs to mock API with `Idempotency-Key`, handles 201/200.
- qbit resolver basics:
  - `qbit resolve`: hermetic lockfile reading and summary; tests included.
- Auth:
  - `whoami`: prints `api-key: set|not set` per V1.

Coding standards & build
- Kingsrook style enforced:
  - Checkstyle (validate) uses `codestyle/checkstyle.config.xml`, warnings are errors.
  - Spotless enforces license headers (Java/XML/YAML/Properties) and import cleanup.
- `codestyle/license.txt` is the source license header; equivalents for XML/hash comments exist.


Notes
- Stay within V1 scope; no new deps; follow CODEGEN-LOCKS-V1.md.
- Logs on stderr, results on stdout; make outputs deterministic for tests.
