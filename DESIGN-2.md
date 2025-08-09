## 1) Design Document (V1 scope)

### V1 Scope (2-week target)

- **Included**:
   - Core CLI, config loader/merger, JSON Schema validation, profiles, env/flag overrides, structured logs
   - qqq: new from local + HTTPS templates (signatures warn-only), YAML-driven gen, post-gen build/run/healthcheck
   - qBit: search, add, list, update, remove with lockfile + integrity hash; signatures warn-only
   - qRun: package (Jib) + publish to registry; minimal verify (hash/image exists); basic status/logs stub
   - qStudio: offline-only planning; indexes code/rules/schema; writes `plan.md` + ledger entry (no provider calls/diff apply)
   - DX/IntelliJ: Toolchains, Spotless/Checkstyle, Qodana, `.run/` configs, unit + Testcontainers tests
   - Binaries: GraalVM native for macOS + Linux; Windows jlink/jpackage zip fallback
- **Deferred (post-V1)**:
   - Sigstore enforcement, plugin system, self-update, Windows signing, full SBOM/provenance, promote/rollback, qStudio diff apply

### Key decisions (resolved)

- **Artifact format**: Default OCI; zip fallback for air-gapped/bare-metal
- **Signatures**: Sigstore/cosign for OCI (future enforce); PGP for templates/qBits initially
- **SemVer**: Honor lockfile first; `preferLatestMinor: true`; strict/hermetic mode available
- **Templates**: Handlebars/Mustache default; Freemarker optional
- **YAML validation**: JSON Schema + bespoke validators for computed/cross-field rules
- **LLM**: Default self-hosted; adapters for OpenAI/Anthropic/Azure; cloud requires explicit opt-in
- **Redaction**: Conservative defaults (`**/*.pem`, `.env*`, `secrets/**`, `.ssh/**`, common token files)
- **Auth**: OAuth Device Flow default; loopback for desktop; service accounts later; OS keychain for tokens
- **Plugins**: Signed-only by default; isolated classloaders; minimal SPI
- **Telemetry**: Opt-in only; minimal fields (anon ID, command, duration, exit, version, os/arch)
- **SBOM**: MVP runtime-only; GA full transitive CycloneDX
- **Cache**: LRU with max size; `qctl cache` commands
- **Self-update**: Weekly check (stable/beta); enterprise disable/mirror (post-V1)

### CLI UX & Commands

 - **Binary**: `qctl` with subcommands: `qqq`, `qbit`, `qrun`, `qstudio`, `auth`, `cache`
 - **Global flags**: `--config`, `--profile`, `--env`, `--verbose`, `--debug`, `--color <auto|always|never>`, `--output <text|json>`, `--yes`, `--dry-run`, `--timeout`, `--log.level`, `--version`, `--help`
 - **Mode flags**: `--offline`, `--hermetic` (see Offline mode). Telemetry opt-in: `--telemetry.enabled` (overrides `telemetry.enabled` in config when provided).
- **Exit codes**:

| Exit Code                                    | Meaning                               | Typical HTTP Status Codes                                                 |
|----------------------------------------------|---------------------------------------|---------------------------------------------------------------------------|
| 0                                            | Success                               | 200, 201, 202, 204                                                        |
| 1                                            | Generic/unclassified error            | Any unexpected non-2xx not mapped below                                   |
| 2                                            | Usage/config error                    | N/A (CLI-level validation)                                                |
| 3                                            | Network/connection error              | 408, 429, 500, 502, 503, 504 (when cause is network/service availability) |
| 4                                            | Authentication/authorization error    | 401, 403                                                                  |
| 5                                            | Resource not found                    | 404                                                                       |
| 6                                            | Validation failed                     | 400, 422                                                                  |
| 7                                            | Integrity/signature verification fail | 409, 412, custom integrity errors                                         |
| 8                                            | Conflict/state error                  | 409                                                                       |
| 9                                            | Cancelled by user                     | N/A (CLI-level signal)                                                    |
- **Completion/man**: `qctl completion <bash|zsh|fish|pwsh>`, `qctl man`

- **Examples**:
   - qqq: `qctl qqq new my-app --template web-basic --non-interactive`
   - qBit: `qctl qbit search auth`, `qctl qbit add io.qbits/auth@^2`
   - qRun: `qctl qrun package`, `qctl qrun publish --env dev`
   - qStudio: `qctl qstudio plan --rules rules.yaml --out plan.md`
   - Auth: `qctl auth login`, `qctl auth whoami`, `qctl auth logout`
   - Cache: `qctl cache ls`, `qctl cache prune`, `qctl cache clean --all`, `qctl cache prune --max-size 500MB`

### Offline mode

- **Flag**: `--offline` forces no network I/O. Commands must use cached data only or fail fast.
- **Failure behavior**: When a command would perform network calls while `--offline` is set, it exits with **code 3 (network)** and prints a human-readable hint. If a real API call executes (when not offline) and the server returns an error, the CLI surfaces the RFC7807 Problem Details payload.
- **Hermetic mode**: `--hermetic` (or `qbit.resolution.hermetic: true`) resolves strictly from the lockfile; network is allowed only for locked URLs (e.g., fetching exact tarballs). `--offline` forbids all network.

### Architecture

- **Modules**: `qctl-core`, `qctl-qqq`, `qctl-qbit`, `qctl-qrun`, `qctl-qstudio`, `qctl-shared`, `qctl-integration-tests`
- **Ports/Adapters (V1)**:
   - Ports: `TemplateProvider`, `QBitRegistry`, `QRunApi`, `LlmProvider` (offline), `AuthProvider`, `InstallerPublisher`, `SignerVerifier` (warn-only)
   - Adapters: HTTP clients (templates/registry/api), filesystem cache, OS keychain, CLI I/O
- **Config system**: `qctl.yaml` loader; JSON Schema validation; errors with JSON Pointer paths; precedence: built-ins < global < project < env (`QCTL_*`) < CLI flags
- **Cache layout** (OS/XDG): `templates/`, `qbits/`, `artifacts/`, `trust/keys/`, `locks/`; per-project `qbits.lock`, `vendor/qbits`
- **Security**: Trust roots scaffold; signature checks warn-only; optional TLS pinning; secrets in OS keychain; strict log redaction
   - V1 signature behavior: when `qbit.verifySignatures` is true, signatures are checked and warnings emitted on issues; enforcement is deferred to post-V1.

### Mode details

- **qqq**: Discover remote/local templates, schema-driven generation, idempotent regeneration with `--force/--merge`, post-gen `build → run → healthcheck`
- **qBit**: Registry search, SemVer resolution, vendored layout, integrity hash
   - **Lockfile v1**:
      - Fields per package entry: `name`, `version`, `resolved` (URL), `integrity` (sha512), `dependencies` (map), `registry` (URL), `vendorPath`, optional `signature`
      - Top-level: `lockfileVersion: 1`, `generatedAt` (RFC3339), `packages` (map keyed by package name)
      - Integrity rules: verify `sha512` of tarball; warn-only in V1 on mismatch; resolve strictly from lockfile when present
      - Dedupe policy: prefer single version per minor when compatible; otherwise keep both and record explicit tree in `dependencies`
      - Example entry:
        ```json
        {
          "lockfileVersion": 1,
          "generatedAt": "2025-01-15T12:00:00Z",
          "packages": {
            "io.qbits/auth": {
              "name": "io.qbits/auth",
              "version": "2.3.1",
              "resolved": "https://registry.qrun.io/io.qbits/auth/2.3.1.tgz",
              "integrity": "sha512-b8c1…9d44",
              "dependencies": { "io.qbits/jwt": "^1.4.0" },
              "registry": "https://registry.qrun.io",
              "vendorPath": "vendor/qbits/io.qbits/auth"
            }
          }
        }
        ```
- **qRun**: Package OCI via Jib, push to registry, verify by digest existence; status/logs endpoints (stub). Note: verify is done by checking the remote registry by digest (e.g., HEAD on the OCI manifest); this remote check is out of scope for the mock server.
- **qStudio**: Offline plan indexer with allow/deny lists; writes `plan.md`; records ledger entry

### OCI annotations for qRun (frozen for V1)

**Required labels (must be present)**

- `org.opencontainers.image.title` — app name
- `org.opencontainers.image.version` — release version
- `org.opencontainers.image.revision` — VCS commit
- `org.opencontainers.image.source` — VCS URL
- `io.qrun.artifact` — ULID of Artifact record

**Optional (nice-to-have)**

- `org.opencontainers.image.description`
- `org.opencontainers.image.url`
- `org.opencontainers.image.created`
- `org.opencontainers.image.authors`
- `io.qrun.sbom`
- `io.qrun.provenance`
- `io.qrun.qqq.manifest`

### Cross-platform packaging

- **macOS/Linux**: GraalVM native images
- **Windows**: jlink/jpackage zip fallback in V1
- **Installers**: Archives in V1; Homebrew/winget/Scoop post-V1

#### Windows baseline & path policy (V1)

- Minimum: Windows 10 22H2 and Windows Server 2019
- Default install dir for zip: `%LOCALAPPDATA%\qctl` (user-scoped)
- Long-path support: recommend enabling `LongPathsEnabled=1` (documented); CLI handles `\\?\` prefixes in edge cases
- Shell completion: PowerShell module installed under user profile; signed scripts (script signing optional in V1; full code signing post-V1)

**Path locations (intentional split):**

- Config: `%APPDATA%\qctl\qctl.yaml`
- Cache/Install (zip default): `%LOCALAPPDATA%\qctl`

### Developer Experience & IntelliJ Standards

- Maven Wrapper + Java Toolchains (latest LTS Temurin); reproducible builds (maven-enforcer)
- Spotless + our custom Java Format; Checkstyle (see `codestyle/checkstyle.config.xml`); Error Prone; Nullness annotations; Qodana in CI
- Tests: JUnit 5, AssertJ, Mockito, Testcontainers; golden CLI tests
- GraalVM reflection configs committed; debug profile
- All classes, packages, and methods must include full flowerbox-style Javadoc comments.

#### Javadoc flowerbox style (canonical format)

Use the following header format for all methods (and analogous style for classes and packages):

```java
/***************************************************************************
 * Parses a human-readable size expression into its byte count.
 *
 * Why: Centralizes unit parsing (B, KB, MB, GB) and validation for cache pruning.
 *
 * @param sizeExpression human-readable size (e.g., "500MB", "1.5 GB", "1024B")
 * @return the size in bytes (non-negative)
 * @throws IllegalArgumentException if the expression is empty or invalid
 * @since 0.1.0
 ***************************************************************************/
```

### Configuration & Default qctl.yaml Schema

- Note: `qrun.release.strategy` and `qrun.release.rollback` are reserved for future; ignored by V1.
- **Precedence (lowest → highest)**: Built-in defaults < Global config < Project config < Environment (`QCTL_*`) < CLI flags
- **Default locations**:
   - macOS: `~/Library/Application Support/qctl/qctl.yaml`
   - Linux: `$XDG_CONFIG_HOME/qctl/qctl.yaml` (fallback `~/.config/qctl/qctl.yaml`)
   - Windows: `%APPDATA%\qctl\qctl.yaml`
   - Cache dirs follow OS/XDG
- **Embedded JSON Schema**:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "qctl config",
  "type": "object",
  "properties": {
    "output": { "enum": ["text", "json"] },
    "endpoints": {
      "type": "object",
      "properties": {
        "templates": {"type": "string", "format": "uri"},
        "registry":  {"type": "string", "format": "uri"},
        "api":       {"type": "string", "format": "uri"},
        "studio":    {"type": "string", "format": "uri"},
        "cdn":       {"type": "string", "format": "uri"}
      },
      "additionalProperties": false
    },
    "auth": {
      "type": "object",
      "properties": {
        "provider": {"enum": ["oidc"]},
        "issuer": {"type": "string", "format": "uri"},
        "clientId": {"type": "string"},
        "audience": {"type": "string"},
        "scopes": {"type": "array", "items": {"type": "string"}},
        "tokenStore": {"enum": ["os-keychain", "file"]},
        "profile": {"type": "string"}
      },
      "additionalProperties": false
    },
    "profiles": {"type": "object", "additionalProperties": {"$ref": "#"}},
    "qrun": {
      "type": "object",
      "properties": {
        "defaultEnv": {"enum": ["dev","stage","qa","prod"]},
        "packageFormat": {"enum": ["oci","zip"]},
        "release": {
          "type": "object",
          "properties": {
            "channel": {"enum": ["stable","beta"]},
            "strategy": {"enum": ["blue-green","rolling"]},
            "rollback": {"type": "boolean"}
          },
          "additionalProperties": false
        },
        "secrets": {
          "type": "object",
          "properties": {
            "provider": {"enum": ["os-keychain","env","file"]},
            "paths": {"type": "array", "items": {"type": "string"}}
          },
          "additionalProperties": false
        }
      },
      "additionalProperties": false
    },
    "qbit": {
      "type": "object",
      "properties": {
        "cacheDir": {"type": "string"},
        "vendorDir": {"type": "string"},
        "lockfile": {"type": "string"},
        "resolution": {
          "type": "object",
          "properties": {
            "preferLatestMinor": {"type": "boolean"},
            "dedupe": {"type": "boolean"}
          },
          "additionalProperties": false
        },
        "verifySignatures": {"type": "boolean"}
      },
      "additionalProperties": false
    },
    "qqq": {
      "type": "object",
      "properties": {
        "defaultTemplate": {"type": "string"},
        "postGen": {"type": "array", "items": {"type": "string"}},
        "generator": {
          "type": "object",
          "properties": {
            "interactive": {"type": "boolean"},
            "validate": {"enum": ["strict","lenient"]}
          },
          "additionalProperties": false
        }
      },
      "additionalProperties": false
    },
    "qstudio": {
      "type": "object",
      "properties": {
        "provider": {"enum": ["openai","anthropic","azure","selfhosted"]},
        "model": {"type": "string"},
        "policy": {
          "type": "object",
          "properties": {
            "redactPaths": {"type": "array", "items": {"type": "string"}},
            "allowPaths": {"type": "array", "items": {"type": "string"}},
            "maxTokens": {"type": "integer", "minimum": 1024}
          },
          "additionalProperties": false
        },
        "confirmWrites": {"type": "boolean"}
      },
      "additionalProperties": false
    },
    "telemetry": {
      "type": "object",
      "properties": {
        "enabled": {"type": "boolean"}
      },
      "additionalProperties": false
    },
    "updates": {
      "type": "object",
      "properties": {
        "autoCheck": {"type": "boolean"},
        "channel": {"enum": ["stable","beta"]}
      },
      "additionalProperties": false
    },
    "log": {
      "type": "object",
      "properties": {
        "level": {"enum": ["info","debug","trace"]},
        "format": {"enum": ["text","json"]},
        "color": {"enum": ["auto","always","never"]}
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
}
```

- **Sample `qctl.yaml`**:

```yaml
endpoints:
  templates: https://templates.qrun.io
  registry:  https://registry.qrun.io
  api:       https://api.qrun.io
  studio:    https://studio.qrun.io
  cdn:       https://cdn.qrun.io

auth:
  provider: oidc
  issuer: https://auth.qrun.io/
  clientId: qctl-cli
  audience: qrun
  scopes: [ "openid", "profile", "offline_access" ]
  tokenStore: os-keychain
  profile: default

qrun:
  defaultEnv: dev
  packageFormat: oci
  release:
    channel: stable
    strategy: rolling
    rollback: true
  secrets:
    provider: os-keychain

qbit:
  vendorDir: vendor/qbits
  lockfile: qbits.lock
  resolution:
    preferLatestMinor: true
    dedupe: true
  verifySignatures: true # V1: checks and warns on signature issues (does not fail); enforcement post-V1

qqq:
  defaultTemplate: web-basic
  postGen: [ "build", "run", "healthcheck" ]
  generator:
    interactive: true
    validate: strict

qstudio:
  provider: selfhosted
  model: "enterprise-default"
  policy:
    redactPaths: [ "**/*.pem", "**/secrets/**" ]
    allowPaths:  [ "src/**", "rules/**", "schema/**" ]
    maxTokens: 120000
  confirmWrites: true

telemetry:
  enabled: false

updates:
  autoCheck: true
  channel: stable

log:
  level: info
  format: text
  color: auto

profiles:
  stage:
    endpoints:
      api: https://api.stage.qrun.io
    qrun:
      defaultEnv: stage
  prod:
    endpoints:
      api: https://api.qrun.io
    qrun:
      defaultEnv: prod
```

## 2) Backend Model & API Stub (OpenAPI 3.1 with fixtures)

### Domain model (POJOs)

- Templates: `Template {id, name, description, latestVersion, createdAt}`; `TemplateVersion {templateId, version, manifestUrl, signatureUrl, sha256, createdAt}`
- qBits: `QBit {id, name, description, latestVersion, publisher, createdAt}`; `QBitVersion {qbitId, version, tarballUrl, signatureUrl, sha512, dependencies[], createdAt}`
- Artifacts/Releases: `Artifact {id, kind:oci|zip, digest, sizeBytes, sbomUrl?, createdAt}`; `Release {id, appName, version, artifactId, channel:stable|beta, provenanceUrl?, createdAt}`
- Environments/Deployments: `Environment {name:dev|stage|qa|prod, configOverrides{}, createdAt}`; `Deployment {id, appName, env, releaseId, status:pending|active|failed|rolled_back, createdAt, updatedAt}`
- LLM Planning: `PlanRequest {projectId?, allowPaths[], denyPaths[], request}`; `Plan {id, summary, steps[], costEstimate?, tokenEstimate?, createdAt}`; `LedgerEntry {id, planId, provider, promptHash, outputHash, artifacts[]?, timestamp}`
- Common: `ProblemDetail {type, title, status, detail, instance, errors?[]}` (RFC 7807)

### Common API rules (clients and mock)

- **Idempotency**: All POSTs accept `Idempotency-Key` (header). Retries with the same key must be 201 (first) or 200 (subsequent) with the same body.
- **Timeouts**: Default client timeout 30s; connect 5s; write 30s; read 30s.
- **Retries**: Exponential backoff with jitter for 429/5xx (up to 3 attempts); respect `Retry-After` when present.
- **Errors**: RFC7807 Problem Details; include `type`, `title`, `status`, `detail`, optional `errors[]`.
- **Auth**: Real endpoints use OIDC (device flow). `X-API-Key` is accepted only for mocks/local testing.

### OpenAPI 3.1 YAML (with fixed fixtures and schema endpoints)

```yaml
openapi: 3.1.0
info:
  title: qRun API (V1 mock)
  version: 0.1.0
  description: |
    Mockable API for qctl V1 contract testing.
    Uses fixed example dataset (ULIDs and timestamps) for golden tests.
servers:
  - url: https://api.qrun.io
  - url: http://localhost:4010
    description: Prism mock default
security:
  - openId: []
  - apiKey: []
components:
  securitySchemes:
    openId:
      type: openIdConnect
      openIdConnectUrl: https://auth.qrun.io/.well-known/openid-configuration
    apiKey:
      type: apiKey
      in: header
      name: X-API-Key
  headers:
    X-Total-Count:
      description: Total number of items available for pagination
      schema: { type: integer, minimum: 0 }
  parameters:
    page:
      name: page
      in: query
      schema: { type: integer, minimum: 0, default: 0 }
    size:
      name: size
      in: query
      schema: { type: integer, minimum: 1, maximum: 200, default: 20 }
    query:
      name: query
      in: query
      schema: { type: string }
    env:
      name: env
      in: query
      schema: { type: string, enum: [dev, stage, qa, prod] }
    follow:
      name: follow
      in: query
      schema: { type: boolean, default: false }
    IdempotencyKey:
      name: Idempotency-Key
      in: header
      required: false
      description: Idempotency key for safely retrying POST requests
      schema: { type: string, maxLength: 256 }
  responses:
    Unauthorized401:
      description: Unauthorized
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/ProblemDetail' }
          examples: { unauth: { $ref: '#/components/examples/exProblem401' } }
    NotFound404:
      description: Not Found
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/ProblemDetail' }
          examples: { notFound: { $ref: '#/components/examples/exProblem404' } }
    TooManyRequests429:
      description: Too Many Requests
      headers:
        Retry-After:
          description: Seconds to wait before retrying
          schema: { type: integer, minimum: 0 }
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/ProblemDetail' }
    ServerError503:
      description: Service Unavailable
      content:
        application/problem+json:
          schema: { $ref: '#/components/schemas/ProblemDetail' }
  schemas:
    ULID:
      type: string
      pattern: '^[0-9A-HJKMNP-TV-Z]{26}$'
    RFC3339:
      type: string
      format: date-time
    Template:
      type: object
      properties:
        id: { $ref: '#/components/schemas/ULID' }
        name: { type: string }
        description: { type: string }
        latestVersion: { type: string }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
      required: [id, name, latestVersion, createdAt]
      additionalProperties: false
    TemplateVersion:
      type: object
      properties:
        templateId: { $ref: '#/components/schemas/ULID' }
        version: { type: string }
        manifestUrl: { type: string, format: uri }
        signatureUrl: { type: string, format: uri }
        sha256: { type: string }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
      required: [templateId, version, manifestUrl, sha256]
      additionalProperties: false
    QBit:
      type: object
      properties:
        id: { $ref: '#/components/schemas/ULID' }
        name: { type: string }
        description: { type: string }
        latestVersion: { type: string }
        publisher: { type: string }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
      required: [id, name, latestVersion, publisher, createdAt]
      additionalProperties: false
    QBitVersion:
      type: object
      properties:
        qbitId: { $ref: '#/components/schemas/ULID' }
        version: { type: string }
        tarballUrl: { type: string, format: uri }
        signatureUrl: { type: string, format: uri }
        sha512: { type: string }
        dependencies:
          type: array
          items: { type: string }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
      required: [qbitId, version, tarballUrl, sha512]
      additionalProperties: false
    Artifact:
      type: object
      properties:
        id: { $ref: '#/components/schemas/ULID' }
        kind: { type: string, enum: [oci, zip] }
        digest: { type: string }
        sizeBytes: { type: integer, minimum: 0 }
        sbomUrl: { type: string, format: uri }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
      required: [id, kind, digest, sizeBytes, createdAt]
      additionalProperties: false
    Release:
      type: object
      properties:
        id: { $ref: '#/components/schemas/ULID' }
        appName: { type: string }
        version: { type: string }
        artifactId: { $ref: '#/components/schemas/ULID' }
        channel: { type: string, enum: [stable, beta] }
        provenanceUrl: { type: string, format: uri }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
      required: [id, appName, version, artifactId, channel]
      additionalProperties: false
    Environment:
      type: object
      properties:
        name: { type: string, enum: [dev, stage, qa, prod] }
        configOverrides: { type: object, additionalProperties: true }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
      required: [name]
      additionalProperties: false
    Deployment:
      type: object
      properties:
        id: { $ref: '#/components/schemas/ULID' }
        appName: { type: string }
        env: { type: string, enum: [dev, stage, qa, prod] }
        releaseId: { $ref: '#/components/schemas/ULID' }
        status: { type: string, enum: [pending, active, failed, rolled_back] }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
        updatedAt: { $ref: '#/components/schemas/RFC3339' }
      required: [id, appName, env, releaseId, status]
      additionalProperties: false
    PlanRequest:
      type: object
      properties:
        projectId: { type: string }
        allowPaths: { type: array, items: { type: string } }
        denyPaths: { type: array, items: { type: string } }
        request: { type: string }
      required: [request]
      additionalProperties: false
    Plan:
      type: object
      properties:
        id: { $ref: '#/components/schemas/ULID' }
        summary: { type: string }
        steps: { type: array, items: { type: string } }
        costEstimate: { type: number }
        tokenEstimate: { type: integer, minimum: 0 }
        createdAt: { $ref: '#/components/schemas/RFC3339' }
      required: [id, summary, steps, createdAt]
      additionalProperties: false
    LedgerEntry:
      type: object
      properties:
        id: { $ref: '#/components/schemas/ULID' }
        planId: { $ref: '#/components/schemas/ULID' }
        provider: { type: string }
        promptHash: { type: string }
        outputHash: { type: string }
        artifacts: { type: array, items: { type: string } }
        timestamp: { $ref: '#/components/schemas/RFC3339' }
      required: [id, planId, provider, promptHash, outputHash, timestamp]
      additionalProperties: false
    ProblemDetail:
      type: object
      properties:
        type: { type: string, format: uri }
        title: { type: string }
        status: { type: integer }
        detail: { type: string }
        instance: { type: string, format: uri }
        errors: { type: array, items: { type: string } }
      additionalProperties: true
  examples:
    exTemplate:
      value:
        id: 01J0M3K6K7Q8Z6ZDX8Q5A2F7KM
        name: web-basic
        description: Basic web app starter
        latestVersion: '1.2.0'
        createdAt: '2025-01-15T12:00:00Z'
    exTemplateVersions:
      value:
        - templateId: 01J0M3K6K7Q8Z6ZDX8Q5A2F7KM
          version: '1.2.0'
          manifestUrl: https://templates.qrun.io/web-basic/1.2.0/manifest.yaml
          signatureUrl: https://templates.qrun.io/web-basic/1.2.0/manifest.sig
          sha256: '6e3b…a9f1'
          createdAt: '2025-01-15T12:00:00Z'
        - templateId: 01J0M3K6K7Q8Z6ZDX8Q5A2F7KM
          version: '1.1.0'
          manifestUrl: https://templates.qrun.io/web-basic/1.1.0/manifest.yaml
          signatureUrl: https://templates.qrun.io/web-basic/1.1.0/manifest.sig
          sha256: '4a12…be77'
          createdAt: '2025-01-15T12:00:00Z'
    exQBit:
      value:
        id: 01J0M3QF5V2J0F1TBKQ1S6F2ZP
        name: io.qbits/auth
        description: Authentication & RBAC
        latestVersion: '2.3.1'
        publisher: qrun.io
        createdAt: '2025-01-15T12:00:00Z'
    exQBitVersion:
      value:
        qbitId: 01J0M3QF5V2J0F1TBKQ1S6F2ZP
        version: '2.3.1'
        tarballUrl: https://registry.qrun.io/io.qbits/auth/2.3.1.tgz
        signatureUrl: https://registry.qrun.io/io.qbits/auth/2.3.1.tgz.sig
        sha512: 'b8c1…9d44'
        dependencies: ['io.qbits/jwt@^1.4.0']
        createdAt: '2025-01-15T12:00:00Z'
    exArtifact:
      value:
        id: 01J0M40G3SJ0QJ9E3V1QK8A3R2
        kind: oci
        digest: sha256:9f2c…1d0b
        sizeBytes: 73400320
        sbomUrl: https://cdn.qrun.io/artifacts/01J0M40G…/sbom.cdx.json
        createdAt: '2025-01-15T12:00:00Z'
    exRelease:
      value:
        id: 01J0M41V9X5J2B4M5H0G7D2T1Q
        appName: demo-app
        version: '0.1.0'
        artifactId: 01J0M40G3SJ0QJ9E3V1QK8A3R2
        channel: stable
        provenanceUrl: https://cdn.qrun.io/releases/01J0M41V…/provenance.json
        createdAt: '2025-01-15T12:00:00Z'
    exDeployment:
      value:
        id: 01J0M43J8E4S7N9C0L2X5V6B3M
        appName: demo-app
        env: dev
        releaseId: 01J0M41V9X5J2B4M5H0G7D2T1Q
        status: active
        createdAt: '2025-01-15T12:00:00Z'
        updatedAt: '2025-01-15T12:05:00Z'
    exPlan:
      value:
        id: 01J0M45Q2K6L8P0S3N4R7U8W9X
        summary: Add RBAC to admin panel
        steps: ["Analyze roles", "Add JWT middleware", "Generate tests"]
        tokenEstimate: 12000
        createdAt: '2025-01-15T12:00:00Z'
    exLedgerEntry:
      value:
        id: 01J0M46Z4B7C9D1E2F3G4H5J6K
        planId: 01J0M45Q2K6L8P0S3N4R7U8W9X
        provider: selfhosted
        promptHash: ph_abc123
        outputHash: oh_def456
        timestamp: '2025-01-15T12:01:00Z'
    exAppStatus:
      value:
        status: healthy
        version: '0.1.0'
        updatedAt: '2025-01-15T12:05:00Z'
    exAppLogs:
      summary: demo-app logs (dev)
      value: |
        2025-01-15T12:04:57Z demo-app[web]: started
        2025-01-15T12:04:58Z demo-app[web]: listening on :8080
        2025-01-15T12:05:00Z demo-app[web]: health=ok
    exProblem400:
      value:
        type: https://qrun.io/problems/validation-error
        title: Invalid request
        status: 400
        detail: missing or invalid fields
    exProblem401:
      value:
        type: https://qrun.io/problems/unauthorized
        title: Unauthorized
        status: 401
        detail: missing or invalid credentials
    exProblem404:
      value:
        type: https://qrun.io/problems/not-found
        title: Not Found
        status: 404
        detail: resource not found
paths:
  /v1/templates:
    get:
      summary: List templates
      parameters: [ { $ref: '#/components/parameters/query' }, { $ref: '#/components/parameters/page' }, { $ref: '#/components/parameters/size' } ]
      responses:
        '200':
          description: OK
          headers:
            X-Total-Count: { $ref: '#/components/headers/X-Total-Count' }
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/Template' }
              examples:
                fixed:
                  value:
                    - $ref: '#/components/examples/exTemplate/value'
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/templates/{templateId}:
    get:
      summary: Get template
      parameters:
        - name: templateId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Template' }
              examples: { fixed: { $ref: '#/components/examples/exTemplate' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/templates/{templateId}/versions:
    get:
      summary: List template versions
      parameters:
        - name: templateId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/TemplateVersion' }
              examples: { fixed: { $ref: '#/components/examples/exTemplateVersions' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
  /v1/templates/{templateId}/versions/{version}:
    get:
      summary: Get a template version
      parameters:
        - name: templateId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
        - name: version
          in: path
          required: true
          schema: { type: string }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/TemplateVersion' }
              examples:
                latest:
                  value: { $ref: '#/components/examples/exTemplateVersions/value/0' }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
  /v1/qbits:
    get:
      summary: List qBits
      parameters: [ { $ref: '#/components/parameters/query' }, { $ref: '#/components/parameters/page' }, { $ref: '#/components/parameters/size' } ]
      responses:
        '200':
          description: OK
          headers:
            X-Total-Count: { $ref: '#/components/headers/X-Total-Count' }
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/QBit' }
              examples:
                fixed:
                  value:
                    - $ref: '#/components/examples/exQBit/value'
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/qbits/{qbitId}:
    get:
      summary: Get qBit
      parameters:
        - name: qbitId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/QBit' }
              examples: { fixed: { $ref: '#/components/examples/exQBit' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/qbits/{qbitId}/versions:
    get:
      summary: List qBit versions
      parameters:
        - name: qbitId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                type: array
                items: { $ref: '#/components/schemas/QBitVersion' }
              examples: { fixed: { value: [ { $ref: '#/components/examples/exQBitVersion/value' } ] } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
  /v1/qbits/{qbitId}/versions/{version}:
    get:
      summary: Get a qBit version
      parameters:
        - name: qbitId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
        - name: version
          in: path
          required: true
          schema: { type: string }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/QBitVersion' }
              examples: { fixed: { $ref: '#/components/examples/exQBitVersion' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
  /v1/artifacts:
    post:
      summary: Create artifact record (JSON manifest or upload-URL flow)
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/Artifact' }
            examples: { fixed: { $ref: '#/components/examples/exArtifact' } }
      parameters:
        - $ref: '#/components/parameters/IdempotencyKey'
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Artifact' }
              examples: { fixed: { $ref: '#/components/examples/exArtifact' } }
        '200':
          description: OK (idempotent replay)
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Artifact' }
              examples: { fixed: { $ref: '#/components/examples/exArtifact' } }
        '400':
          description: Bad Request
          content:
            application/problem+json:
              schema: { $ref: '#/components/schemas/ProblemDetail' }
              examples: { bad: { $ref: '#/components/examples/exProblem400' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/artifacts/{artifactId}:
    get:
      summary: Get artifact
      parameters:
        - name: artifactId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Artifact' }
              examples: { fixed: { $ref: '#/components/examples/exArtifact' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
  /v1/releases:
    post:
      summary: Create release
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/Release' }
            examples: { fixed: { $ref: '#/components/examples/exRelease' } }
      parameters:
        - $ref: '#/components/parameters/IdempotencyKey'
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Release' }
              examples: { fixed: { $ref: '#/components/examples/exRelease' } }
        '200':
          description: OK (idempotent replay)
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Release' }
              examples: { fixed: { $ref: '#/components/examples/exRelease' } }
        '400':
          description: Bad Request
          content:
            application/problem+json:
              schema: { $ref: '#/components/schemas/ProblemDetail' }
              examples: { bad: { $ref: '#/components/examples/exProblem400' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/releases/{releaseId}:
    get:
      summary: Get release
      parameters:
        - name: releaseId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Release' }
              examples: { fixed: { $ref: '#/components/examples/exRelease' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
  /v1/deployments:
    post:
      summary: Create deployment
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/Deployment' }
            examples: { fixed: { $ref: '#/components/examples/exDeployment' } }
      parameters:
        - $ref: '#/components/parameters/IdempotencyKey'
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Deployment' }
              examples: { fixed: { $ref: '#/components/examples/exDeployment' } }
        '200':
          description: OK (idempotent replay)
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Deployment' }
              examples: { fixed: { $ref: '#/components/examples/exDeployment' } }
        '400':
          description: Bad Request
          content:
            application/problem+json:
              schema: { $ref: '#/components/schemas/ProblemDetail' }
              examples: { bad: { $ref: '#/components/examples/exProblem400' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }

  /schemas/lockfile-v1.json:
    get:
      summary: Get Lockfile v1 JSON Schema
      responses:
        '200':
          description: OK
          content:
            application/schema+json:
              schema: { type: object }
              examples:
                external:
                  externalValue: https://qrun.io/schemas/Lockfile-v1.schema.json

  /schemas/update-manifest-v1.json:
    get:
      summary: Get Update Manifest v1 JSON Schema
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { type: object }
              examples:
                schema:
                  value:
                    $id: https://qrun.io/schemas/UpdateManifest-v1.schema.json
                    $schema: https://json-schema.org/draft/2020-12/schema
                    type: object
                    properties:
                      version: { type: string }
                      channel: { type: string, enum: [stable, beta] }
                      platforms:
                        type: array
                        items:
                          type: object
                          properties:
                            os: { type: string, enum: [macOS, Linux, Windows] }
                            arch: { type: string, enum: [x64, arm64] }
                            format: { type: string, enum: [native, jlink, zip] }
                            url: { type: string, format: uri }
                            sha256: { type: string }
                            signatureUrl: { type: string, format: uri }
                          required: [os, arch, format, url, sha256]
                      releaseNotesUrl: { type: string, format: uri }
                      publishedAt: { type: string, format: date-time }
                    required: [version, channel, platforms]

  /schemas/qctl-config-v1.json:
    get:
      summary: Get qctl.yaml config JSON Schema
      responses:
        '200':
          description: OK
          content:
            application/schema+json:
              schema:
                type: object
              examples:
                external:
                  externalValue: https://qrun.io/schemas/qctl-config-v1.json
  /v1/deployments/{deploymentId}:
    get:
      summary: Get deployment
      parameters:
        - name: deploymentId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Deployment' }
              examples: { fixed: { $ref: '#/components/examples/exDeployment' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
  /v1/apps/{appName}/deployments:
    get:
      summary: List app deployments (latest first)
      parameters:
        - name: appName
          in: path
          required: true
          schema: { type: string }
        - $ref: '#/components/parameters/env'
        - $ref: '#/components/parameters/page'
        - $ref: '#/components/parameters/size'
      responses:
        '200':
          description: OK
          headers:
            X-Total-Count: { $ref: '#/components/headers/X-Total-Count' }
          content:
            application/json:
              schema: { type: array, items: { $ref: '#/components/schemas/Deployment' } }
              examples: { fixed: { value: [ { $ref: '#/components/examples/exDeployment/value' } ] } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/apps/{appName}/status:
    get:
      summary: Get app status
      parameters:
        - name: appName
          in: path
          required: true
          schema: { type: string }
        - $ref: '#/components/parameters/env'
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema:
                type: object
                properties:
                  status: { type: string }
                  version: { type: string }
                  updatedAt: { $ref: '#/components/schemas/RFC3339' }
              examples: { fixed: { $ref: '#/components/examples/exAppStatus' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/apps/{appName}/logs:
    get:
      summary: Get app logs (stub)
      description: In the V1 mock server, `follow=true` is ignored. Logs are returned in a single static response with no streaming or SSE support.
      parameters:
        - name: appName
          in: path
          required: true
          schema: { type: string }
        - $ref: '#/components/parameters/env'
        - $ref: '#/components/parameters/follow'
      responses:
        '200':
          description: OK
          content:
            text/plain:
              schema: { type: string }
              examples: { fixed: { $ref: '#/components/examples/exAppLogs' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/qstudio/plan:
    post:
      summary: Create plan (offline V1)
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: '#/components/schemas/PlanRequest' }
            examples: { sample: { value: { request: "Add RBAC to admin panel" } } }
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Plan' }
              examples: { fixed: { $ref: '#/components/examples/exPlan' } }
        '200':
          description: OK (idempotent replay)
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Plan' }
              examples: { fixed: { $ref: '#/components/examples/exPlan' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '429': { $ref: '#/components/responses/TooManyRequests429' }
        '503': { $ref: '#/components/responses/ServerError503' }
  /v1/qstudio/plans/{planId}:
    get:
      summary: Get plan
      parameters:
        - name: planId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { $ref: '#/components/schemas/Plan' }
              examples: { fixed: { $ref: '#/components/examples/exPlan' } }
        '401': { $ref: '#/components/responses/Unauthorized401' }
        '404': { $ref: '#/components/responses/NotFound404' }
  /v1/qstudio/plans/{planId}/ledger:
    get:
      summary: Get plan ledger entries
      parameters:
        - name: planId
          in: path
          required: true
          schema: { $ref: '#/components/schemas/ULID' }
      responses:
        '200':
          description: OK
          content:
            application/json:
              schema: { type: array, items: { $ref: '#/components/schemas/LedgerEntry' } }
              examples: { fixed: { value: [ { $ref: '#/components/examples/exLedgerEntry/value' } ] } }
```

### Mock plan

- Use Prism to mock directly from the spec:
   - Install: `npm i -g @stoplight/prism-cli`
   - Run (static, deterministic examples): `prism mock --dynamic false DESIGN-2.md#openapi` (or extract the YAML to `openapi.yaml` and run `prism mock --dynamic false openapi.yaml`)
- Or generate a Spring server stub:
   - `openapi-generator generate -i openapi.yaml -g spring -o mock-server`
   - Run the stub and serve fixed examples by default
- Or a minimal Java service skeleton with Swagger UI using the same spec
- Point CLI tests to the mock via `--endpoint.api http://localhost:4010`

### Contract testing

- Provide golden CLI tests against the mock with fixed dataset
- Validate responses conform to schemas; assert headers (e.g., `X-Total-Count`) on list endpoints

### JSON Schemas (contract artifacts)

```json
// TemplateVersion.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/TemplateVersion.schema.json",
  "type": "object",
  "properties": {
    "templateId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "version": { "type": "string" },
    "manifestUrl": { "type": "string", "format": "uri" },
    "signatureUrl": { "type": "string", "format": "uri" },
    "sha256": { "type": "string" },
    "createdAt": { "type": "string", "format": "date-time" }
  },
  "required": ["templateId", "version", "manifestUrl", "sha256"],
  "additionalProperties": false
}
```

```json
// QBitVersion.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/QBitVersion.schema.json",
  "type": "object",
  "properties": {
    "qbitId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "version": { "type": "string" },
    "tarballUrl": { "type": "string", "format": "uri" },
    "signatureUrl": { "type": "string", "format": "uri" },
    "sha512": { "type": "string" },
    "dependencies": { "type": "array", "items": { "type": "string" } },
    "createdAt": { "type": "string", "format": "date-time" }
  },
  "required": ["qbitId", "version", "tarballUrl", "sha512"],
  "additionalProperties": false
}
```

```json
// Artifact.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/Artifact.schema.json",
  "type": "object",
  "properties": {
    "id": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "kind": { "type": "string", "enum": ["oci", "zip"] },
    "digest": { "type": "string" },
    "sizeBytes": { "type": "integer", "minimum": 0 },
    "sbomUrl": { "type": "string", "format": "uri" },
    "createdAt": { "type": "string", "format": "date-time" }
  },
  "required": ["id", "kind", "digest", "sizeBytes", "createdAt"],
  "additionalProperties": false
}
```

```json
// Release.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/Release.schema.json",
  "type": "object",
  "properties": {
    "id": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "appName": { "type": "string" },
    "version": { "type": "string" },
    "artifactId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "channel": { "type": "string", "enum": ["stable", "beta"] },
    "provenanceUrl": { "type": "string", "format": "uri" },
    "createdAt": { "type": "string", "format": "date-time" }
  },
  "required": ["id", "appName", "version", "artifactId", "channel"],
  "additionalProperties": false
}
```

```json
// Deployment.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/Deployment.schema.json",
  "type": "object",
  "properties": {
    "id": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "appName": { "type": "string" },
    "env": { "type": "string", "enum": ["dev", "stage", "qa", "prod"] },
    "releaseId": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "status": { "type": "string", "enum": ["pending", "active", "failed", "rolled_back"] },
    "createdAt": { "type": "string", "format": "date-time" },
    "updatedAt": { "type": "string", "format": "date-time" }
  },
  "required": ["id", "appName", "env", "releaseId", "status"],
  "additionalProperties": false
}
```

```json
// Plan.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/Plan.schema.json",
  "type": "object",
  "properties": {
    "id": { "type": "string", "pattern": "^[0-9A-HJKMNP-TV-Z]{26}$" },
    "summary": { "type": "string" },
    "steps": { "type": "array", "items": { "type": "string" } },
    "costEstimate": { "type": "number" },
    "tokenEstimate": { "type": "integer", "minimum": 0 },
    "createdAt": { "type": "string", "format": "date-time" }
  },
  "required": ["id", "summary", "steps", "createdAt"],
  "additionalProperties": false
}
```

```json
// ProblemDetail.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/ProblemDetail.schema.json",
  "type": "object",
  "properties": {
    "type": { "type": "string", "format": "uri" },
    "title": { "type": "string" },
    "status": { "type": "integer" },
    "detail": { "type": "string" },
    "instance": { "type": "string", "format": "uri" },
    "errors": { "type": "array", "items": { "type": "string" } }
  },
  "additionalProperties": true
}
```

```json
// Lockfile-v1.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/Lockfile-v1.schema.json",
  "type": "object",
  "properties": {
    "lockfileVersion": { "const": 1 },
    "generatedAt": { "type": "string", "format": "date-time" },
    "packages": {
      "type": "object",
      "additionalProperties": {
        "type": "object",
        "properties": {
          "name": { "type": "string" },
          "version": { "type": "string" },
          "resolved": { "type": "string", "format": "uri" },
          "integrity": { "type": "string" },
          "dependencies": { "type": "object", "additionalProperties": { "type": "string" } },
          "registry": { "type": "string", "format": "uri" },
          "vendorPath": { "type": "string" },
          "signature": { "type": "string" }
        },
        "required": ["version", "resolved", "integrity"]
      }
    }
  },
  "required": ["lockfileVersion", "packages"],
  "additionalProperties": false
}
```

```json
// UpdateManifest-v1.schema.json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://qrun.io/schemas/UpdateManifest-v1.schema.json",
  "type": "object",
  "properties": {
    "version": { "type": "string" },
    "channel": { "type": "string", "enum": ["stable", "beta"] },
    "platforms": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "os": { "type": "string", "enum": ["macOS", "Linux", "Windows"] },
          "arch": { "type": "string", "enum": ["x64", "arm64"] },
          "format": { "type": "string", "enum": ["native", "jlink", "zip"] },
          "url": { "type": "string", "format": "uri" },
          "sha256": { "type": "string" },
          "signatureUrl": { "type": "string", "format": "uri" }
        },
        "required": ["os", "arch", "format", "url", "sha256"]
      }
    },
    "releaseNotesUrl": { "type": "string", "format": "uri" },
    "publishedAt": { "type": "string", "format": "date-time" }
  },
  "required": ["version", "channel", "platforms"],
  "additionalProperties": false
}
```

### Telemetry stub (event shape)

- **Opt-in flag**: `telemetry.enabled: true` or `--telemetry.enabled` (flag overrides config)
- Docs: Run `qctl telemetry explain` to see the exact fields and retention policy.
- **Event (JSON)**:

```json
{
  "install_id": "a1b2c3d4",
  "command": "qrun publish",
  "duration_ms": 1234,
  "exit_code": 0,
  "version": "0.1.0",
  "os": "macOS",
  "arch": "arm64",
  "timestamp": "2025-01-15T12:34:56Z"
}
```

## 3) Build Checklist (V1)

1. **Bootstrap** — Maven reactor, `.editorconfig`, Spotless/Checkstyle, CI skeleton
2. **Core CLI/Config** — global flags, config loader, schema validation, logging
3. **Auth Stub** — device flow against mock issuer; OS keychain storage
4. **qqq** — local+remote template load, Handlebars render, post-gen
5. **qBit** — search/add/update/remove/list; lockfile v1; integrity hash
6. **qRun** — package OCI (Jib), push to mock registry, verify hash
7. **qStudio** — offline plan, ledger entry
8. **Completion/Man** — generators wired
9. **Telemetry Stub** — opt-in, minimal fields
10. **Security** — warn-only signatures, trust store scaffold
11. **API Mock** — publish OpenAPI with fixtures; serve Swagger UI
12. **Tests** — unit + CLI golden + contract (mock)
13. **Native Builds** — GraalVM macOS/Linux; Windows jlink zip
