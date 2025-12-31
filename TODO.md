# TODO

## Completed

### Phase 1: Framework Cleanup
- [x] Update dependencies to latest stable versions
- [x] Add ExitCodes constants class in qctl-shared
- [x] Update commands to use ExitCodes constants
- [x] Fix SystemPaths.ensureDir() logging
- [x] Remove duplicate Javadoc in JsonMerge.java
- [x] Add Handlebars dependency to qctl-qqq

### Phase 2: qqq init Command
- [x] Create InitCommand with picocli structure
- [x] Create TemplateResolver for git/local/github sources
- [x] Create TemplateManifest parser
- [x] Create TemplateEngine with Handlebars rendering
- [x] Create PromptRunner for inquirer-style prompts
- [x] Create PostGenHookRunner for executing hooks
- [x] Create TemplatesHub to fetch templates.yaml from hub
- [x] Create ListCommand to list available templates
- [x] Fix Handlebars native image resource loading
- [x] Remove native profile from qctl-core (only qctl-cli builds)

### Phase 3: CI/CD & Distribution
- [x] Create GitHub Actions workflow for multi-platform builds
- [x] Add Linux ARM64 build target
- [x] Fix macOS runner (macos-13 deprecated, use macos-15)
- [x] Add Homebrew formula with auto-update
- [x] Add Scoop manifest for Windows
- [x] Add AUR PKGBUILD for Arch Linux
- [x] Add Dockerfile for container distribution
- [x] Add Docker build/push to GHCR in workflow
- [x] Create comprehensive README
- [x] Verify GitHub Actions builds pass

## Pending

### Release
- [ ] Create first release (v0.1.0)
- [ ] Verify all distribution channels work
- [ ] Publish AUR package to aur.archlinux.org

### Template System Enhancements
- [ ] Add template versioning (semver tags)
- [ ] Add template signature verification
- [ ] Add template caching for offline use
- [ ] Add `qctl qqq update` to check for template updates

### qbit Commands
- [ ] Implement `qctl qbit add <package>`
- [ ] Implement `qctl qbit remove <package>`
- [ ] Implement `qctl qbit update`
- [ ] Implement lockfile integrity verification

### qrun Commands
- [ ] Implement `qctl qrun deploy`
- [ ] Implement `qctl qrun logs`
- [ ] Implement `qctl qrun rollback`
- [ ] OCI image building

### qstudio Commands
- [ ] Implement AI-assisted planning
- [ ] Integration with Claude API

### Auth System
- [ ] OAuth2 device flow implementation
- [ ] Token refresh mechanism
- [ ] Secure credential storage (keychain integration)

### Testing
- [ ] Add unit tests for TemplateEngine
- [ ] Add unit tests for TemplatesHub
- [ ] Add integration tests for init command
- [ ] Add golden tests for scaffolded projects

### Documentation
- [ ] Add man pages
- [ ] Add shell completion scripts (bash, zsh, fish)
- [ ] Add examples directory with sample templates

## Distribution Channels

| Channel | Install Command | Manifest |
|---------|-----------------|----------|
| Homebrew | `brew tap QRun-IO/qctl && brew install qctl` | `HomebrewFormula/qctl.rb` |
| Scoop | `scoop bucket add qrun https://github.com/QRun-IO/qctl && scoop install qctl` | `scoop/qctl.json` |
| AUR | `yay -S qctl-bin` | `aur/PKGBUILD` |
| Docker | `docker run ghcr.io/qrun-io/qctl` | `Dockerfile` |
| GitHub | Download from Releases | `.github/workflows/build.yml` |

## Supported Platforms

| Platform | Architecture | Artifact |
|----------|--------------|----------|
| Linux | x64 | `qctl-linux-amd64` |
| Linux | ARM64 | `qctl-linux-arm64` |
| macOS | Intel | `qctl-macos-amd64` |
| macOS | Apple Silicon | `qctl-macos-arm64` |
| Windows | x64 | `qctl-windows-amd64.exe` |

## Notes

### Native Image Requirements
- GraalVM 21+ required
- Only `qctl-cli` module has native profile
- Handlebars requires `--initialize-at-run-time=com.github.jknack.handlebars.helper.DefaultHelperRegistry`
- Jackson requires reflection config for record types

### Templates Hub
- Central registry: https://github.com/QRun-IO/templates-hub
- Templates defined in `templates.yaml`
- Each template references a git repo with Handlebars templates

### Release Process
```bash
git tag v1.0.0
git push origin v1.0.0
```
This triggers GitHub Actions to build, release, and update all package manifests.
