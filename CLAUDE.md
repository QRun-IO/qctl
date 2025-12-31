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

# Run single test class
mvn test -pl qctl-core -Dtest=ConfigLoaderTest

# Run single test method
mvn test -pl qctl-core -Dtest=ConfigLoaderTest#loads_defaults_when_no_file

# Checkstyle only
mvn checkstyle:check

# Spotless check/apply
mvn spotless:check
mvn spotless:apply

# Native build (requires GraalVM 21)
./scripts/build-native.sh
# or manually:
mvn -Pnative -DskipTests -pl qctl-cli -am package
```

## Module Structure

```
qctl-parent (reactor)
├── qctl-shared     # DTOs, SemVer, SPI interfaces, utilities
├── qctl-core       # Config, HTTP client, auth, cache, logging, Main entrypoint
├── qctl-qqq        # Template scaffolding commands
├── qctl-qbit       # Package management commands, lockfile handling
├── qctl-qrun       # OCI packaging and deployment commands
├── qctl-qstudio    # AI planning commands (offline V1)
├── qctl-cli        # Aggregator for native image build
└── qctl-integration-tests  # Golden tests, contract tests with Prism mock
```

## Architecture

**Plugin System**: Commands are discovered via `ServiceLoader<CommandPlugin>`. Each feature module (qctl-qqq, qctl-qbit, etc.) implements `CommandPlugin` in `qctl-shared/spi/` and registers via `META-INF/services`.

**CLI Framework**: Picocli with annotation-based command definitions. Main entrypoint in `qctl-core/Main.java` registers core commands and discovers plugins.

**Config System**: YAML config loaded from OS-specific paths, merged with env vars (`QCTL_*` prefix) and CLI flags. JSON Schema validation via networknt. Precedence: built-in < global < project < env < flags.

**HTTP Layer**: JDK HttpClient with retry/backoff for 429/5xx. RFC 7807 ProblemDetail error handling.

## Code Style

- **3-space indentation** (Checkstyle enforced)
- **Braces on new line** (`LeftCurly: nl`)
- **Javadoc required** on all methods (flowerbox style)
- **No star imports**
- **Import order**: javax, java, third-party, static (alphabetical within groups)
- **License header** required on all Java files

Flowerbox Javadoc format:
```java
/***************************************************************************
 * Brief description of what this method does.
 *
 * Why: Explain the rationale or context.
 *
 * @param foo description
 * @return description
 * @since 0.1.0
 ***************************************************************************/
```

## Key Patterns

**Exit Codes**: 0=success, 1=generic, 2=usage/config, 3=network, 4=auth, 5=not found, 6=validation, 7=integrity, 8=conflict, 9=cancelled

**Lockfile** (`qbits.lock`): JSON with `lockfileVersion: 1`, `generatedAt`, `packages` map. Atomic writes via temp file + `Files.move(ATOMIC_MOVE)`.

**Config Paths**:
- macOS: `~/Library/Application Support/qctl/qctl.yaml`
- Linux: `$XDG_CONFIG_HOME/qctl/qctl.yaml`
- Windows: `%APPDATA%\qctl\qctl.yaml`

## Testing

- JUnit 5 + AssertJ + Mockito
- Testcontainers for integration tests
- Golden tests: snapshots in `qctl-integration-tests/src/test/resources/golden/`
- Update golden files: `mvn test -Dgolden.update=true`
- Contract tests run against Prism mock on port 4010

## Dependencies

Key versions (see `pom.xml` properties):
- Java 21 (Temurin)
- Picocli 4.7.5
- Jackson 2.17.1
- SLF4J 2.0.13 / Logback 1.5.13
- json-schema-validator 1.4.1
