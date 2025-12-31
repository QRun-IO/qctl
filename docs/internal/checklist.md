# Remaining design decisions to unblock code generation (V1)

Lock these choices before scaffolding code. Keep scope limited to V1.

## Build baseline

- Java LTS: choose exact version (e.g., Temurin 21)
- Maven: exact version; enable Maven Wrapper
- GraalVM: exact version for native builds (macOS/Linux)
- Root/module POM structure: reactor layout, modules list, groupId/artifactId
- Java Toolchains: configure target LTS across modules

## Dependencies (pin versions)

- CLI: Picocli
- Config/JSON/YAML: Jackson (JSON), Jackson YAML (or SnakeYAML)
- JSON Schema validation: library + draft support (2020-12)
- HTTP client: choose JDK HttpClient vs OkHttp (retries/backoff, timeouts)
- Templates: Handlebars (jknack) with partials/helpers
- Crypto: BouncyCastle (PGP) for warn-only signatures in V1
- Logging: SLF4J + backend (e.g., logback) or simple JUL bridge
- Testing: JUnit 5, AssertJ, Mockito, Testcontainers, JaCoCo
- Quality: Spotless, Checkstyle, Error Prone, Qodana config

## Project/package structure

- Base package: e.g., `io.qrun.qctl`
- Modules and responsibilities:
   - `qctl-core`: entrypoint, global flags, output/rendering, completion/man
   - `qctl-shared`: config loader/merger/validation, HTTP, auth, crypto, logging
   - `qctl-qqq`, `qctl-qbit`, `qctl-qrun`, `qctl-qstudio`: mode-specific logic
   - `qctl-integration-tests`: mock-backed contract and e2e tests
- Public APIs between modules; avoid cyclic dependencies

## Config implementation

- Discovery order and precedence: built-in < global < project < env `QCTL_*` < flags
- File locations per OS (from design)
- Env var parsing: booleans, arrays, dot-path mapping; case-insensitive values where applicable
- Top-level `output` default vs `--output` override
- Error reporting: JSON Pointer paths, Problem Details for CLI validation where helpful

## Output and logging

- Output renderer: text vs `--output json` structure
- Color rules: `--color auto|always|never`
- Logging: log levels, log format (text/json) mapping, where logs vs results print (stderr/stdout)

## HTTP layer

- Client choice (JDK vs OkHttp)
- Default timeouts: connect/write/read
- Retries/backoff: exponential with jitter; respect Retry-After for 429/503
- Headers: Idempotency-Key, auth, user-agent/version

## Auth (V1)

- Device flow stub UX (OIDC), fallback for mock (`X-API-Key`)
- Token store: OS keychain by default; file fallback path/perms in V1
- Profiles: default profile selection/override rules

## Error/exit mapping

- Implement mapping from RFC7807 + HTTP codes to exit codes per the table in DESIGN-2.md
- CLI-level validation → exit 2; integrity warnings (V1) → do not fail

## qqq (templates)

- Template manifest shape (min fields for V1)
- Handlebars features enabled; default helpers; partials support
- Local vs remote template rules; trust/warn model (warn-only V1)
- Post-gen hooks execution model; `--dry-run` behavior

## qbit (packages)

- Lockfile (`qbits.lock`) read/write schema and atomicity
- Vendor directory layout; symlink strategy for linked mode (if any in V1)
- Resolution algorithm: caret ranges, dedupe, hermetic vs offline behavior
- Integrity hash algorithm (sha512) and warning text (V1 warn-only)

## qrun (package/publish)

- Jib integration: programmatic config or POM snippet injection
- OCI label injection (frozen set from DESIGN-2.md)
- Registry auth and endpoint configuration
- Verify-by-digest (HEAD OCI manifest) implementation; stub out of mock

## qstudio (offline)

- Ledger storage: path (e.g., `.qctl/ledger/`), file naming, rotation rules
- Plan output location: `plan.md` placement and overwrite behavior
- Allow/deny globbing implementation and defaults

## Cache commands

- Cache layout: templates/qbits/artifacts/trust/locks (paths per OS)
- LRU index format and eviction policy
- `cache ls`: output format (text/json)
- `cache prune`: `--max-size` parsing (e.g., KB/MB/GB); idempotency
- `cache clean --all`: safety prompts or `--yes` behavior

## Telemetry (opt-in)

- Local event storage path; retention/rotation
- Event write timing (on exit) and minimal fields (install_id, command, duration_ms, exit_code, version, os/arch, timestamp)
- No network in V1; flag `--telemetry.enabled` overrides config

## OpenAPI integration

- DTOs: generate from OpenAPI vs hand-write POJOs for V1
- If generate: generator, packages, ignore rules, and example stubbing
- Client: minimal wrapper over HTTP with typed models

## CLI UX details

- Exact command/flag grammar for each subcommand (finalize names, help text order)
- Completion/man generation wiring (Picocli autocompletion)

## Testing strategy

- Golden snapshots: file locations, update workflow, normalization of volatile data
- Schema validation against OpenAPI for JSON outputs
- Mock server startup in tests and endpoint base URL injection
- Error-path tests for 401/404/429/503 and exit code assertions
- Idempotency replay tests (201 → 200 with same key)

## CI/CD scaffolding

- CI matrix: build, test, native image for macOS/Linux; Windows jlink zip
- Static analysis (Qodana) and coverage thresholds
- Artifact publishing (archives only in V1)

---

When these are pinned, proceed to:

1) Scaffold reactor + modules, 2) Implement `qctl-core` foundation (config, logging, output), 3) Add mode-by-mode features per V1 checklist, 4) Wire tests/golden/contract, 5) Build native/jlink artifacts.
