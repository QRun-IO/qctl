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

# Native build (requires GraalVM 21)
JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-21.jdk/Contents/Home mvn clean package -DskipTests

# Output: qctl-cli/target/qctl (native profile is in qctl-cli pom.xml, not a -P flag)

# Test version flag
./qctl-cli/target/qctl --version
```

## Module Structure

```
qctl-parent (reactor)
├── qctl-shared     # DTOs, ExitCodes, SPI interfaces, utilities
├── qctl-core       # Config, HTTP client, auth, cache, logging, Main entrypoint
├── qctl-qqq        # Template scaffolding (init, list) with Handlebars
├── qctl-qbit       # Package management commands, lockfile handling
├── qctl-qrun       # OCI packaging and deployment commands
├── qctl-qstudio    # AI planning commands (offline V1)
└── qctl-cli        # Aggregator for native image build (only module with native profile)
```

## Architecture

**Plugin System**: Commands discovered via `ServiceLoader<CommandPlugin>`. Each module implements `CommandPlugin` and registers via `META-INF/services`.

**CLI Framework**: Picocli with annotation-based commands. Main entrypoint in `qctl-core/Main.java`.

**Template System**: Templates fetched from [QRun-IO/templates-hub](https://github.com/QRun-IO/templates-hub). Handlebars for rendering with custom helpers (camelCase, pascalCase, etc.).

**Interactive Commands**: All console I/O goes through `ConsoleUI` class (`qctl-qqq/template/ConsoleUI.java`). This separates UI from logic. Commands use `ConsoleUI` for prompts, styled output, and selections.

**Version Info**: `VersionProvider` reads from `version.properties` (Maven-filtered). Shows version, build timestamp, and git commit hash.

**Native Image**: GraalVM 21 with reflection config for Jackson records and Handlebars resources.

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

## Distribution

On tagged release (`git tag v1.0.0 && git push origin v1.0.0`):

| Channel | Location | Auto-Updated |
|---------|----------|--------------|
| Homebrew | `HomebrewFormula/qctl.rb` | Yes |
| Scoop | `scoop/qctl.json` | Yes |
| AUR | `aur/PKGBUILD` | Yes |
| Docker | `ghcr.io/qrun-io/qctl` | Yes |
| GitHub | Releases | Yes |

**Platforms**: Linux (x64, ARM64), macOS (Intel, Apple Silicon), Windows (x64)

## Native Image Notes

- Only `qctl-cli` builds native image (other modules removed native profile)
- Handlebars requires `--initialize-at-run-time=com.github.jknack.handlebars.helper.DefaultHelperRegistry`
- Jackson records need reflection config in `META-INF/native-image/reflect-config.json`
- Resources config in `META-INF/native-image/resource-config.json`

## Testing

- JUnit 5 + AssertJ + Mockito
- Run: `mvn test` or `mvn verify`
