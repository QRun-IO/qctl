#!/bin/bash

# GraalVM Environment Validation Script
# Checks if the environment is properly configured for native image builds

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 Validating GraalVM Native Build Environment...${NC}"
echo

# Check if we're running on a supported OS
OS=$(uname -s)
ARCH=$(uname -m)
echo -e "📋 System: ${BLUE}${OS} ${ARCH}${NC}"

# Function to print error and exit
error_exit() {
    echo -e "${RED}❌ $1${NC}"
    echo -e "${YELLOW}💡 $2${NC}"
    exit 1
}

# Function to print warning
warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Function to print success
success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Check Java version and vendor
echo -e "${BLUE}☕ Checking Java environment...${NC}"
if ! command -v java &> /dev/null; then
    error_exit "Java is not installed or not in PATH" \
               "Install Java 21+ from https://adoptium.net/ or use GraalVM distribution"
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
JAVA_VENDOR=$(java -version 2>&1 | grep -i "build\|vm" | head -n 1)

echo "   Version: ${JAVA_VERSION}"
echo "   Details: ${JAVA_VENDOR}"

# Check if Java version is 21+
MAJOR_VERSION=$(echo ${JAVA_VERSION} | cut -d'.' -f1)
if [ "${MAJOR_VERSION}" -lt 21 ]; then
    error_exit "Java ${JAVA_VERSION} is not supported" \
               "Native builds require Java 21+. Install GraalVM 21+ or OpenJDK 21+"
fi

# Check for GraalVM
echo -e "${BLUE}🚀 Checking GraalVM installation...${NC}"
IS_GRAALVM=false

# Method 1: Check GRAALVM_HOME
if [ -n "${GRAALVM_HOME}" ]; then
    if [ -d "${GRAALVM_HOME}" ]; then
        success "GRAALVM_HOME is set: ${GRAALVM_HOME}"
        IS_GRAALVM=true
    else
        warning "GRAALVM_HOME is set but directory doesn't exist: ${GRAALVM_HOME}"
    fi
fi

# Method 2: Check JAVA_HOME for GraalVM
if [ -n "${JAVA_HOME}" ]; then
    echo "   JAVA_HOME: ${JAVA_HOME}"
    
    # Check if it's a GraalVM distribution
    if [ -f "${JAVA_HOME}/bin/native-image" ]; then
        success "native-image found in JAVA_HOME/bin"
        IS_GRAALVM=true
    elif [ -f "${JAVA_HOME}/lib/svm/bin/native-image" ]; then
        success "native-image found in JAVA_HOME/lib/svm/bin"
        IS_GRAALVM=true
    else
        warning "JAVA_HOME does not appear to be a GraalVM distribution"
    fi
else
    warning "JAVA_HOME is not set"
fi

# Method 3: Check if native-image is in PATH
if command -v native-image &> /dev/null; then
    NATIVE_IMAGE_VERSION=$(native-image --version 2>&1 | head -n 1)
    success "native-image found in PATH: ${NATIVE_IMAGE_VERSION}"
    IS_GRAALVM=true
fi

# Final validation
if [ "${IS_GRAALVM}" = false ]; then
    echo
    error_exit "GraalVM native-image tool not found" \
               "Install GraalVM from https://www.graalvm.org/downloads/ or install native-image: 'gu install native-image'"
fi

# Check for required build tools on Linux
if [ "${OS}" = "Linux" ]; then
    echo -e "${BLUE}🔧 Checking Linux build tools...${NC}"
    
    # Check for gcc
    if ! command -v gcc &> /dev/null; then
        warning "gcc not found - may be needed for static linking"
        echo "   Install with: sudo apt-get install build-essential (Ubuntu/Debian)"
        echo "   Or: sudo yum groupinstall 'Development Tools' (RHEL/CentOS)"
    else
        success "gcc found: $(gcc --version | head -n 1)"
    fi
    
    # Check for musl-gcc if we're doing static builds
    if ! command -v musl-gcc &> /dev/null; then
        warning "musl-gcc not found - required for static builds"
        echo "   Install with: sudo apt-get install musl-tools (Ubuntu/Debian)"
        echo "   Or: sudo yum install musl-gcc (RHEL/CentOS)"
    else
        success "musl-gcc found: $(musl-gcc --version | head -n 1)"
    fi
    
    # Check for zlib
    if ! ldconfig -p | grep -q libz.so; then
        warning "zlib development libraries may be missing"
        echo "   Install with: sudo apt-get install zlib1g-dev (Ubuntu/Debian)"
        echo "   Or: sudo yum install zlib-devel (RHEL/CentOS)"
    else
        success "zlib libraries found"
    fi
fi

# Check Maven
echo -e "${BLUE}📦 Checking Maven...${NC}"
if ! command -v mvn &> /dev/null; then
    error_exit "Maven is not installed or not in PATH" \
               "Install Maven from https://maven.apache.org/download.cgi"
fi

MVN_VERSION=$(mvn -version 2>/dev/null | head -n 1 | cut -d' ' -f3)
success "Maven found: ${MVN_VERSION}"

# Memory check
echo -e "${BLUE}💾 Checking system resources...${NC}"
if [ "${OS}" = "Darwin" ]; then
    TOTAL_MEM_GB=$(($(sysctl -n hw.memsize) / 1024 / 1024 / 1024))
elif [ "${OS}" = "Linux" ]; then
    TOTAL_MEM_GB=$(($(grep MemTotal /proc/meminfo | awk '{print $2}') / 1024 / 1024))
else
    TOTAL_MEM_GB="unknown"
fi

if [ "${TOTAL_MEM_GB}" != "unknown" ]; then
    echo "   Total RAM: ${TOTAL_MEM_GB} GB"
    if [ "${TOTAL_MEM_GB}" -lt 4 ]; then
        warning "Less than 4GB RAM detected - native builds may be slow or fail"
        echo "   Consider increasing memory or using -J-Xmx2g flag"
    else
        success "Sufficient RAM for native builds"
    fi
fi

echo
echo -e "${GREEN}🎉 Environment validation completed successfully!${NC}"
echo -e "${BLUE}You can now run: ${NC}${GREEN}mvn -Pnative -DskipTests -pl qctl-shared,qctl-core -am package${NC}"
echo
