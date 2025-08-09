#!/bin/bash

# Native Build Wrapper Script
# Validates environment before attempting native build

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 qctl Native Build${NC}"
echo "=================="
echo

# Step 1: Validate environment
echo -e "${BLUE}Step 1: Environment Validation${NC}"
if [ -f "scripts/validate-graalvm.sh" ]; then
    ./scripts/validate-graalvm.sh
else
    echo -e "${RED}❌ Cannot find validation script. Run from project root directory.${NC}"
    exit 1
fi

echo
echo -e "${BLUE}Step 2: Building Native Executable${NC}"
echo "Running: mvn -Pnative -DskipTests -pl qctl-shared,qctl-core -am clean package"
echo

# Step 2: Build native executable (include dependencies)
# -am = also-make (build required dependencies)
# -pl = projects list (target qctl-core and its dependencies)
mvn -Pnative -DskipTests -pl qctl-shared,qctl-core -am clean package

if [ $? -eq 0 ]; then
    echo
    echo -e "${GREEN}🎉 Native build completed successfully!${NC}"
    
    # Find the native executable
    NATIVE_BINARY=$(find . -name "qctl" -type f -executable 2>/dev/null | head -n 1)
    if [ -n "$NATIVE_BINARY" ]; then
        echo -e "${GREEN}📦 Native executable: ${NC}${NATIVE_BINARY}"
        
        # Show file size
        if command -v du &> /dev/null; then
            SIZE=$(du -h "$NATIVE_BINARY" | cut -f1)
            echo -e "${GREEN}📊 Size: ${NC}${SIZE}"
        fi
        
        # Test the executable
        echo -e "${BLUE}🧪 Testing executable...${NC}"
        if "$NATIVE_BINARY" --version; then
            echo -e "${GREEN}✅ Native executable works correctly!${NC}"
        else
            echo -e "${RED}⚠️  Native executable test failed${NC}"
        fi
    else
        echo -e "${RED}⚠️  Could not locate native executable${NC}"
    fi
else
    echo -e "${RED}❌ Native build failed${NC}"
    exit 1
fi
