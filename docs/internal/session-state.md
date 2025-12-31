# Session State

Last updated: 2025-12-31

## Current Status

**CI/CD pipeline is fully operational.** All workflows tested and passing.

## Recently Completed

### CI/CD Workflow Overhaul (2025-12-31)

Fixed broken release workflow with multiple issues:

1. **Native profile** - Moved from main build to `-Pnative` profile in `qctl-cli/pom.xml`
2. **macOS runner** - Updated from retired `macos-13` to `macos-15`
3. **Artifact handling** - Fixed download paths and flattening for release
4. **Docker build** - Fixed binary download to use explicit names instead of patterns
5. **Package updates** - Split into separate jobs:
   - Homebrew: Direct push to `QRun-IO/homebrew-qctl` tap
   - Scoop/AUR: Create PRs (avoids branch protection issues)
6. **Binary leak fix** - Added cleanup and `add-paths` to prevent committing downloaded artifacts

### Releases

- `v0.0.1` - Initial release (failed due to workflow bugs)
- `v0.0.2` - Successful release with all 5 platform binaries

## Current Branch State

- **Branch**: `develop`
- **Last commit**: `334133a fix: remove accidentally committed binary artifacts`
- **Clean**: Yes (no uncommitted changes)

## Infrastructure

### GitHub Secrets Configured
- `HOMEBREW_TAP_TOKEN` - PAT for homebrew tap push

### External Repositories
- `QRun-IO/homebrew-qctl` - Homebrew tap (auto-updated on release)
- `QRun-IO/templates-hub` - Template repository

### Docker Registry
- `ghcr.io/qrun-io/qctl` - Multi-arch images (amd64, arm64)

## Next Steps / TODO

### Immediate
- None - CI/CD is stable

### Short-term
- Continue V1 feature implementation per `docs/internal/prompt-resume.md`
- Implement remaining qrun/qbit commands

### Long-term
- Real OCI registry integration
- OIDC device flow authentication
- Plugin enforcement

## How to Continue

When starting a new session, say "continue from last session" and Claude will:
1. Read this file for context
2. Check git status for any pending work
3. Review `docs/internal/prompt-resume.md` for V1 implementation tasks
4. Resume where left off

## Key Files

| File | Purpose |
|------|---------|
| `CLAUDE.md` | Project overview and build commands |
| `docs/internal/session-state.md` | This file - session continuity |
| `docs/internal/prompt-resume.md` | V1 implementation tasks |
| `docs/internal/checklist.md` | Design decisions checklist |
| `.github/workflows/build.yml` | CI/CD pipeline |
| `qctl-cli/pom.xml` | Native build profile |
