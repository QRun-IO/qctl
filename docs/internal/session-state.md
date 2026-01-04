# Session State

Last updated: 2026-01-03

## Current Status

**Stories #18-24 complete.** Velocity template engine, computed variables, transforms, manifest v2, templates-hub migration, Voyage mock API, and prompt validation all implemented. Ready for Story #25 (error messages).

## Recently Completed

### Story #24 - Add Prompt Validation (2026-01-03)

Implemented input validation with retry loop for template prompts:
- Created `PromptValidation` record for validation rules (pattern, length, range, enum)
- Created `PromptValidator` class with support for required, pattern, length, range, and enum validation
- Updated `PromptRunner` to validate input and re-prompt on failure
- Added validation for `--var` CLI overrides with fallback to interactive prompt
- Added `ConsoleUI` methods for validation feedback (error checkmark, success checkmark, warning)
- Added 20 unit tests for PromptValidator

Manifest schema now supports:
```yaml
prompts:
  - name: packageName
    message: "Java package"
    required: true
    validation:
      pattern: "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$"
      message: "Must be a valid Java package name"
```

### Story #23 - Voyage Mock API Integration (2026-01-03)

Implemented TemplateRegistry abstraction for template management:
- Created `TemplateRegistry` interface with list/get/download/refresh methods
- Created `TemplateInfo` record for template metadata
- Created `MockTemplateRegistry` with embedded template data
- Created `GitHubTemplateRegistry` wrapping existing TemplatesHub
- Created `TemplateRegistryFactory` for registry selection via env var
- Refactored `ListCommand` to use TemplateRegistry with `--refresh` flag
- Refactored `InitCommand` to use TemplateRegistry
- Added 13 unit tests for registry classes
- Added native-image reflection config for TemplateInfo

### Story #22 - Migrate templates-hub to Velocity (2026-01-03)

Completed migration of new-qqq-application-template:
- Restructured template repo with `template/` subdirectory
- Converted all Java files to Velocity syntax (`$variable`, `${variable}`)
- Fixed pom.xml with literal blocks (`#[[${maven.prop}]]#`) for Maven properties
- Added native-image reflection config for Velocity runtime classes
- Implemented verbatim file handling for shell scripts (mvnw, gradlew)
- Added Maven property detection to skip validation for `${prop.name}` patterns
- All changes pushed to both template repo and templates-hub

### Story #21 - Template Manifest Schema v2 (2026-01-03)

Updated manifest schema to v2 with backward compatibility:
- Added `schemaVersion` field (Integer) to TemplateManifest and TemplateEntry
- Added `minimumQctlVersion` field with version check in InitCommand
- Added `required` field (Boolean) to Prompt record
- Added `phase` field (String) to PostGenHook record
- Added `getEffectiveSchemaVersion()` helper method (defaults to 1)
- Added `isVersionSatisfied()` and `parseVersion()` helpers in InitCommand
- Updated TemplateEngineTest constructor calls (9 → 11 params)

Simplified scope (per user agreement):
- Deferred `prompts[].validation` to Story #24
- Deferred conditional fields to future story

### Story #20 - Directory Transforms (2026-01-03)

Implemented post-render file transforms for template processing:
- Created `Transform.java` record with type, pattern, replacement fields
- Created `TransformExecutor.java` for rename/delete operations using glob patterns
- Added `transforms` field to `TemplateManifest` and `TemplatesHub.TemplateEntry`
- Integrated transform execution in `InitCommand.java` after rendering
- Added 13 tests for TransformExecutor

Simplified scope (per user agreement):
- Only `rename` and `delete` transforms (dropped `move` as redundant with path variables)
- Glob pattern matching with `${varName}` substitution in replacement strings

Manifest schema now supports:
```yaml
transforms:
  - type: rename
    pattern: "**/*Template*.java"
    replacement: "${appClassName}.java"
  - type: delete
    pattern: "**/.gitkeep"
```

### Story #19 - Computed Variables (2026-01-04)

Implemented computed variable support for template manifests:
- Added `replace()` method to `StringTool.java` for string substitution
- Created `ComputedVariable.java` record for manifest definitions
- Added `computed` field to `TemplateManifest` and `TemplatesHub.TemplateEntry`
- Added `evaluateComputed()` method to `TemplateEngine.java`
- Updated `InitCommand.java` to evaluate computed vars after prompts
- Added 16 new tests (9 ComputedVariable + 7 StringTool.replace)

Manifest schema now supports:
```yaml
computed:
  - name: packagePath
    expression: "$str.replace($packageName, '.', '/')"
  - name: appClassName
    expression: "$str.pascal($appName)"
```

### Story #18 - Velocity Engine (2026-01-04)

Implemented Apache Velocity template engine replacing Handlebars:
- Created `StringTool.java` with transformation methods ($str.camel(), etc.)
- Created `TemplateRenderException.java` for fail-fast error handling
- Refactored `TemplateEngine.java` to use VelocityEngine
- Added 51 unit tests (15 new + 36 existing)
- Updated native-image reflect-config.json for Velocity classes

### Epic #13-17 Story Details (2026-01-04)

Fleshed out all remaining stories for Epics #13-17:
- **Epic #13** (qrun Build & Publish): #36-42 - Jib integration, OCI packaging, publishing, labels, digest verification, release channels, rollback
- **Epic #14** (qrun Service Operations): #43-49 - Status, logs (SSE streaming), stop, start, restart, deployment history, scale
- **Epic #15** (qstudio AI Planning): #50-53 - Code indexing, plan generation (multi-LLM), path policies, plan ledger
- **Epic #16** (Authentication & Security): #54-57 - OIDC device flow, service accounts, signature verification (Sigstore/PGP), OS keychain integration
- **Epic #17** (Enterprise Features): #58-61 - SSO (SAML/OIDC), RBAC, audit logging, org/team management

### Epic #12 Story Details (2026-01-04)

Fleshed out all 9 stories for Epic #12 (qqq - Template System) with:
- User stories and design specs
- Architecture diagrams (current vs target state)
- Files to create/modify tables
- Implementation task checklists
- Acceptance criteria

Stories updated: #18-26 (Velocity, computed vars, transforms, manifest v2, templates migration, Voyage mock, validation, errors, merge mode)

### Epic #11 Story Details (2026-01-04)

Fleshed out all 9 stories for Epic #11 (qbit - Package Management) with:
- User stories and design specs
- Command interfaces with examples
- Files to create/modify tables
- Implementation task checklists
- Acceptance criteria

Stories updated: #27-35 (search, add, remove, update, list, SemVer, conflicts, vendor, integrity)

### Roadmap & Issue Creation (2026-01-03)

Created comprehensive roadmap and GitHub project structure:

1. **Roadmap document** - `docs/ROADMAP.md` with user journey focus
2. **Milestones** - v0.2.0, v0.3.0, v1.0.0
3. **Epics** - 7 epic issues (#11-#17) with proper labels
4. **Stories** - 44 story issues (#18-#61) linked as sub-issues
5. **Issue types** - Epic for #11-17, Feature for #18-61
6. **Sub-issues** - All stories linked to parent epics

### Epic Summary

| Epic | Milestone | Stories |
|------|-----------|---------|
| #11 qbit - Package Management | v0.2.0 | #27-35 (9) |
| #12 qqq - Template System | v0.2.0 | #18-26 (9) |
| #13 qrun - Build & Publish | v0.3.0 | #36-42 (7) |
| #14 qrun - Service Operations | v0.3.0 | #43-49 (7) |
| #15 qstudio - AI Planning | v1.0.0 | #50-53 (4) |
| #16 Authentication & Security | v1.0.0 | #54-57 (4) |
| #17 Enterprise Features | v1.0.0 | #58-61 (4) |

### Key Design Decisions

- **Template engine**: Apache Velocity (not Handlebars) for QQQ consistency
- **Voyage integration**: Mock APIs for now, real integration when ready
- **User journey focus**: Roadmap organized by concrete use cases

## Current Branch State

- **Branch**: `develop`
- **Clean**: Yes (no uncommitted changes)

## Next Steps / TODO

### v0.2.0 - Create & Manage (Priority)

**Epic #12 - qqq Template System** (in progress):
1. ~~#18 - Replace Handlebars with Apache Velocity~~ DONE
2. ~~#19 - Add computed variables support~~ DONE
3. ~~#20 - Implement directory transforms~~ DONE
4. ~~#21 - Update template manifest schema v2~~ DONE
5. ~~#22 - Migrate templates-hub to Velocity~~ DONE
6. ~~#23 - Integrate template listing with Voyage mock~~ DONE
7. ~~#24 - Add prompt validation~~ DONE
8. #25 - Improve error messages (next)
9. #26 - Implement --merge mode

**Epic #11 - qbit Package Management** (after qqq):
1. #27 - qbit search command
2. #28 - qbit add with version constraints
3. #29 - qbit remove command
4. #30 - qbit update command
5. #31 - qbit list command
6. #32 - SemVer constraint resolution
7. #33 - Dependency conflict detection
8. #34 - Vendor directory layout
9. #35 - SHA-512 integrity verification

### Infrastructure

- GitHub project: https://github.com/orgs/QRun-IO/projects/12

## How to Continue

When starting a new session, say "continue from last session" and Claude will:
1. Read this file for context
2. Check git status for any pending work
3. Review GitHub project for current sprint

## Key Files

| File | Purpose |
|------|---------|
| `CLAUDE.md` | Project overview and build commands |
| `docs/ROADMAP.md` | User journey roadmap |
| `docs/internal/session-state.md` | This file - session continuity |
| `.github/workflows/build.yml` | CI/CD pipeline |
