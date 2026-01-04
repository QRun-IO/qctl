# qctl Vision & Architecture

## Purpose

qctl is the unified CLI for the QRun platform - a complete developer workflow tool for building, packaging, deploying, and managing QQQ-based applications. It consolidates project scaffolding, dependency management, artifact publishing, and AI-assisted development into a single binary.

## Core Principles

1. **Single Binary**: One native executable for all QRun platform operations
2. **Offline-First**: Full functionality without network when possible (hermetic mode)
3. **Declarative Config**: YAML-driven configuration with sensible defaults
4. **Platform Parity**: Consistent behavior across macOS, Linux, Windows
5. **Developer Experience**: Fast startup (native), helpful errors, shell completion

## Command Structure

```
qctl
├── qqq                    # Project scaffolding
│   ├── new <name>         # Create new project from template
│   ├── list               # List available templates
│   └── upgrade            # Upgrade project to newer template version
├── qbit                   # Dependency management (QQQ modules)
│   ├── search <query>     # Search registry for qBits
│   ├── add <pkg>[@ver]    # Add qBit to project
│   ├── remove <pkg>       # Remove qBit
│   ├── update [pkg]       # Update qBit(s)
│   ├── list               # List installed qBits
│   └── resolve            # Resolve and lock dependencies
├── qrun                   # Build, package, deploy
│   ├── package            # Build OCI image via Jib
│   ├── publish            # Push to registry, create release
│   ├── status             # Check deployment status
│   └── logs               # Stream application logs
├── qstudio                # AI-assisted development
│   └── plan               # Generate implementation plan
├── auth                   # Authentication
│   ├── login              # Authenticate (device flow or API key)
│   ├── logout             # Clear credentials
│   └── whoami             # Show current identity
└── cache                  # Local cache management
    ├── ls                 # List cached items
    ├── prune              # Remove old entries
    └── clean              # Clear entire cache
```

## Module Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         qctl-cli                                │
│              (Native image aggregator, Main class)              │
└─────────────────────────────────────────────────────────────────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│  qctl-qqq   │ │  qctl-qbit  │ │  qctl-qrun  │ │qctl-qstudio │
│  Templates  │ │  Packages   │ │  Deploy     │ │  AI Plan    │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
         │              │              │              │
         └──────────────┴──────┬───────┴──────────────┘
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                         qctl-core                               │
│  Config, HTTP, Auth, Cache, Output, Logging, CLI framework      │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                        qctl-shared                              │
│          DTOs, SemVer, SPI, Utilities, Constants                │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow: `qctl qqq init`

```
User: qctl qqq init qqq-app my-project

1. Load config (defaults → global → project → env → flags)
2. Resolve template:
   - Check local cache for "qqq-app" template
   - If missing/outdated, fetch from templates-hub
   - Verify signature (warn-only in V1)
3. Prompt for variables (or use --no-prompt defaults)
4. Evaluate computed variables (e.g., packagePath from packageName)
5. Render template via Apache Velocity:
   - Process template.yaml manifest (schema v2)
   - Substitute $variables in file content and paths
   - Convert __VARNAME__ to $VARNAME in paths (CI-safe syntax)
   - Apply transforms (delete patterns)
   - Copy rendered files to target directory
6. Post-generation hooks (if defined)
7. Output success message with next steps
```

## Configuration Precedence

```
Lowest ──────────────────────────────────────────────────► Highest

Built-in    Global Config    Project Config    Env Vars    CLI Flags
defaults    ~/...qctl.yaml   ./qctl.yaml       QCTL_*      --flag
```

## Exit Codes

| Code | Meaning                    | When                           |
|------|----------------------------|--------------------------------|
| 0    | Success                    | Operation completed            |
| 1    | Generic error              | Unexpected failure             |
| 2    | Usage/config error         | Invalid arguments or config    |
| 3    | Network error              | Connection failed, timeout     |
| 4    | Auth error                 | 401/403 from API               |
| 5    | Not found                  | 404 from API                   |
| 6    | Validation error           | 400/422, bad input             |
| 7    | Integrity error            | Hash/signature mismatch        |
| 8    | Conflict                   | 409, state conflict            |
| 9    | Cancelled                  | User interrupted (Ctrl+C)      |

## V1 Scope (Current Target)

**Included**:
- `qqq new` from local and HTTPS templates
- `qbit` search/add/list/update/remove with lockfile
- `qrun package` (Jib) and `publish` to registry
- `qstudio plan` (offline, writes plan.md)
- Config loading, validation, profiles
- Native builds for macOS/Linux; jlink for Windows

**Deferred (Post-V1)**:
- Sigstore enforcement (currently warn-only)
- Plugin system
- Self-update mechanism
- `qstudio` diff apply (AI code changes)
- Streaming logs
- Promote/rollback workflows

## Template Structure (new-qqq-application)

```
templates/new-qqq-application/
├── template.yaml           # Manifest with prompts, computed vars, transforms
├── template/               # Template files directory
│   ├── README.md           # Velocity template with $variables
│   ├── pom.xml             # Maven POM with $groupId, $artifactId, etc.
│   └── src/
│       └── main/
│           └── java/
│               └── $packagePath/        # Path variable substitution
│                   └── Application.java # Uses $packageName, $projectName
```

**template.yaml Schema v2**:
```yaml
schemaVersion: 2
id: new-qqq-application
name: New QQQ Application
version: 1.0.0
prompts:
  - name: packageName
    message: Java package name
    type: text
    defaultValue: com.example
    required: true
computed:
  - name: packagePath
    expression: "$str.replace($packageName, '.', '/')"
transforms:
  - type: delete
    pattern: "**/.gitkeep"
```

## Key Files

- `qctl.yaml` - Project and global configuration
- `qbits.lock` - Locked dependency versions (JSON)
- `vendor/qbits/` - Vendored qBit packages
- `.qctl/` - Project-local qctl state

## Technology Stack

- **Language**: Java 21 (Temurin/GraalVM)
- **CLI**: Picocli 4.7.5
- **Config**: Jackson YAML + JSON Schema validation
- **HTTP**: JDK HttpClient with retry/backoff
- **Templates**: Apache Velocity 2.4.1
- **Build**: Maven 3.9+, GraalVM native-image
- **Test**: JUnit 5, AssertJ, Mockito, Testcontainers

## Testing Architecture

```
qctl-integration-tests/
├── src/test/java/io/qrun/qctl/e2e/
│   ├── InitCommandE2ETest.java      # CLI flag tests
│   ├── InitCommandFullE2ETest.java  # Full variable/transform tests
│   └── harness/
│       ├── CommandTestHarness.java  # In-process command execution
│       ├── TestTemplateRegistry.java # Fixture-based registry
│       └── TestableConsoleUI.java   # Simulated user input
└── src/test/resources/fixtures/templates/
    ├── test-minimal/                # Basic template
    ├── test-computed-vars/          # Computed variable tests
    ├── test-full-substitution/      # Variable replacement tests
    └── test-transforms/             # Transform operation tests
```

**Path Variable Convention**: Use `__VARNAME__` in directory names for templates. The engine converts this to `$VARNAME` before Velocity processing, avoiding shell/Maven interpretation issues.
