## 1) Design Document

### Key decisions (from product)
- **Artifact format default**: Default OCI; keep zip fallback for air-gapped/bare-metal and early dev loops.
- **Registry signatures**: Sigstore/cosign for OCI artifacts; PGP for templates/qBits initially with migration path to Sigstore.
- **SemVer resolution**: Honor lockfile first. Default `preferLatestMinor: true` (caret floats within minor). Strict/hermetic mode pins exact versions and fails on drift.
- **Template engine**: Default Handlebars/Mustache (deterministic, simple). Optional Freemarker behind a feature flag for complex logic.
- **YAML validation**: JSON Schema for validation; bespoke validators only for computed/derived or cross-field constraints.
- **LLM providers**: Default self-hosted (no network by default). Pluggable adapters for OpenAI/Anthropic/Azure. Explicit opt-in and API key required for cloud use. Configurable token/rate caps.
- **Local redaction**: Conservative defaults: `**/*.pem`, `**/*.key`, `**/.ssh/**`, `**/.env*`, `**/secrets/**`, and common token files (npmrc, piprc, gradle.properties). Redact before prompt assembly or telemetry.
- **Auth flows**: Default OAuth Device Flow; loopback browser flow when available. Service accounts later. Tokens stored in OS keychain by default.
- **Plugins**: Require signed plugins by trusted keys (unsigned blocked unless explicit override). Isolated classloaders, minimal SPI, provenance ledger.
- **Windows shell**: Provide a PowerShell completion module; publish via winget/Scoop. Ship signed PS scripts; `qctl completion powershell` and `qctl man` install helpers.
- **Telemetry**: Opt-in only. Minimal fields: anonymized install ID, command_name, duration_ms, exit_code, version, os/arch. Arguments/paths/env only with an explicit “extended” flag.
- **SBOM**: MVP runtime-only; GA full transitive CycloneDX attached to OCI manifests and published alongside artifacts.
- **Cache eviction**: LRU with configurable max size (global default, profile-overridable). Commands: `qctl cache ls|prune|clean --all`. Background prune on command exit (jittered).
- **Self-update**: Weekly auto-check (jittered) on command use. Channels: stable (default) and beta. Enterprise: disable auto-check, pin channel/version, or mirror via internal endpoint.

### CLI UX & Commands
- **Binary**: Single `qctl` with subcommands: `qqq`, `qbit`, `qrun`, `qstudio`.
- **Global flags**: `--config <path>`, `--profile <name>`, `--env <dev|stage|qa|prod>`, `--verbose`, `--debug`, `--color <auto|always|never>`, `--output <text|json>`, `--yes`, `--dry-run`, `--timeout <sec>`, `--log.level <info|debug|trace>`, `--version`, `--help`.
- **Config precedence**: Built-ins < global config < project config < env `QCTL_*` < CLI flags.
- **Interactivity**: Non-interactive when `--yes` or no TTY; interactive uses prompts and confirmations.
- **Exit codes**: 0 success; 1 generic; 2 usage/config; 3 network; 4 auth; 5 not found; 6 validation; 7 integrity/signature; 8 conflict; 9 cancelled.
- **Help/completion**: `qctl --help`, `qctl <mode> --help`; `qctl completion <bash|zsh|fish|pwsh>`; `qctl man` writes manpages to build dir.
- **Examples**:
  - `qctl qqq new my-app --template web-basic --non-interactive`
  - `qctl qqq generate --spec app.yaml --dry-run`
  - `qctl qbit add io.qbits/auth@^2`
  - `qctl qrun publish --env stage`
  - `qctl qstudio enhance --request "add RBAC to admin panel" --confirm`

### Architecture
- **Modules**: `qctl-core`, `qctl-qqq`, `qctl-qbit`, `qctl-qrun`, `qctl-qstudio`, `qctl-shared` (logging, config, HTTP, crypto), `qctl-integration-tests`.
- **Ports/Adapters**:
  - Ports: `TemplateProvider`, `QBitRegistry`, `QRunApi`, `LlmProvider`, `AuthProvider`, `InstallerPublisher`, `SignerVerifier`, `ArtifactStore`.
  - Adapters: HTTP clients, filesystem cache, OS keychain, CLI I/O.
- **Config system**: `qctl.yaml` loader merges built-ins, global, project, env, and flags; validates against embedded JSON Schema; prints actionable errors with JSON Pointer paths.
- **Local cache/state** (OS/XDG paths): `templates/<name>/<version>/`, `qbits/<name>/<version>/`, `artifacts/<hash>/`, `trust/keys/`, `signatures/`, `manifests/`, `locks/`; per-project `qbits.lock` and `vendor/qbits`.
- **Security model**: Pinned trust roots; signature verification (templates/qBits/releases); optional TLS pinning; secrets never logged; OS keychain for tokens by default.
- **Plugin model**: Discover `CommandPlugin` JARs via ServiceLoader in `~/.qctl/plugins` and project `.qctl/plugins`; signature check; isolated classloaders; `qctl plugin list/install/remove`.
- **Observability**: Structured logs; `--output json` for machine-readable results; `qctl doctor` for diagnostics; qStudio activity ledger.

### qqq (Scaffolding)
- **Template discovery**: Search/list from `templates.qrun.io` (signed metadata); support URL/local path.
- **Template format**: Manifest (versioned), inputs schema, file templates with placeholders/partials, post-gen hooks; signature over manifest + content hash tree.
- **Generation flows**: Interactive prompts from schema; non-interactive via flags or `--spec app.yaml`. Idempotent regeneration with conflict detection and `--force/--merge`.
- **Post-gen**: Steps `build`, `run`, `healthcheck` configurable via `qqq.postGen`; honor `--dry-run`.

### qBit (Package Manager-like)
- **Registry API**: `registry.qrun.io` search, package metadata, tarball download, detached signatures, provenance.
- **Commands**: `search`, `add <name[@range]>`, `update [<name>]`, `remove <name>`, `audit`, `list`, `info <name>`, `publish`.
- **Resolution**: SemVer ranges with dedupe; defaults per decisions above; conflict diagnostics; vendored vs linked modes.
- **Lockfile**: `qbits.lock` includes name, version, resolved URL, integrity (sha512), dependencies, signature, source registry, vendor path.

### qRun (Env & Deploy)
- **Packaging**: Default OCI images via Buildpacks (Paketo) or Jib; fallback `zip+manifest`.
- **Pipeline**: `package` → `verify` (SBOM, signatures, policy) → `publish` (to `cdn.qrun.io`/registry) → `promote` → `rollback`.
- **Config/secrets**: Profiles and overlays; secrets providers: OS keychain (default), env, file path.
- **Auth**: OIDC device/browser flows; least-privilege scopes.
- **Observability**: `logs --follow`, `status`, `metrics`, `doctor`.

### qStudio (LLM Assist)
- **Sources**: Local code, rules, DB schema, OpenAPI, tests, commit history using allow/deny lists.
- **Prompt assembly**: Chunking, token budgeting, reproducibility with local ledger; offline/local mode first.
- **Providers**: Pluggable; OpenAI/Anthropic/Azure/self-hosted; retries/backoff, cost accounting; policy enforcement.
- **Outputs**: Propose edits/diffs/new files/tests/migrations; preview; `--confirm` to apply.
- **Privacy**: On-device redaction and policy checks before any provider call.

### Cross-Platform Packaging & Installers
- **Targets**: macOS arm64/x64; Linux x64/arm64; Windows x64.
- **Distribution**: Prefer GraalVM Native Image single-binary; fallback jlink/jpackage minimal JRE.
- **Installers**: Homebrew tap, winget, Scoop, Linux one-liner (apt/yum/zypper detection) plus manual archives.
- **Self-update**: `qctl update --channel <stable|beta>` with signed manifests; offline-aware.

### Security
- **Supply chain**: Code signing, macOS notarization, Windows Authenticode; CycloneDX SBOM; provenance; signature verification for templates/qBits/releases; optional TLS pinning.
- **Key management**: Bundled root keys + rotation; trust store under cache; revocation list sync; warnings on stale keys.
- **Secret handling**: OS keychain for tokens; redaction in logs; zero secrets in telemetry.

### Developer Experience & IntelliJ Standards
- **Build**: Maven Wrapper + Java Toolchains (latest LTS Temurin); modular reactor; reproducible builds via enforcer.
- **Quality**: Spotless + Google Java Format, Checkstyle, Error Prone, Nullness annotations; Qodana in CI.
- **Testing**: JUnit 5, AssertJ, Mockito; Testcontainers; JaCoCo coverage; golden CLI tests.
- **IDE**: `.editorconfig`, `.run/` configs, code style and inspections committed; Live Templates.
- **Native Image**: Pinned GraalVM; reflection configs (Picocli, Jackson, SnakeYAML); debug profile.

### Configuration & Default qctl.yaml Schema
- **Precedence (lowest → highest)**: Built-in defaults < Global config < Project config < Environment (`QCTL_*`) < CLI flags.
- **Default locations**:
  - macOS: `~/Library/Application Support/qctl/qctl.yaml`
  - Linux: `$XDG_CONFIG_HOME/qctl/qctl.yaml` (fallback `~/.config/qctl/qctl.yaml`)
  - Windows: `%APPDATA%\qctl\qctl.yaml`
  - Cache dirs follow OS/XDG (e.g., `~/.cache/qctl`, `~/Library/Caches/qctl`, `%LOCALAPPDATA%\qctl\Cache`).
- **Environment variable mapping**: `QCTL_<SECTION>_<SUBKEY>=value` (dot-path convention); examples in sample below.
- **JSON Schema (concise)**:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "qctl config",
  "type": "object",
  "properties": {
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
    "profiles": { "type": "object", "additionalProperties": {"$ref": "#"} },
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
        "enabled": {"type": "boolean"},
        "explainCommand": {"type": "string"}
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
  verifySignatures: true

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
  explainCommand: "telemetry explain"

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

### External Dependencies (and why)
- **Picocli**: Mature CLI parsing, subcommand trees, autocompletion, GraalVM-friendly, great manpage support.
- **Jackson (JSON) + Jackson YAML / SnakeYAML**: Robust JSON/YAML parsing, schema-friendly, broad ecosystem.
- **JSON Schema validator**: Validates `qctl.yaml` with actionable errors; prevents misconfigurations early.
- **java.net.http (JDK) or OkHttp**: Reliable HTTP client; prefer JDK to minimize deps; OkHttp if advanced features (HTTP/2 tuning, connection pooling) are needed.
- **Nimbus JOSE + OIDC**: Standards-compliant OAuth/OIDC flows and JWT handling.
- **BouncyCastle**: Cryptography/sigs and optional PGP support for templates/qBits until Sigstore migration.
- **CycloneDX Maven Plugin**: SBOM generation for supply-chain visibility and compliance.
- **JUnit 5, AssertJ, Mockito**: Unit testing ergonomics and expressive assertions/mocking.
- **Testcontainers**: Realistic integration tests against services (registries, APIs) without bespoke infra.
- **Spotless, Checkstyle, Error Prone, JaCoCo**: Formatting, static analysis, and coverage for maintainability.
- **Qodana**: CI static analysis at scale.
- **Renovate/Dependabot**: Automated dependency updates with policy control.
- **GraalVM Native Image**: Single-binary distribution for fast startup and simple install.
- **Jib / Buildpacks (Paketo)**: Build OCI images without a Docker daemon; good for CI and hermetic builds.
- **cosign (integration)**: Verify/publish signatures for OCI artifacts with Sigstore.

---

## 2) Build Checklist

### Milestones
- **MVP**: Core CLI; `qqq` basic scaffolding; `qbit` add/update/remove with lockfile; `qrun` package/publish to mock; `qstudio` plan/preview offline; config/telemetry; macOS/Linux binaries.
- **Beta**: Windows support; signature verification (templates/qBits/releases); `promote/rollback`; provider integrations (OpenAI/Anthropic/Azure); Homebrew/winget/Scoop; self-update.
- **GA**: SBOM/signing, notarization; `audit`; full docs; stable plugin API; performance hardening.
- **Hardening**: Key rotation, TLS pinning, fuzz/chaos, e2e durability, reproducibility proofing.

### Steps (Objective • Artifacts • DoD • Deps • Owner)
1) Project bootstrap
- Artifacts: Root `pom.xml` + modules; `.editorconfig`; `.run/`; Spotless/Checkstyle; CI skeleton.
- DoD: `mvn -q -DskipTests verify` green locally and in CI; style enforced.
- Deps: Java LTS, Maven.

2) Core CLI and config
- Artifacts: `qctl-core`, `qctl-shared`; global flags; config loader/merger; JSON Schema validation; logging.
- DoD: `qctl --help` and config precedence tests pass.
- Deps: Picocli, Jackson, JSON Schema validator.

3) Auth (OIDC)
- Artifacts: `AuthProvider` port; Nimbus adapter; OS keychain adapter; profiles.
- DoD: `qctl auth login/logout/whoami` works against stub issuer; tokens stored securely; unit tests.
- Deps: Nimbus JOSE, keychain APIs.

4) qqq scaffolding v1
- Artifacts: `TemplateProvider` (HTTP/fs), renderer (Handlebars), post-gen hooks.
- DoD: `qctl qqq new my-app --template web-basic` produces runnable sample; idempotent re-run; tests.
- Deps: HTTP client, Handlebars, file templating.

5) qbit resolver and lockfile
- Artifacts: `QBitRegistry` adapter; lockfile read/write; vendor/linked modes; integrity checks.
- DoD: `qctl qbit add` resolves and vendors; `qbits.lock` stable; integrity verified; snapshot tests.
- Deps: SemVer lib or in-house; hashing; PGP for signatures.

6) qrun packaging/publish (mock backend)
- Artifacts: Packager (Jib/Buildpacks), publisher, verify step (hash/SBOM).
- DoD: `qctl qrun package|publish` to mock; SBOM emitted; CI artifact.
- Deps: Jib/Buildpacks, CycloneDX.

7) qstudio offline planning
- Artifacts: Project indexer, policy, ledger, diff previewer.
- DoD: `qctl qstudio plan --rules rules.yaml --out plan.md` writes plan; tests.
- Deps: None external yet.

8) Completion scripts and man pages
- Artifacts: Completion subcommand; man generator; packaged assets.
- DoD: `qctl completion zsh` works; `qctl man` outputs pages; docs updated.
- Deps: Picocli.

9) Telemetry (opt-in) and logging polish
- Artifacts: Telemetry gate, event schemas, local “telemetry explain”.
- DoD: Disabled by default; enabling emits minimal events; explain shows fields.
- Deps: Logger only.

10) Security: signatures and trust store
- Artifacts: Trust store, signature verifier, key rotation, revocation sync.
- DoD: Signed artifacts verify; unsigned blocked unless override; negative tests (bad sigs) pass.
- Deps: BouncyCastle; cosign integration.

11) Real backend integration
- Artifacts: HTTP adapters to `templates.qrun.io`, `registry.qrun.io`, `api.qrun.io`, `studio.qrun.io`; retries/backoff.
- DoD: E2E flows against staging; contract/golden tests.
- Deps: HTTP, OIDC.

12) Windows support and installers
- Artifacts: CI matrix; winget/Scoop manifests; code signing; PS completion module.
- DoD: Installers smoke-tested in CI VMs; signed; `qctl` works on Windows.
- Deps: GraalVM/jpackage; signing credentials.

13) Self-update channel
- Artifacts: Update client; channel logic; delta downloads; rollback.
- DoD: `qctl update` works with signed manifests; offline-aware; e2e tests.
- Deps: HTTP; signature verify.

14) Docs and examples
- Artifacts: Quickstart; `docs/`; examples dir; diagnostics guide.
- DoD: Docs published; `qctl doctor` implemented.
- Deps: None.

15) GA hardening
- Artifacts: Repro checks; load tests; fuzzers; stricter validations/policies.
- DoD: SLAs met; reproducible artifacts; security review sign-off.
- Deps: Test infra.

---

## 3) Open Questions
- **Template manifest versioning and signing migration**: Confirm the versioning scheme and timeline to migrate templates/qBits from PGP to Sigstore. Proposed: support both for two minor releases, then require Sigstore.
- **Exact registry API surface**: Finalize endpoints, pagination, and auth scopes for `registry.qrun.io` and `templates.qrun.io`.
- **Default cache size limits**: Recommend 2 GB default with LRU eviction; confirm enterprise defaults and override mechanisms.
- **Minimum supported OS versions**: Define minimum macOS, Windows, and Linux glibc/musl baselines for native images and installers.
- **Homebrew tap namespace**: Confirm tap owner (`qrun-io/tap/qctl`) and release automation steps.
- **Telemetry legal text**: Confirm exact opt-in wording and data retention policy; enterprise disablement policy.
- **qStudio ledger storage location**: Default to project `.qctl/ledger/` vs global cache; confirm retention and redaction strategy.
- **Proxy and corporate CA support**: Confirm how enterprise CA bundles and proxies are configured (env vars vs `qctl.yaml`), and TLS pinning interplay.
