# Native Build Guide

This document explains how to build `qctl` as a native executable using GraalVM.

## Quick Start

### Option 1: Automated Build Script (Recommended)
```bash
./scripts/build-native.sh
```

### Option 2: Manual Maven Command
```bash
# Check environment first
mvn -Pnative-check validate

# Build native executable (target qctl-core and dependencies)
mvn -Pnative -DskipTests -pl qctl-shared,qctl-core -am package
```

### Option 3: Environment Check Only
```bash
./scripts/validate-graalvm.sh
# or
./scripts/check-native-build-env.sh
```

## Prerequisites

### 1. GraalVM Installation

Download and install GraalVM 21+ from:
- **Official**: https://www.graalvm.org/downloads/
- **Oracle GraalVM**: https://www.oracle.com/downloads/graalvm-downloads.html
- **SDKMAN**: `sdk install java 21.0.1-graal`

### 2. Native Image Tool

If not included with your GraalVM distribution:
```bash
gu install native-image
```

### 3. Environment Setup

Set one of the following:
```bash
# Option A: Set JAVA_HOME to GraalVM
export JAVA_HOME=/path/to/graalvm

# Option B: Set GRAALVM_HOME
export GRAALVM_HOME=/path/to/graalvm
export PATH=$GRAALVM_HOME/bin:$PATH
```

### 4. Platform-Specific Requirements

#### Linux
```bash
# Ubuntu/Debian
sudo apt-get install build-essential zlib1g-dev

# For static builds (recommended)
sudo apt-get install musl-tools

# RHEL/CentOS
sudo yum groupinstall 'Development Tools'
sudo yum install zlib-devel musl-gcc
```

#### macOS
```bash
# Xcode command line tools (usually already installed)
xcode-select --install
```

#### Windows
- Install Visual Studio Build Tools or Visual Studio Community
- Ensure Windows SDK is available

## Build Configuration

The native build is configured with:

- **Static linking** (`--static --libc=musl`) for maximum portability on Linux
- **HTTP/HTTPS support** for API communication
- **Resource inclusion** for configuration files (`.properties`, `.xml`, `.json`, `.yaml`)
- **Optimized logging** initialization at build time
- **CLI-friendly** error reporting and stack traces

## Output

The native executable will be created at:
```
qctl-core/target/qctl
```

## Troubleshooting

### Common Issues

1. **"native-image is not installed"**
   - Install GraalVM native-image: `gu install native-image`
   - Verify installation: `native-image --version`

2. **"JAVA_HOME is not a GraalVM distribution"**
   - Set `JAVA_HOME` to point to GraalVM installation
   - Or set `GRAALVM_HOME` environment variable

3. **Build fails with memory errors**
   - Increase memory: `export MAVEN_OPTS="-Xmx4g"`
   - Or add to build: `mvn -Pnative -DskipTests -Dgraalvm.native.memory=4g package`

4. **Missing build tools on Linux**
   - Install development tools and zlib as shown above
   - For static builds, ensure musl-gcc is available

5. **Slow build times**
   - Native compilation can take 5-15 minutes depending on system
   - Use `-DskipTests` to speed up builds during development

### Environment Validation

Run the validation script to check your setup:
```bash
./scripts/validate-graalvm.sh
```

This will check:
- ✅ Java version (21+)
- ✅ GraalVM installation
- ✅ native-image tool availability
- ✅ Build tools (Linux)
- ✅ System memory
- ✅ Maven installation

## Advanced Configuration

### Custom Build Arguments

Modify the native profile in `pom.xml` to add custom GraalVM arguments:

```xml
<buildArgs>
  <buildArg>--your-custom-arg</buildArg>
</buildArgs>
```

### Profile Activation

The build uses Maven profiles:
- `native`: Builds the native executable
- `native-check`: Validates environment only

### Performance Tuning

For faster builds during development:
```bash
# Skip validation (if environment is known good)
mvn -Pnative -DskipTests -Dgraalvm.native.agent=false package
```

For smaller executables:
```bash
# Add optimization flags
mvn -Pnative -DskipTests -Dgraalvm.native.optimize=true package
```

## Integration with CI/CD

Example GitHub Actions workflow:
```yaml
- name: Setup GraalVM
  uses: graalvm/setup-graalvm@v1
  with:
    version: '21.0.1'
    java-version: '21'
    components: 'native-image'

- name: Build Native Executable
  run: ./scripts/build-native.sh
```

## Performance Notes

- **Startup time**: Native executables start ~10-50x faster than JVM
- **Memory usage**: Typically 2-5x lower memory footprint
- **Runtime performance**: May be slightly slower for long-running processes
- **Build time**: 5-15 minutes vs ~30 seconds for JVM builds
