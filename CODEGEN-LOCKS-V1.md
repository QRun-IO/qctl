## V1 Code-Gen Locks (finalized)

Use this as the source of truth to scaffold modules, POMs, deps, and initial code.

### Build baseline
- Java: Temurin 21.0.x (LTS)
- Maven: 3.9.7 (wrapper pinned)
- GraalVM: 21.0.2 (matching JDK 21)
- GroupId / base package: `io.qrun.qctl` / `io.qrun.qctl.*`
- Modules:
  - root POM (reactor)
  - `qctl-shared` (DTOs, utils)
  - `qctl-core` (config, logging, http, auth, telemetry, cache)
  - `qctl-qqq`, `qctl-qbit`, `qctl-qrun`, `qctl-qstudio` (commands)
  - `qctl-integration-tests` (Testcontainers, golden tests)
- Toolchain (root `pom.xml`):

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-toolchain-plugin</artifactId>
  <version>3.2.0</version>
  <configuration>
    <toolchains>
      <jdk>
        <version>21</version>
        <vendor>eclipse</vendor>
      </jdk>
    </toolchains>
  </configuration>
</plugin>
```

### Dependency matrix (pin exact versions)
- Picocli: 4.7.5 (+ `picocli-codegen` for completion/man)
- Jackson: BOM 2.17.1 (core, databind, dataformat-yaml)
- JSON Schema: `com.networknt:json-schema-validator:1.4.1`
- HTTP: JDK HttpClient (`java.net.http`) — no OkHttp in V1
- Templating: `com.github.jknack:handlebars:4.4.0`
- PGP: `org.bouncycastle:bcpg-jdk18on:1.78.1`
- SLF4J: API 2.0.13; backend Logback 1.5.6 (JSON via jackson encoder)
- Retry/backoff: homegrown (tiny util in `qctl-core`)
- Tests: JUnit 5.10.2, AssertJ 3.25.3, Mockito 5.12.0, Testcontainers BOM 1.19.7
- Code quality: Spotless 2.43.0 (GJF 1.17.0), Checkstyle 10.15.0, Error Prone 2.28.0 (via EP Maven plugin)
- Native: `native-image-maven-plugin` 22.3+ (cfg per-OS)

### Package & project structure
- `qctl-shared`: ULID, RFC7807 types, small I/O utils, path/glob utils, SemVer helper
- `qctl-core`:
  - Config loader/merger + env overlay
  - Logger setup & output formatters
  - HTTP client + retry/jitter + auth header injector
  - Auth device-flow stub + token store abstraction
  - Telemetry writer
  - Cache/LRU + integrity utilities
- Feature modules (`qctl-qqq`, `qctl-qbit`, `qctl-qrun`, `qctl-qstudio`) expose Picocli commands; depend on `qctl-core` and `qctl-shared`
- `qctl-integration-tests`: launches mock (Prism), runs golden/contract tests

### Config implementation details
- Discovery/merge order (as defined): built-in < global < project < env `QCTL_*` < flags; later wins
- Error messages: JSON Pointer path + short reason
  - Example: Config error at `/qbit/resolution/preferLatestMinor`: expected boolean, got "yes"
- `--output` precedence: flag overrides config for the invocation only
- Env vars: `QCTL_` prefix, uppercase keys with `_` mapping to nested fields (e.g., `QCTL_QRUN_DEFAULTENV=stage`)
- Booleans: true if {1,true,yes,on}; false if {0,false,no,off} (case-insensitive)
- Arrays: JSON array or comma-separated with trimming
- URI fields validated per schema `format: uri`

### Logging & output
- Facade: SLF4J; backend: Logback
- Levels: default INFO; `--debug` => DEBUG; `--verbose` adds CLI prints (does not raise logger level unless combined with `--debug`)
- Formats:
  - Text: `[time] level module - msg` with optional color
  - JSON: one-line JSON (logstash-logback-encoder)
- Color: auto = enabled on TTY and `NO_COLOR` not set; `always`/`never` obeyed
- Streams: logs → stderr; command results (per `--output`) → stdout

### HTTP layer
- Client: `java.net.http.HttpClient` (HTTP/2 on)
- Timeouts: connect 5s, request 30s; `--timeout` overrides
- Retries: for 429/5xx and IOExceptions; max 3; decorrelated jitter backoff (base 250ms, cap 5s); honor `Retry-After`
- Headers: Idempotency-Key (new ULID per POST unless provided), `User-Agent: qctl/<ver> (<os>/<arch>)`, auth header
- Errors: parse RFC7807 into ProblemDetail

### Auth V1 behavior
- Primary: OIDC Device Flow (stub prints code & polls)
- Token store: OS keychain; fallback file store `~/.config/qctl/tokens.json` (0600)
- Mock override: if `endpoints.api` is `http://localhost:*` or `QCTL_API_KEY` is set → send `X-API-Key`, skip OIDC
- Precedence: `X-API-Key` > OIDC token > none
- `whoami`: token claims (OIDC) or `api-key: set`

### Error → exit mapping
- HTTP ProblemDetails mapping: 401/403 → 4, 404 → 5, 400/422 → 6, 409/412 → 8, 408/429/5xx + IO → 3
- Integrity/signature (local) → 7; CLI validation → 2; unexpected → 1; Ctrl-C → 9

### qqq (templates)
- Handlebars: enable partials & helpers; built-ins: upper, lower, camel, snake, kebab, json, env
- Manifest (template.yaml) minimal fields:

```yaml
id: web-basic
version: 1.2.0
description: Basic web app starter
prompts: []            # optional interactive questions
postGen: [build, run, healthcheck]  # overrides config if present
files: {}              # mapping or glob rules if needed (V1: simple copy)
```

- Local path rules: `--template PATH` supports dir or `.zip`; must contain `template.yaml`
- Post-gen hooks: phases (build/run/healthcheck), each as command array, inherited env; stream output; non-zero fails phase but not generation; print guidance

### qbit (packages)
- Lockfile path: `qbits.lock` (project root)
- Write semantics: atomic via temp + `Files.move(ATOMIC_MOVE)`; include `generatedAt`
- File locking: advisory via `FileChannel.tryLock()`; helpful wait/fail message
- Vendor layout: `vendor/qbits/<group>/<name>`; tarball expanded there
- Resolution:
  - hermetic: only lockfile entries used; fetch exact resolved URLs permitted; no registry search
  - offline: no network ever; must already be in cache or vendor
- Integrity: compute sha512 of downloaded tarball; compare to lockfile integrity
- Mismatch (V1): warn (do not fail)
- Signatures: if present, verify PGP → warn only on failure

### qrun (package/publish)
- Build: Jib Core (programmatic) to build OCI without invoking a Maven goal
- Labels: inject frozen OCI annotations; prevent overwrite in V1
- Registry auth: detect from `~/.docker/config.json` (credHelpers or auths); else anonymous
- Verify-by-digest: after push, HEAD `v2/<name>/manifests/<digest>` (OCI Accept); 200 OK; 401/404 → exit 6 if local metadata says it should exist, else 3 if network issue (heuristics by exception)

### qstudio (offline)
- Ledger: `.qctl/ledger/` in project; filename `${ulid}.json`
- Plan output: `plan.md` at project root unless `--out`
- Globs: PathMatcher + `glob`/`**` patterns; allow/deny applied in order, deny wins

### Cache
- Roots:
  - macOS `~/Library/Caches/qctl/`
  - Linux `$XDG_CACHE_HOME/qctl/` or `~/.cache/qctl/`
  - Windows `%LOCALAPPDATA%\qctl\cache\`
- Subdirs: `templates/`, `qbits/`, `artifacts/`, `trust/keys/`, `http/`
- LRU index: `index.json` (array of {path,size,atime}); lazy recompute if missing
- Prune: `--max-size` with units (KB/MB/GB); evict oldest atime first; print before/after
- Clean `--all`: deletes cache root; confirm unless `--yes`

### Telemetry (opt-in)
- Gate: config or `--telemetry.enabled`
- Storage: `$configDir/telemetry/events.log` (rotate at 5MB, keep 3)
- Write on process exit; failures are silent
- Schema: minimal fields per DESIGN-2.md; no network in V1

### OpenAPI integration
- Client: hand-written minimal DTOs (POJOs in `qctl-shared`) + small `ApiClient` in `qctl-core`
- DTO packages: `io.qrun.qctl.shared.api.*`

### CLI UX details
- Structure: `qctl <group> <verb>` where group matches module
- Help: Picocli usage; `qctl man` generates man pages via codegen plugin
- Completion: picocli-codegen outputs bash/zsh/fish/pwsh scripts; `qctl completion <shell>` prints install instructions

### Testing
- Golden snapshots: `qctl-integration-tests/src/test/resources/golden/`; update via `-Dgolden.update=true`
- Contract tests: start Prism on port 4010; validate schemas via networknt; assert headers like `X-Total-Count`
- Simulated errors: Use an additional WireMock for 401/404/429/503 and timeouts to test retry/backoff and exit codes
- CLI golden: run via `ProcessBuilder`, capture stdout/stderr, compare to snapshots (normalize timestamps/ULIDs)
