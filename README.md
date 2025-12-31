# qctl

Command-line interface for the QRun ecosystem. Scaffold QQQ applications, manage qBits, and deploy to qRun.

## Installation

### Homebrew (macOS/Linux)

```bash
brew tap QRun-IO/qctl
brew install qctl
```

### Scoop (Windows)

```powershell
scoop bucket add qrun https://github.com/QRun-IO/qctl
scoop install qctl
```

### AUR (Arch Linux)

```bash
yay -S qctl-bin
```

### Docker

```bash
docker run --rm ghcr.io/qrun-io/qctl:latest --help

# Mount current directory for project scaffolding
docker run --rm -v $(pwd):/work -w /work ghcr.io/qrun-io/qctl:latest qqq init new-qqq-application my-app
```

### Pre-built Binaries

Download from [Releases](https://github.com/QRun-IO/qctl/releases):

| Platform | Binary |
|----------|--------|
| Linux x64 | `qctl-linux-amd64` |
| Linux ARM64 | `qctl-linux-arm64` |
| macOS Intel | `qctl-macos-amd64` |
| macOS Apple Silicon | `qctl-macos-arm64` |
| Windows | `qctl-windows-amd64.exe` |

```bash
# Example: macOS Apple Silicon
curl -L https://github.com/QRun-IO/qctl/releases/latest/download/qctl-macos-arm64 -o qctl
chmod +x qctl
sudo mv qctl /usr/local/bin/
```

### Build from Source

Requires GraalVM 21+:

```bash
git clone https://github.com/QRun-IO/qctl.git
cd qctl
JAVA_HOME=/path/to/graalvm mvn clean package -DskipTests -Pnative
./qctl-cli/target/qctl --version
```

## Quick Start

```bash
# List available project templates
qctl qqq list

# Scaffold a new QQQ application
qctl qqq init new-qqq-application my-app

# With custom variables
qctl qqq init new-qqq-application my-app \
  --var=projectName=MyApp \
  --var=groupId=com.example \
  --var=package=com.example.myapp
```

## Commands

### qqq - Scaffolding

| Command | Description |
|---------|-------------|
| `qctl qqq list` | List available templates from the hub |
| `qctl qqq init <template> <dir>` | Initialize a new project from template |

**Init Options:**
- `--var=key=value` - Set template variable
- `--version=1.0.0` - Use specific template version
- `--dry-run` - Preview without writing files
- `--force` - Overwrite existing directory
- `--no-prompt` - Skip interactive prompts
- `--skip-hooks` - Skip post-generation hooks

### auth - Authentication

| Command | Description |
|---------|-------------|
| `qctl auth login` | Authenticate to qRun APIs |
| `qctl auth logout` | Clear stored credentials |

### qbit - Package Management

| Command | Description |
|---------|-------------|
| `qctl qbit resolve` | Resolve qBit dependencies |

### qrun - Deployment

| Command | Description |
|---------|-------------|
| `qctl qrun status` | Check deployment status |
| `qctl qrun package` | Package application |
| `qctl qrun publish` | Publish to registry |

### cache - Maintenance

| Command | Description |
|---------|-------------|
| `qctl cache clean` | Clear local cache |

## Global Options

```
--debug       Enable debug logging
--verbose     Enable verbose output
--offline     Run without network access
--hermetic    Resolve strictly from lockfile
--output      Output format: text|json
--help        Show help
--version     Show version
```

## Templates

Templates are managed in [QRun-IO/templates-hub](https://github.com/QRun-IO/templates-hub). Available templates:

| ID | Description |
|----|-------------|
| `new-qqq-application` | Complete QQQ application with entities, database, and dashboard |

## Project Structure

```
qctl/
├── qctl-cli/       # Native image entry point (aggregates all modules)
├── qctl-core/      # Core commands (auth, cache)
├── qctl-shared/    # Shared utilities and constants
├── qctl-qqq/       # Scaffolding commands (init, list)
├── qctl-qbit/      # Package management commands
├── qctl-qrun/      # Deployment commands
└── qctl-qstudio/   # Planning commands
```

## Development

```bash
# Run tests
mvn clean verify

# Build native image (requires GraalVM)
JAVA_HOME=/path/to/graalvm mvn clean package -DskipTests -Pnative

# Run with JVM (faster iteration)
mvn clean package -DskipTests
java -jar qctl-cli/target/qctl-cli-0.1.0-SNAPSHOT.jar --help
```

## Releasing

Create a tagged release to trigger the full CI/CD pipeline:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This automatically:
1. Runs tests on Ubuntu
2. Builds native binaries for all platforms
3. Creates a GitHub Release with binaries attached
4. Pushes Docker image to `ghcr.io/qrun-io/qctl`
5. Updates package manifests (Homebrew, Scoop, AUR) with correct checksums

### Distribution Channels

| Channel | Package | Auto-Updated |
|---------|---------|--------------|
| Homebrew | `HomebrewFormula/qctl.rb` | Yes |
| Scoop | `scoop/qctl.json` | Yes |
| AUR | `aur/PKGBUILD` | Yes |
| Docker | `ghcr.io/qrun-io/qctl` | Yes |
| GitHub | Releases page | Yes |

### Supported Platforms

| Platform | Architecture | Binary |
|----------|--------------|--------|
| Linux | x64 | `qctl-linux-amd64` |
| Linux | ARM64 | `qctl-linux-arm64` |
| macOS | Intel | `qctl-macos-amd64` |
| macOS | Apple Silicon | `qctl-macos-arm64` |
| Windows | x64 | `qctl-windows-amd64.exe` |

## License

AGPL-3.0 - See [LICENSE](LICENSE) for details.
