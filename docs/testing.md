# Testing Guide

qctl uses JUnit 5 with comprehensive E2E tests that verify actual file content, variable substitution, and generated project validity.

## Running Tests

```bash
# All tests
mvn verify

# Integration tests only
mvn test -pl qctl-integration-tests

# Specific test class
mvn test -pl qctl-integration-tests -Dtest=InitCommandFullE2ETest
```

## Test Fixtures

Fixtures in `qctl-integration-tests/src/test/resources/fixtures/templates/`:

| Fixture | Purpose |
|---------|---------|
| `test-minimal` | Basic template with no variables |
| `test-computed-vars` | Computed variable evaluation (`packagePath` from `packageName`) |
| `test-full-substitution` | Variable replacement in content and paths |
| `test-transforms` | Delete and rename transforms |
| `test-with-prompts` | Interactive prompt handling |
| `test-variable-typo` | "Did you mean?" error suggestions |
| `test-transform-error` | Error message quality |

## Path Variable Convention

Use `__VARNAME__` syntax in directory names (e.g., `__packagePath__`). The template engine converts this to `$VARNAME` before Velocity processing, avoiding shell/Maven interpretation issues on CI.

```
template/src/main/java/__packagePath__/App.java
                         ↓ converted to
template/src/main/java/$packagePath/App.java
                         ↓ Velocity renders
src/main/java/com/example/app/App.java
```

## Test Harness

- **CommandTestHarness**: Executes commands in-process, captures stdout/stderr/exit code
- **TestTemplateRegistry**: Provides fixtures without network access
- **TestableConsoleUI**: Simulates user input for interactive prompts

## Adding a Fixture

1. Create directory under `fixtures/templates/<name>/`
2. Add `template.yaml` manifest (schema v2)
3. Add `template/` directory with files
4. Register in `TestTemplateRegistry.java`
