# qctl Roadmap

## Vision

qctl is the unified CLI for QRun platform developers. It enables the complete developer lifecycle: create projects, manage dependencies, build/deploy applications, and operate running services.

## Core Integration

**Voyage** - The QRun platform backend that qctl integrates with for:
- Template registry (qqq)
- Package registry (qbit)
- Artifact storage & deployment (qrun)
- Service orchestration & observability (qrun)

---

## User Journeys

### Journey 1: Create a New QQQ Application

**Goal:** Developer creates a new QQQ application from a template and has a running local dev environment.

**User Flow:**

```bash
# Step 1: See available templates
$ qctl qqq list
Available templates:
  qqq-web-api      Web API application with Javalin middleware
  qqq-cli-tool     Command-line tool with Picocli
  qqq-batch        Batch processing application
  qqq-full-stack   Full-stack with React frontend

# Step 2: Initialize a new project
$ qctl qqq init qqq-web-api my-app

? Java package [com.example.myapp]: com.acme.orders
? Maven groupId [com.acme]: com.acme
? Maven artifactId [my-app]: orders-service
? Application name [myApp]: ordersApp
? Application label [My App]: Orders Service

Creating project from template: qqq-web-api
  Cloning template...
  Rendering files with Velocity...
  Moving src/main/java/com/example/orders → src/main/java/com/acme/orders
  Moving src/test/java/com/example/orders → src/test/java/com/acme/orders
  Running post-gen hook: build
  > mvn clean verify -DskipTests
  [BUILD SUCCESS]

Project created: ./my-app
Next steps:
  cd my-app
  mvn spring-boot:run

# Step 3: Run locally
$ cd my-app && mvn spring-boot:run
[INFO] Started OrdersApp in 2.3s
```

**Required Capabilities:**

| Capability | Description | Status |
|------------|-------------|--------|
| Template listing | Fetch templates from Voyage (or local cache) | POC - needs Voyage integration |
| Template selection | Interactive or CLI arg | POC - needs polish |
| Prompt collection | Interactive prompts from manifest | POC - needs validation |
| CLI var overrides | `--var key=value` for non-interactive | POC - needs testing |
| Velocity rendering | Render file contents with Velocity | **TODO** (currently Handlebars) |
| Path rendering | Render file/dir names with variables | POC - switch to Velocity |
| Computed variables | Derive `packagePath` from `packageName` | **TODO** |
| Directory transforms | Move directories per manifest rules | **TODO** |
| Post-gen hooks | Run shell commands after render | POC - needs phases |
| Dry-run mode | Show what would be created | POC - needs refinement |
| Force overwrite | `--force` to overwrite existing | POC - needs merge mode |
| Error handling | Clear errors with recovery suggestions | **TODO** |
| Template caching | Cache templates with TTL | **TODO** |

**Design Decision: Template Engine**

**Decision:** Use **Apache Velocity** instead of Handlebars for consistency with QQQ's existing template/report system.

- QQQ already uses Velocity for template rendering
- Template authors familiar with QQQ will know Velocity syntax (`$variable`, `#if...#end`)
- More powerful than Handlebars (full control flow, macros)
- Requires migration from current Handlebars implementation

**Design Decision: Template Transformation**

**Decision:** Hybrid approach with Velocity rendering + structural transformations.

1. **Velocity for content:** Templates use `$packageName`, `$groupId` in source files
2. **Manifest rules for structure:** Define path transformations and directory moves
3. **Computed variables:** Derive `packagePath` from `packageName` automatically

**Template Manifest Example:**
```yaml
name: qqq-web-api
prompts:
  - name: packageName
    message: "Java package"
    default: "com.example.myapp"
  - name: groupId
    message: "Maven groupId"
    default: "com.example"
  - name: artifactId
    message: "Maven artifactId"
    default: "my-qqq-app"
  - name: appName
    message: "Application name"
    default: "myApp"

computed:
  packagePath: "${packageName.replace('.', '/')}"

transforms:
  - type: move
    from: "src/main/java/com/example/orders"
    to: "src/main/java/${packagePath}"
  - type: move
    from: "src/test/java/com/example/orders"
    to: "src/test/java/${packagePath}"

postGen:
  - name: build
    command: "mvn clean verify -DskipTests"
```

---

### Journey 2: Add QBit Dependencies

**Goal:** Developer finds and adds QQQ modules (qbits) to their project.

```bash
# Search for authentication modules
qctl qbit search auth

# Add the SSO qbit to project
qctl qbit add @qrun/auth-sso

# See what's installed
qctl qbit list

# Update to latest versions
qctl qbit update
```

**Required Capabilities:**
- [ ] Search Voyage registry by keyword/tag
- [ ] View qbit details (description, version, dependencies)
- [ ] Add qbit with version constraint (`@qrun/auth-sso@^2.0`)
- [ ] Resolve dependency tree (handle conflicts)
- [ ] Generate/update lockfile (`qbits.lock`)
- [ ] Vendor packages to `vendor/qbits/`
- [ ] Integrity verification (sha512)

---

### Journey 3: Build & Deploy Application

**Goal:** Developer packages their application and deploys it to an environment.

```bash
# Build OCI image
qctl qrun package

# Deploy to dev environment
qctl qrun publish --env dev

# Deploy to production
qctl qrun publish --env prod --channel stable
```

**Required Capabilities:**
- [ ] Build OCI image via Jib
- [ ] Push to Voyage artifact registry
- [ ] Create release record with metadata
- [ ] Deploy to target environment via Voyage
- [ ] Support release channels (dev, beta, stable)
- [ ] Rollback to previous version

---

### Journey 4: Operate Running Services

**Goal:** Developer monitors and manages their deployed applications.

```bash
# Check service status
qctl qrun status --app my-app --env prod

# View logs
qctl qrun logs --app my-app --env prod --follow

# Restart service
qctl qrun restart --app my-app --env prod

# Stop service (maintenance)
qctl qrun stop --app my-app --env prod

# Start service
qctl qrun start --app my-app --env prod
```

**Required Capabilities:**
- [ ] Query Voyage for service status (replicas, health, version)
- [ ] Stream logs from Voyage
- [ ] Stop/start/restart services via Voyage API
- [ ] View deployment history
- [ ] Scale replicas up/down

---

## Milestones

### v0.2.0 - Create & Manage

**Focus:** Complete "Create New Application" and "Add Dependencies" journeys.

**Epic: qqq - Template System**
| Feature | Description |
|---------|-------------|
| Velocity engine | Replace Handlebars with Apache Velocity |
| Computed variables | Auto-derive `packagePath` from `packageName` |
| Directory transforms | Move folders per manifest `transforms` rules |
| Template manifest v2 | Add `computed` and `transforms` sections |
| Migrate templates | Update templates-hub to Velocity syntax |
| Voyage mock integration | Template listing from mock API |
| Prompt validation | Validate required fields, types, constraints |
| Error messages | Clear errors with recovery suggestions |
| Merge mode | `--merge` for selective regeneration |

**Epic: qbit - Package Management**
| Feature | Description |
|---------|-------------|
| qbit search | Search against Voyage mock registry |
| qbit add | Add package with version constraints |
| qbit remove | Remove package, update lockfile |
| qbit update | Update packages to latest |
| qbit list | List installed packages |
| SemVer resolution | Constraint solving (^, ~, exact) |
| Conflict detection | Identify and report version conflicts |
| Vendor layout | Download to `vendor/qbits/` |
| Integrity verification | SHA-512 hash checks |

---

### v0.3.0 - Deploy & Operate

**Focus:** Complete "Deploy Application" and "Operate Services" journeys.

**Epic: qrun - Build & Publish**
| Feature | Description |
|---------|-------------|
| Jib integration | OCI image building |
| qrun package | Build container image |
| qrun publish | Push to Voyage registry |
| OCI labels | Inject standard and custom labels |
| Digest verification | Verify image after push |
| Release channels | stable/beta/alpha support |
| qrun rollback | Rollback to previous version |

**Epic: qrun - Service Operations**
| Feature | Description |
|---------|-------------|
| qrun status | Service status from Voyage |
| qrun logs | Stream logs with --follow |
| qrun stop | Stop service |
| qrun start | Start service |
| qrun restart | Restart service |
| Deployment history | View past deployments |
| qrun scale | Scale replicas up/down |

---

### v1.0.0 - Production Ready

**Focus:** AI planning, security hardening, enterprise features.

**Epic: qstudio - AI Planning**
| Feature | Description |
|---------|-------------|
| Code indexing | Local codebase analysis |
| qstudio plan | Generate implementation plan |
| Path policies | Allow/deny path lists |
| Ledger storage | Track plan history |

**Epic: Authentication & Security**
| Feature | Description |
|---------|-------------|
| OIDC device flow | Full authentication flow |
| Service accounts | CI/CD support |
| Signature enforcement | Require signed packages/images |
| OS keychain | Secure credential storage |

**Epic: Enterprise Features**
| Feature | Description |
|---------|-------------|
| SSO integration | SAML/OIDC enterprise SSO |
| RBAC | Team-based access control |
| Audit logging | Compliance audit trail |
| Org management | Organization/team management |

---

## Voyage Integration

**Voyage** is the QRun platform backend (currently in development). Until Voyage is ready, qctl will:
1. Use mock APIs for development and testing
2. Support local/offline modes where possible
3. Design against Voyage's planned API contracts (standard QQQ data model)

Voyage will provide a versioned API that drives templates, qbits, artifacts, and service orchestration.

## Voyage API Dependencies (Planned)

qctl requires these Voyage API endpoints:

### Templates (qqq)
- `GET /v1/templates` - List available templates
- `GET /v1/templates/{id}` - Get template details
- `GET /v1/templates/{id}/versions/{version}` - Download template

### Packages (qbit)
- `GET /v1/qbits?q={query}` - Search packages
- `GET /v1/qbits/{id}` - Get package details
- `GET /v1/qbits/{id}/versions/{version}` - Download package

### Artifacts (qrun)
- `POST /v1/artifacts` - Register artifact
- `GET /v1/artifacts/{id}` - Get artifact info
- `POST /v1/releases` - Create release
- `POST /v1/deployments` - Deploy to environment

### Services (qrun)
- `GET /v1/apps/{app}/status` - Service status
- `GET /v1/apps/{app}/logs` - Service logs
- `POST /v1/apps/{app}/restart` - Restart service
- `POST /v1/apps/{app}/stop` - Stop service
- `POST /v1/apps/{app}/start` - Start service
- `POST /v1/apps/{app}/scale` - Scale replicas

---

## GitHub Issues Structure

Each milestone will have:
- 1 Epic issue tracking the user journey
- Story issues for each capability (linked to epic)
- Labels: `epic`, `story`, `module:qqq|qbit|qrun|qstudio`

Example:
```
Epic: [v0.2.0] Create New Application Journey
├── Story: Template registry integration with Voyage
├── Story: Add web-api template
├── Story: Add cli-tool template
├── Story: Add batch-processor template
├── Story: Implement qqq upgrade command
└── Story: Enhanced post-init hooks
```
