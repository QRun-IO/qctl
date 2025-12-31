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

### Phase 3: CI/CD
- [x] Create GitHub Actions workflow for multi-platform builds
- [x] Create README.md

## In Progress

### Native Image Builds
- [ ] Verify GitHub Actions builds complete successfully
- [ ] Test release workflow with a tag

## Pending

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

## Notes

### Native Image Requirements
- GraalVM 21+ required
- Handlebars requires runtime initialization for DefaultHelperRegistry
- Jackson requires reflection config for record types

### Templates Hub
- Central registry: https://github.com/QRun-IO/templates-hub
- Templates defined in `templates.yaml`
- Each template references a git repo with Handlebars templates
