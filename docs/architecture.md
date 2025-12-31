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

## Data Flow: `qctl qqq new`

```
User: qctl qqq new voyage --template qqq-app

1. Load config (defaults → global → project → env → flags)
2. Resolve template:
   - Check local cache for "qqq-app" template
   - If missing/outdated, fetch from templates.qrun.io
   - Verify signature (warn-only in V1)
3. Prompt for variables (or use --non-interactive defaults)
4. Render template via Handlebars:
   - Process template.yaml manifest
   - Apply variable substitutions
   - Copy/transform files to target directory
5. Post-generation hooks:
   - Run `build` phase (mvn compile)
   - Run `run` phase (start app)
   - Run `healthcheck` phase (verify startup)
6. Output success message with next steps
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

## Template Structure (qqq-app)

```
templates/qqq-app/
├── template.yaml           # Manifest with prompts and hooks
├── README.md.hbs           # Handlebars template
├── pom.xml.hbs
├── src/
│   └── main/
│       └── java/
│           └── {{packagePath}}/
│               ├── Application.java.hbs
│               └── {{name}}QInstance.java.hbs
└── .qctl/
    └── template-version    # Tracks template version for upgrades
```

## Key Files

- `qctl.yaml` - Project and global configuration
- `qbits.lock` - Locked dependency versions (JSON)
- `vendor/qbits/` - Vendored qBit packages
- `.qctl/` - Project-local qctl state

## Technology Stack

- **Language**: Java 21 (Temurin)
- **CLI**: Picocli 4.7.5
- **Config**: Jackson YAML + JSON Schema validation
- **HTTP**: JDK HttpClient with retry/backoff
- **Templates**: Handlebars 4.4.0
- **Build**: Maven 3.9+, GraalVM native-image
- **Test**: JUnit 5, AssertJ, Mockito, Testcontainers
