# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

qctl is a multi-module Maven CLI tool for the QRun platform. It provides commands for project scaffolding (`qqq`), package management (`qbit`), deployment (`qrun`), and AI-assisted planning (`qstudio`).

## Build Commands

```bash
# Full build with tests
mvn clean verify

# Fast compile (skip tests)
mvn compile -DskipTests

# Checkstyle only
mvn checkstyle:check

# Spotless check/apply
mvn spotless:check
mvn spotless:apply

# Native build (requires GraalVM 21 and -Pnative profile)
JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-21.jdk/Contents/Home mvn clean package -DskipTests -Pnative

# Output: qctl-cli/target/qctl

# Test version flag
./qctl-cli/target/qctl --version
```

## Module Structure

```
qctl/
├── qctl-shared     # DTOs, ExitCodes, SPI interfaces, utilities
├── qctl-core       # Config, HTTP client, auth, cache, logging, Main entrypoint
├── qctl-qqq        # Template scaffolding (init, list) with Handlebars
├── qctl-qbit       # Package management commands, lockfile handling
├── qctl-qrun       # OCI packaging and deployment commands
├── qctl-qstudio    # AI planning commands (offline V1)
├── qctl-cli        # Aggregator for native image build (has native profile)
├── docs/           # Architecture and design documentation
├── packaging/      # Distribution manifests (Homebrew, Scoop, AUR, Docker)
└── codestyle/      # Checkstyle config and license headers
```

## Architecture

**Plugin System**: Commands discovered via `ServiceLoader<CommandPlugin>`. Each module implements `CommandPlugin` and registers via `META-INF/services`.

**CLI Framework**: Picocli with annotation-based commands. Main entrypoint in `qctl-core/Main.java`.

**Template System**: Templates fetched from [QRun-IO/templates-hub](https://github.com/QRun-IO/templates-hub). Handlebars for rendering with custom helpers (camelCase, pascalCase, etc.).

**Interactive Commands**: All console I/O goes through `ConsoleUI` class (`qctl-qqq/template/ConsoleUI.java`). This separates UI from logic.

**Version Info**: `VersionProvider` reads from `version.properties` (Maven-filtered). Shows version, build timestamp, and git commit hash. Uses `${revision}` property for CI-friendly versioning.

**Native Image**: GraalVM 21 with reflection config. Native build is in a Maven profile (`-Pnative`) in `qctl-cli/pom.xml`.

## Code Style

- **3-space indentation** (Checkstyle enforced)
- **Braces on new line** (`LeftCurly: nl`)
- **Javadoc required** on all methods (flowerbox style)
- **No star imports**
- **Import order**: javax, java, third-party, static (alphabetical)

Flowerbox Javadoc:
```java
/***************************************************************************
 * Brief description.
 *
 * @param foo description
 * @return description
 * @since 0.1.0
 ***************************************************************************/
```

## Key Patterns

**Exit Codes** (`qctl-shared/ExitCodes.java`):
- 0=success, 1=generic, 2=usage, 3=network, 4=auth, 5=not found, 6=validation, 7=integrity, 8=conflict, 9=cancelled

**Config Paths**:
- macOS: `~/Library/Application Support/qctl/qctl.yaml`
- Linux: `$XDG_CONFIG_HOME/qctl/qctl.yaml`
- Windows: `%APPDATA%\qctl\qctl.yaml`

## CI/CD Pipeline

Workflow: `.github/workflows/build.yml`

**Trigger**: Push to `main`/`develop`, tags `v*`, PRs

**Jobs**:
1. `test` - Run `mvn clean verify`
2. `build-native` - Build native images for 5 platforms (only on push, not PRs)
3. `release` - Create GitHub release with artifacts (only on tags)
4. `docker` - Build/push multi-arch Docker image to GHCR
5. `update-homebrew` - Push to `QRun-IO/homebrew-qctl` tap
6. `update-scoop` - Create PR for Scoop manifest update
7. `update-aur` - Create PR for AUR PKGBUILD update

**Build Matrix**:
| Platform | Runner | Artifact |
|----------|--------|----------|
| Linux x64 | ubuntu-latest | qctl-linux-amd64 |
| Linux ARM64 | ubuntu-24.04-arm | qctl-linux-arm64 |
| macOS Intel | macos-15 | qctl-macos-amd64 |
| macOS ARM | macos-14 | qctl-macos-arm64 |
| Windows x64 | windows-latest | qctl-windows-amd64.exe |

**Required Secrets**:
- `HOMEBREW_TAP_TOKEN` - PAT with repo scope for homebrew tap

**Release Process**:
```bash
git tag -a v0.0.3 -m "Release v0.0.3"
git push origin v0.0.3
```

## Distribution

| Channel | Location | Update Method |
|---------|----------|---------------|
| Homebrew | `QRun-IO/homebrew-qctl` | Direct push |
| Scoop | `packaging/scoop/qctl.json` | PR to develop |
| AUR | `packaging/aur/PKGBUILD` | PR to develop |
| Docker | `ghcr.io/qrun-io/qctl` | Direct push |
| GitHub | Releases | Auto-created |

**Install**:
```bash
# Homebrew
brew tap QRun-IO/qctl && brew install qctl

# Docker
docker run ghcr.io/qrun-io/qctl --help
```

## Native Image Notes

- Native profile is in `qctl-cli/pom.xml` (activated with `-Pnative`)
- Handlebars requires `--initialize-at-run-time=com.github.jknack.handlebars.helper.DefaultHelperRegistry`
- Jackson records need reflection config in `META-INF/native-image/reflect-config.json`
- Resources config in `META-INF/native-image/resource-config.json`

## Testing

- JUnit 5 + AssertJ + Mockito
- Run: `mvn test` or `mvn verify`

## Session Continuity

To continue from last session, see `docs/internal/session-state.md` for current progress and next steps.
